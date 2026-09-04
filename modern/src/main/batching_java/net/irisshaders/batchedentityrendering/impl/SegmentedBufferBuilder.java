package net.irisshaders.batchedentityrendering.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.batchedentityrendering.mixin.RenderTypeAccessor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Accumulates immediate-mode geometry into a single arena, cutting a new {@link BufferSegment} whenever the render type
 * changes.
 * <p>
 * Two properties fall out of this that the rest of the system relies on:
 * <ul>
 *     <li>The segment list is in <b>submission order</b>. That is what the translucent drawing phase needs, since
 *     translucent geometry cannot be reordered without changing what ends up on screen.</li>
 *     <li>Consecutive submissions of the <i>same</i> render type are merged into one segment. A builder that only ever
 *     sees one render type therefore still produces exactly one draw call.</li>
 * </ul>
 * Note that a segment owns memory in the arena until it is drawn (the buffer uploader takes ownership) or explicitly
 * handed to {@link #discard(BufferSegment)}.
 */
public class SegmentedBufferBuilder implements MultiBufferSource, MemoryTrackingBuffer {
	private static final int INITIAL_ARENA_SIZE = 512 * 1024;

    //? if <1.21 {
	private BufferBuilder arena;
    //?} else {
    /*private com.mojang.blaze3d.vertex.ByteBufferBuilder arena;
	private BufferBuilder writer;
    *///?}

	private final List<BufferSegment> segments = new ArrayList<>();
	private RenderType currentType;
	private long lastUse;

	public SegmentedBufferBuilder() {
		this.arena = newArena();
		this.lastUse = System.currentTimeMillis();
	}

    //? if <1.21 {
	private static BufferBuilder newArena() {
		return new BufferBuilder(INITIAL_ARENA_SIZE);
	}
    //?} else
    //private static com.mojang.blaze3d.vertex.ByteBufferBuilder newArena() { return new com.mojang.blaze3d.vertex.ByteBufferBuilder(INITIAL_ARENA_SIZE); }

	private static boolean shouldSortOnUpload(RenderType type) {
		return ((RenderTypeAccessor) type).shouldSortOnUpload();
	}

	/**
	 * Releases a segment that will never be drawn. Drawn segments are released by the buffer uploader instead.
	 */
	public static void discard(BufferSegment segment) {
        //? if <1.21 {
		segment.renderedBuffer().release();
        //?} else
        //segment.renderedBuffer().close();
	}

	private VertexConsumer sink() {
        //? if <1.21 {
		return arena;
        //?} else
        //return writer;
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		// Triangle fans and line strips can't be concatenated with anything, so they always start a fresh segment.
		if (!Objects.equals(currentType, renderType) || RenderTypeUtil.requiresSegmentSplits(renderType)) {
			finishSegment();
			beginSegment(renderType);
		}

		// Use duplicate vertices to break up triangle strips
		// https://developer.apple.com/library/archive/documentation/3DDrawing/Conceptual/OpenGLES_ProgrammingGuide/Art/degenerate_triangle_strip_2x.png
		// This works by generating zero-area triangles that don't end up getting rendered.
		if (RenderTypeUtil.isTriangleStripDrawMode(currentType)) {
			((BufferBuilderExt) sink()).splitStrip();
		}

		return sink();
	}

	private void beginSegment(RenderType renderType) {
        //? if <1.21 {
		arena.begin(renderType.mode(), renderType.format());
        //?} else
        //writer = new BufferBuilder(arena, renderType.mode(), renderType.format());

		currentType = renderType;
		lastUse = System.currentTimeMillis();
	}

	private void finishSegment() {
		if (currentType == null) {
			return;
		}

        //? if <1.21 {
		if (shouldSortOnUpload(currentType)) {
			arena.setQuadSorting(RenderSystem.getVertexSorting());
		}

		BufferBuilder.RenderedBuffer built = arena.endOrDiscardIfEmpty();
        //?} else {
        /*com.mojang.blaze3d.vertex.MeshData built = writer.build();
		writer = null;

		if (built != null && shouldSortOnUpload(currentType)) {
			built.sortQuads(arena, RenderSystem.getVertexSorting());
		}
        *///?}

		if (built != null) {
			segments.add(new BufferSegment(built, currentType));
		}

		currentType = null;
	}

	/**
	 * Finishes any in-progress segment and hands over everything accumulated so far, in submission order. Ownership of
	 * the returned segments passes to the caller.
	 */
	public List<BufferSegment> getSegments() {
		finishSegment();

		if (segments.isEmpty()) {
			return Collections.emptyList();
		}

		List<BufferSegment> finalSegments = new ArrayList<>(segments);

		segments.clear();

		return finalSegments;
	}

	/**
	 * Finishes any in-progress segment and releases everything this builder is still holding. Leaves the builder idle,
	 * which callers rely on before they start releasing segments they took from it - releasing the last outstanding
	 * segment resets the arena, and that would corrupt a build that was still in progress.
	 */
	public void discardPending() {
		finishSegment();

		for (BufferSegment segment : segments) {
			discard(segment);
		}

		segments.clear();
	}

	private void resetArena() {
		// The arena itself is going away, so the segments inside it don't need to be released one by one.
		segments.clear();
		currentType = null;
        //? if >=1.21
        //writer = null;

		((MemoryTrackingBuffer) arena).freeAndDeleteBuffer();
		arena = newArena();
		lastUse = System.currentTimeMillis();
	}

	/**
	 * Returns the arena to its initial size if this builder has been idle for a while. Without this, one unusually
	 * heavy frame would keep its peak allocation for the rest of the session.
	 */
	public void clearBuffers(int clearTime) {
		if (segments.isEmpty() && currentType == null
			&& getAllocatedSize() > INITIAL_ARENA_SIZE
			&& System.currentTimeMillis() - lastUse > clearTime) {
			resetArena();
		}
	}

	/**
	 * Throws away this frame's geometry to free memory. Only called when we're about to run out anyway; the frame will
	 * be missing some entities, which is better than crashing.
	 */
	public void lastDitchAttempt() {
		resetArena();
	}

	@Override
	public long getAllocatedSize() {
		return ((MemoryTrackingBuffer) arena).getAllocatedSize();
	}

	@Override
	public long getUsedSize() {
		return ((MemoryTrackingBuffer) arena).getUsedSize();
	}

	@Override
	public void freeAndDeleteBuffer() {
		segments.clear();
		currentType = null;
        //? if >=1.21
        //writer = null;

		((MemoryTrackingBuffer) arena).freeAndDeleteBuffer();
	}
}
