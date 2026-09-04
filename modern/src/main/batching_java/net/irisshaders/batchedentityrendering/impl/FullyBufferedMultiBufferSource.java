package net.irisshaders.batchedentityrendering.impl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import net.irisshaders.batchedentityrendering.impl.wrappers.WrappingMultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Buffers all immediate-mode geometry for a frame and draws it in a fixed sequence of phases, one per
 * {@link TransparencyType}, in enum order.
 * <p>
 * Every phase except {@link #SEQUENCED_PHASE} is <b>batched</b>: its geometry is grouped by render type and each type is
 * drawn once. That is sound because opaque geometry is order-independent (the depth test resolves it), and because the
 * remaining phases are decals and overlays whose ordering relative to each other doesn't matter.
 * <p>
 * {@link #SEQUENCED_PHASE} is different. Translucent geometry blends with whatever is already in the framebuffer, so
 * reordering it changes the image. Its geometry therefore goes to a dedicated builder that preserves submission order.
 * Because opaque geometry is routed elsewhere, a run of similar objects (say, a herd of slimes) still collapses into a
 * single draw call - the sequenced builder only splits when the translucent render type actually changes, which is
 * exactly when the order is observable.
 * <p>
 * The phase split assumes that translucent geometry belongs on top of opaque geometry, which holds <i>between</i>
 * objects but not always <i>within</i> one. A horse submits its markings ({@code entityTranslucent}) before its armor
 * ({@code entityCutoutNoCull}), and on the parts where the two models are coplanar the later draw wins, so hoisting the
 * armor into the opaque phase paints the markings over it. {@link #startGroup()} marks off one object's submissions;
 * within a group, anything submitted after the first translucent geometry is demoted into the sequenced phase so that it
 * keeps its place. Only the object that interleaves pays for it - other users of the same render type still batch.
 */
public class FullyBufferedMultiBufferSource extends MultiBufferSource.BufferSource implements MemoryTrackingBuffer, Groupable, WrappingMultiBufferSource {
	private static final int NUM_BUFFERS = 32;
	private static final TransparencyType[] TRANSPARENCY_TYPES = TransparencyType.values();
	private static final TransparencyType SEQUENCED_PHASE = TransparencyType.GENERAL_TRANSPARENT;

	/**
	 * Builders for the batched phases. A render type sticks to one builder so that repeated use of the same type
	 * concatenates instead of splitting.
	 */
	private final SegmentedBufferBuilder[] builders;
	/**
	 * The builder for {@link #SEQUENCED_PHASE}. Everything goes through this one builder so that its segment list is a
	 * faithful record of submission order.
	 */
	private final SegmentedBufferBuilder sequencedBuilder;
	/**
	 * An LRU cache mapping RenderType objects to a relevant buffer.
	 */
	private final Object2IntLinkedOpenHashMap<RenderType> affinities;
	private final BufferSegmentRenderer segmentRenderer;
	private final UnflushableWrapper unflushableWrapper;
	private final List<Function<RenderType, RenderType>> wrappingFunctionStack;

	/**
	 * Collected geometry for the batched phases, grouped by render type.
	 */
	private final EnumMap<TransparencyType, Map<RenderType, List<BufferSegment>>> batchedPhases = new EnumMap<>(TransparencyType.class);
	/**
	 * Collected geometry for {@link #SEQUENCED_PHASE}, in submission order.
	 */
	private final List<BufferSegment> sequencedSegments = new ArrayList<>();

	private int drawCalls;
	private int renderTypes;
	private Function<RenderType, RenderType> wrappingFunction = null;
	private boolean isReady;
	private boolean inGroup;
	/**
	 * Set once the current group submits translucent geometry. Everything it submits afterwards is demoted into the
	 * sequenced phase so that it stays behind that geometry.
	 */
	private boolean forceSequenced;

	public FullyBufferedMultiBufferSource() {
        //? if <1.21 {
		super(new com.mojang.blaze3d.vertex.BufferBuilder(0), Collections.emptyMap());
        //?} else
        //super(new com.mojang.blaze3d.vertex.ByteBufferBuilder(0), it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps.emptyMap());

		this.builders = new SegmentedBufferBuilder[NUM_BUFFERS];

		for (int i = 0; i < this.builders.length; i++) {
			this.builders[i] = new SegmentedBufferBuilder();
		}

		this.sequencedBuilder = new SegmentedBufferBuilder();

		// use accessOrder=true so our LinkedHashMap works as an LRU cache.
		this.affinities = new Object2IntLinkedOpenHashMap<>(32, 0.75F);
        this.affinities.defaultReturnValue(-1);

		this.drawCalls = 0;
		this.segmentRenderer = new BufferSegmentRenderer();
		this.unflushableWrapper = new UnflushableWrapper(this);
		this.wrappingFunctionStack = new ArrayList<>();
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		// Anything collected so far still needs to be drawn, but it can no longer be considered complete.
		isReady = false;

		if (wrappingFunction != null) {
			renderType = wrappingFunction.apply(renderType);
		}

		SegmentedBufferBuilder builder = builderFor(renderType);

		VertexConsumer buffer;

		try {
			buffer = builder.getBuffer(renderType);
		} catch (OutOfMemoryError e) {
			weAreOutOfMemory();

			// Try exactly once more. If this throws too, we genuinely can't continue.
			buffer = builder.getBuffer(renderType);
		}

        //? if <1.21 {
        if (buffer instanceof org.embeddedt.embeddium.impl.render.vertex.buffer.ExtendedBufferBuilder bufferBuilderExt) {
            var replacement = bufferBuilderExt.sodium$getDelegate();
            if (replacement != null) {
                return replacement;
            }
        }
        //?}

        return buffer;
	}

	private SegmentedBufferBuilder builderFor(RenderType renderType) {
		if (forceSequenced || RenderTypeUtil.getTransparencyType(renderType) == SEQUENCED_PHASE) {
			// Only keep the flag latched while we're still in a group.
			forceSequenced = inGroup;

			return sequencedBuilder;
		}

		int affinity = affinities.getAndMoveToLast(renderType);

		if (affinity == -1) {
			if (affinities.size() < builders.length) {
				affinity = affinities.size();
			} else {
				// We remove the element from the map that is used least-frequently.
				// With how we've configured our map, that is the first element.
				affinity = affinities.removeFirstInt();
			}

			affinities.put(renderType, affinity);
		}

		return builders[affinity];
	}

	/**
	 * Collects everything submitted so far. Safe to call more than once per frame - later calls append, so geometry
	 * submitted after an earlier call is still drawn, in the right place.
	 */
	public void readyUp() {
		isReady = true;

		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();

		profiler.push("collect");

		for (SegmentedBufferBuilder builder : builders) {
			for (BufferSegment segment : builder.getSegments()) {
				batchedPhases
					.computeIfAbsent(RenderTypeUtil.getTransparencyType(segment.type()), type -> new LinkedHashMap<>())
					.computeIfAbsent(segment.type(), type -> new ArrayList<>())
					.add(segment);
			}
		}

		sequencedSegments.addAll(sequencedBuilder.getSegments());

		affinities.clear();

		profiler.pop();
	}

	@Override
	public void endBatch() {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();

		if (!isReady) readyUp();

		profiler.push("draw buffers");

		for (TransparencyType transparencyType : TRANSPARENCY_TYPES) {
			drawPhase(transparencyType);
		}

		profiler.popPush("reset");

		isReady = false;

        int targetClearTime = getTargetClearTime();

        for (SegmentedBufferBuilder builder : builders) {
            builder.clearBuffers(targetClearTime);
        }

        sequencedBuilder.clearBuffers(targetClearTime);

		profiler.pop();
	}

	public void endBatchWithType(TransparencyType transparencyType) {
		ProfilerFiller profiler = Minecraft.getInstance().getProfiler();

		if (!isReady) readyUp();

		profiler.push("draw buffers");

		drawPhase(transparencyType);

		profiler.popPush("reset type " + transparencyType);

		profiler.pop();
	}

	/**
	 * Draws and consumes everything collected for one phase. Doing nothing on a second call for the same phase is
	 * deliberate - {@code endBatch} runs after {@code endBatchWithType} in the separate-entity-draws path.
	 */
	private void drawPhase(TransparencyType phase) {
		Map<RenderType, List<BufferSegment>> batched = batchedPhases.remove(phase);

		if (batched != null) {
			for (Map.Entry<RenderType, List<BufferSegment>> entry : batched.entrySet()) {
				RenderType type = entry.getKey();

				type.setupRenderState();

				renderTypes += 1;

				for (BufferSegment segment : entry.getValue()) {
					segmentRenderer.drawInner(segment);
					drawCalls += 1;
				}

				type.clearRenderState();
			}
		}

		if (phase == SEQUENCED_PHASE && !sequencedSegments.isEmpty()) {
			drawSequenced();
			sequencedSegments.clear();
		}
	}

	/**
	 * Draws the sequenced phase in submission order, only touching render state when the type actually changes.
	 */
	private void drawSequenced() {
		RenderType active = null;

		for (BufferSegment segment : sequencedSegments) {
			if (!segment.type().equals(active)) {
				if (active != null) {
					active.clearRenderState();
				}

				active = segment.type();
				active.setupRenderState();

				renderTypes += 1;
			}

			segmentRenderer.drawInner(segment);
			drawCalls += 1;
		}

		if (active != null) {
			active.clearRenderState();
		}
	}

	/**
	 * Releases everything collected but not drawn. Anything left over at this point is geometry we've decided not to
	 * render; it still owns arena memory, so it has to be handed back explicitly.
	 */
	public void discardCollected() {
		// Leave every builder idle first. Releasing the last outstanding segment of an arena resets it, which would
		// corrupt a segment that was still being built.
		for (SegmentedBufferBuilder builder : builders) {
			builder.discardPending();
		}

		sequencedBuilder.discardPending();

		for (Map<RenderType, List<BufferSegment>> byType : batchedPhases.values()) {
			for (List<BufferSegment> phaseSegments : byType.values()) {
				for (BufferSegment segment : phaseSegments) {
					SegmentedBufferBuilder.discard(segment);
				}
			}
		}

		batchedPhases.clear();

		for (BufferSegment segment : sequencedSegments) {
			SegmentedBufferBuilder.discard(segment);
		}

		sequencedSegments.clear();

		isReady = false;
	}

    private static long toMib(long x) {
        return x / 1024L / 1024L;
    }

    private int getTargetClearTime() {
        long sizeInMiB = toMib(getAllocatedSize());
        if (sizeInMiB > 5000) { // Over 5GB of RAM used.
            return 1000; // Be extremely aggressive; 1 second per buffer.
        } else if (sizeInMiB > 1000) { // Over 1GB of RAM used.
            return 5000; // Wait 5 seconds.
        } else {
            return 10000; // we chillin; 10 seconds.
        }
    }

	public int getDrawCalls() {
		return drawCalls;
	}

	public int getRenderTypes() {
		return renderTypes;
	}

	public void resetDrawCalls() {
		drawCalls = 0;
		renderTypes = 0;
	}

	@Override
	public void endBatch(RenderType type) {
		// Disable explicit flushing
	}

	public MultiBufferSource.BufferSource getUnflushableWrapper() {
		return unflushableWrapper;
	}

	@Override
	public long getAllocatedSize() {
        long size = sequencedBuilder.getAllocatedSize();

		for (SegmentedBufferBuilder builder : builders) {
			size += builder.getAllocatedSize();
		}

		return size;
	}

	@Override
	public long getUsedSize() {
        long size = sequencedBuilder.getUsedSize();

		for (SegmentedBufferBuilder builder : builders) {
			size += builder.getUsedSize();
		}

		return size;
	}

    public void weAreOutOfMemory() {
        discardCollected();

        for (SegmentedBufferBuilder builder : builders) {
            builder.lastDitchAttempt();
        }

        sequencedBuilder.lastDitchAttempt();
    }

	@Override
	public void freeAndDeleteBuffer() {
		discardCollected();

		for (SegmentedBufferBuilder builder : builders) {
			builder.freeAndDeleteBuffer();
		}

		sequencedBuilder.freeAndDeleteBuffer();
	}

	@Override
	public void startGroup() {
		if (inGroup) {
			throw new IllegalStateException("Already in a group");
		}

		inGroup = true;
		forceSequenced = false;
	}

	@Override
	public boolean maybeStartGroup() {
		if (inGroup) {
			return false;
		}

		inGroup = true;
		forceSequenced = false;

		return true;
	}

	@Override
	public void endGroup() {
		if (!inGroup) {
			throw new IllegalStateException("Not in a group");
		}

		inGroup = false;
		forceSequenced = false;
	}

	@Override
	public void pushWrappingFunction(Function<RenderType, RenderType> wrappingFunction) {
		if (this.wrappingFunction != null) {
			this.wrappingFunctionStack.add(this.wrappingFunction);
		}

		this.wrappingFunction = wrappingFunction;
	}

	@Override
	public void popWrappingFunction() {
		if (this.wrappingFunctionStack.isEmpty()) {
			this.wrappingFunction = null;
		} else {
			this.wrappingFunction = this.wrappingFunctionStack.remove(this.wrappingFunctionStack.size() - 1);
		}
	}

	@Override
	public void assertWrapStackEmpty() {
		if (!this.wrappingFunctionStack.isEmpty() || this.wrappingFunction != null) {
			throw new IllegalStateException("Wrapping function stack not empty!");
		}
	}

	/**
	 * A wrapper that prevents callers from explicitly flushing anything.
	 */
	private static class UnflushableWrapper extends MultiBufferSource.BufferSource implements Groupable {
		private final FullyBufferedMultiBufferSource wrapped;

		UnflushableWrapper(FullyBufferedMultiBufferSource wrapped) {
            //? if <1.21 {
            super(new com.mojang.blaze3d.vertex.BufferBuilder(0), Collections.emptyMap());
             //?} else
            //super(new com.mojang.blaze3d.vertex.ByteBufferBuilder(0), it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps.emptyMap());

			this.wrapped = wrapped;
		}

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			return wrapped.getBuffer(renderType);
		}

		@Override
		public void endBatch() {
			// Disable explicit flushing
		}

		@Override
		public void endBatch(RenderType type) {
			// Disable explicit flushing
		}

		@Override
		public void startGroup() {
			wrapped.startGroup();
		}

		@Override
		public boolean maybeStartGroup() {
			return wrapped.maybeStartGroup();
		}

		@Override
		public void endGroup() {
			wrapped.endGroup();
		}
	}
}
