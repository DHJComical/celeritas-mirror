package net.irisshaders.batchedentityrendering.impl;

//? if >=1.21 {

/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.irisshaders.batchedentityrendering.mixin.RenderTypeAccessor;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SegmentedBufferBuilder implements MemoryTrackingBuffer {
    private final Map<RenderType, ByteBufferBuilderHolder> buffers;
    private final Map<RenderType, BufferBuilder> builders;
    private final List<BufferSegment> segments;
    private final FullyBufferedMultiBufferSource parent;

    public SegmentedBufferBuilder(FullyBufferedMultiBufferSource parent) {
        // 2 MB initial allocation
        this.parent = parent;
        this.buffers = new Object2ObjectOpenHashMap<>();
        this.builders = new Object2ObjectOpenHashMap<>();
        this.segments = new ArrayList<>();
    }

    private static boolean shouldSortOnUpload(RenderType type) {
        return ((RenderTypeAccessor) type).shouldSortOnUpload();
    }

    public VertexConsumer getBuffer(RenderType renderType) {
        try {
            if (RenderTypeUtil.requiresSegmentSplits(renderType)) {
                endAndGenSegmentForType(renderType);
            }

            ByteBufferBuilderHolder buffer = buffers.computeIfAbsent(renderType, (r) -> new ByteBufferBuilderHolder(new ByteBufferBuilder(r.bufferSize())));

            buffer.wasUsed();
            BufferBuilder builder = builders.computeIfAbsent(renderType, (t) -> new BufferBuilder(buffer.getBuffer(), renderType.mode(), renderType.format()));

            // Use duplicate vertices to break up triangle strips
            // https://developer.apple.com/library/archive/documentation/3DDrawing/Conceptual/OpenGLES_ProgrammingGuide/Art/degenerate_triangle_strip_2x.png
            // This works by generating zero-area triangles that don't end up getting rendered.
            // TODO: How do we handle DEBUG_LINE_STRIP?
            if (RenderTypeUtil.isTriangleStripDrawMode(renderType)) {
                ((BufferBuilderExt) builder).splitStrip();
            }

            return builder;
        } catch (OutOfMemoryError e) {
            weAreOutOfMemory();

            // uhhh try again
            return getBuffer(renderType);
        }
    }

    private void weAreOutOfMemory() {
        parent.weAreOutOfMemory();
    }

    private void endAndGenSegmentForType(RenderType renderType) {
        var bufferBuilder = builders.remove(renderType);

        if (bufferBuilder == null) {
            return;
        }

        try {
            MeshData meshData = bufferBuilder.build();

            if (meshData == null) return;

            if (shouldSortOnUpload(renderType)) {
                meshData.sortQuads(buffers.get(renderType).getBuffer(), RenderSystem.getVertexSorting());
            }

            segments.add(new BufferSegment(meshData, renderType));
        } catch (OutOfMemoryError e) {
            // we're freaked. try to clear memory for the next one, but don't bother about this one.

            weAreOutOfMemory();
        }
    }

    public List<BufferSegment> getSegments() {
        if (!builders.isEmpty()) {
            List.copyOf(builders.keySet()).forEach(this::endAndGenSegmentForType);
        }

        List<BufferSegment> finalSegments = new ArrayList<>(segments);

        segments.clear();

        return finalSegments;
    }

    @Override
    public long getAllocatedSize() {
        long usedSize = 0;
        for (ByteBufferBuilderHolder buffer : buffers.values()) {
            usedSize += ((MemoryTrackingBuffer) buffer).getAllocatedSize();
        }

        return usedSize;
    }

    @Override
    public long getUsedSize() {
        long usedSize = 0;
        for (ByteBufferBuilderHolder buffer : buffers.values()) {
            usedSize += ((MemoryTrackingBuffer) buffer).getUsedSize();
        }

        return usedSize;
    }

    @Override
    public void freeAndDeleteBuffer() {
        for (ByteBufferBuilderHolder buffer : buffers.values()) {
            buffer.forceDelete();
        }

        buffers.clear();
    }

    public void clearBuffers(int clearTime) {
        buffers.values().removeIf(b -> b.deleteOrClear(clearTime));
    }

    public void lastDitchAttempt() {
        // JUST REMOVE ANYTHING UNDER 500MS
        buffers.values().removeIf(b -> b.delete(500));
    }
}

*///?} else {
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.batchedentityrendering.mixin.RenderTypeAccessor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SegmentedBufferBuilder implements MultiBufferSource, MemoryTrackingBuffer {
	private final BufferBuilder buffer;
	private final List<BufferSegment> buffers;
	private RenderType currentType;

	public SegmentedBufferBuilder(FullyBufferedMultiBufferSource parent) {
		// 2 MB initial allocation
		this.buffer = new BufferBuilder(512 * 1024);
		this.buffers = new ArrayList<>();
		this.currentType = null;
	}

	private static boolean shouldSortOnUpload(RenderType type) {
		return ((RenderTypeAccessor) type).shouldSortOnUpload();
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		if (!Objects.equals(currentType, renderType) || renderType.mode() == VertexFormat.Mode.TRIANGLE_FAN || renderType.mode() == VertexFormat.Mode.DEBUG_LINE_STRIP || renderType.mode() == VertexFormat.Mode.LINE_STRIP) {
			if (currentType != null) {
				if (shouldSortOnUpload(currentType)) {
					buffer.setQuadSorting(RenderSystem.getVertexSorting());
				}

				buffers.add(new BufferSegment(Objects.requireNonNull(buffer.end()), currentType));
			}

			buffer.begin(renderType.mode(), renderType.format());

			currentType = renderType;
		}

		// Use duplicate vertices to break up triangle strips
		// https://developer.apple.com/library/archive/documentation/3DDrawing/Conceptual/OpenGLES_ProgrammingGuide/Art/degenerate_triangle_strip_2x.png
		// This works by generating zero-area triangles that don't end up getting rendered.
		// TODO: How do we handle DEBUG_LINE_STRIP?
		if (RenderTypeUtil.isTriangleStripDrawMode(currentType)) {
			((BufferBuilderExt) buffer).splitStrip();
		}

		return buffer;
	}

	public List<BufferSegment> getSegments() {
		if (currentType == null) {
			return Collections.emptyList();
		}

		if (shouldSortOnUpload(currentType)) {
			buffer.setQuadSorting(RenderSystem.getVertexSorting());
		}

		buffers.add(new BufferSegment(Objects.requireNonNull(buffer.end()), currentType));

		currentType = null;

		List<BufferSegment> finalSegments = new ArrayList<>(buffers);

		buffers.clear();

		return finalSegments;
	}

	public List<BufferSegment> getSegmentsForType(TransparencyType transparencyType) {
		if (currentType == null) {
			return Collections.emptyList();
		}

		if (((BlendingStateHolder) currentType).getTransparencyType() == transparencyType) {
			if (shouldSortOnUpload(currentType)) {
				buffer.setQuadSorting(RenderSystem.getVertexSorting());
			}

			buffers.add(new BufferSegment(Objects.requireNonNull(buffer.end()), currentType));

			currentType = null;
		}

		List<BufferSegment> finalSegments = buffers.stream().filter(segment -> ((BlendingStateHolder) segment.type()).getTransparencyType() == transparencyType).toList();

		buffers.removeAll(finalSegments);

		return finalSegments;
	}

	@Override
	public long getAllocatedSize() {
		return ((MemoryTrackingBuffer) buffer).getAllocatedSize();
	}

	@Override
	public long getUsedSize() {
		return ((MemoryTrackingBuffer) buffer).getUsedSize();
	}

	@Override
	public void freeAndDeleteBuffer() {
		((MemoryTrackingBuffer) buffer).freeAndDeleteBuffer();
	}

    public void lastDitchAttempt() {

    }

    public void clearBuffers(int clearTime) {

    }
}
//?}
