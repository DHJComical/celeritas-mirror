package org.embeddedt.embeddium.impl.render.chunk.data;

import org.embeddedt.embeddium.impl.gl.util.VertexRange;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;

import java.util.Map;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

// This code is a terrible hack to get around the fact that we are so incredibly memory bound, and that we
// have no control over memory layout. The chunk rendering code spends an astronomical amount of time chasing
// object pointers that are scattered across the heap. Worse yet, because render state is initialized over a long
// period of time as the world loads, those objects are never even remotely close to one another in heap, so
// you also have to pay the penalty of a DTLB miss on every other access.
//
// Unfortunately, Hotspot *still* produces abysmal machine code for the chunk rendering code paths, since any usage of
// unsafe memory intrinsics seems to cause it to become paranoid about memory aliasing. Well, that, and it just produces
// terrible machine code in pretty much every critical code path we seem to have...
//
// Please never try to write performance critical code in Java. This is what it will do to you. And you will still be
// three times slower than the most naive solution in literally any other language that LLVM can compile.
public class SectionRenderDataUnsafe {
    private static final int NUM_FACINGS = ModelQuadFacing.COUNT; // 6 directions + UNASSIGNED

    /**
     * The slice mask lives at offset 0 in <em>every</em> layout, so code which only needs to know which facings are
     * populated (e.g. the render list culling pass) can read it without knowing which {@link Strategy} produced the row.
     */
    private static final long OFFSET_SLICE_MASK = 0;

    public static int getSliceMask(long ptr) {
        return LWJGL.memGetInt(ptr + OFFSET_SLICE_MASK);
    }

    private static void setSliceMask(long ptr, int value) {
        LWJGL.memPutInt(ptr + OFFSET_SLICE_MASK, value);
    }

    /**
     * {@return the number of index buffer elements needed to draw a span of {@code vertexSpan} vertices}
     */
    public static int elementsForVertices(int vertexSpan, ChunkPrimitiveType primitiveType) {
        return (vertexSpan / primitiveType.getVerticesPerPrimitive()) * primitiveType.getIndexBufferElementsPerPrimitive();
    }

    /**
     * The inverse of {@link #elementsForVertices}.
     */
    public static int verticesForElements(int elementCount, ChunkPrimitiveType primitiveType) {
        return (elementCount / primitiveType.getIndexBufferElementsPerPrimitive()) * primitiveType.getVerticesPerPrimitive();
    }

    /**
     * Memory layouts for a single section's mesh data. Which one a {@link SectionRenderDataStorage} uses is fixed for
     * its lifetime and determined by whether its pass is translucency-sorted.
     * <p>
     * The write side of this interface is expressed as whole operations rather than per-field setters, because the two
     * layouts do not store the same fields. {@link #COMPACT} derives element counts and has no index offsets at all.
     * Operations that a layout genuinely cannot express throw.
     */
    public enum Strategy {
        /**
         * The general layout. Stores, for each facing, an explicit vertex offset, element count and index offset.
         * <pre>
         * u64 slice_mask;
         * struct { u32 vertex_offset; u32 element_count; u32 index_offset; } facings[7];
         * </pre>
         * Required for sorted passes, which have per-facing index offsets into a per-section sorted index buffer.
         */
        FULL {
            private static final long OFFSET_SLICE_RANGES = 8;
            private static final long DATA_PER_FACING_SIZE = 12;

            private static long pVertexOffset(long ptr, int facing) {
                return ptr + OFFSET_SLICE_RANGES + (facing * DATA_PER_FACING_SIZE) + 0L;
            }

            private static long pElementCount(long ptr, int facing) {
                return ptr + OFFSET_SLICE_RANGES + (facing * DATA_PER_FACING_SIZE) + 4L;
            }

            private static long pIndexOffset(long ptr, int facing) {
                return ptr + OFFSET_SLICE_RANGES + (facing * DATA_PER_FACING_SIZE) + 8L;
            }

            @Override
            public long getStride() {
                return OFFSET_SLICE_RANGES + (DATA_PER_FACING_SIZE * NUM_FACINGS);
            }

            @Override
            public int getVertexOffset(long ptr, int facing) {
                return LWJGL.memGetInt(pVertexOffset(ptr, facing));
            }

            @Override
            public int getElementCount(long ptr, int facing, ChunkPrimitiveType primitiveType) {
                return LWJGL.memGetInt(pElementCount(ptr, facing));
            }

            @Override
            public int getIndexOffset(long ptr, int facing) {
                return LWJGL.memGetInt(pIndexOffset(ptr, facing));
            }

            @Override
            public int getRunVertexEnd(long ptr, int lastFacing, ChunkPrimitiveType primitiveType) {
                return LWJGL.memGetInt(pVertexOffset(ptr, lastFacing))
                        + verticesForElements(LWJGL.memGetInt(pElementCount(ptr, lastFacing)), primitiveType);
            }

            @Override
            public void writeMeshes(long ptr, int vertexOffset, int indexOffset,
                                    Map<ModelQuadFacing, VertexRange> ranges, ChunkPrimitiveType primitiveType) {
                int sliceMask = 0;

                for (int facing = 0; facing < NUM_FACINGS; facing++) {
                    int vertexCount = vertexCountOf(ranges, facing);
                    int elementCount = elementsForVertices(vertexCount, primitiveType);

                    LWJGL.memPutInt(pVertexOffset(ptr, facing), vertexOffset);
                    LWJGL.memPutInt(pElementCount(ptr, facing), elementCount);
                    LWJGL.memPutInt(pIndexOffset(ptr, facing), indexOffset);

                    if (vertexCount > 0) {
                        sliceMask |= 1 << facing;
                    }

                    vertexOffset += vertexCount;
                    indexOffset += elementCount * 4;
                }

                setSliceMask(ptr, sliceMask);
            }

            @Override
            public void writeIndexOffsets(long ptr, int indexOffset, ChunkPrimitiveType primitiveType) {
                for (int facing = 0; facing < NUM_FACINGS; facing++) {
                    LWJGL.memPutInt(pIndexOffset(ptr, facing), indexOffset);
                    indexOffset += LWJGL.memGetInt(pElementCount(ptr, facing)) * 4;
                }
            }

            @Override
            public void rebase(long ptr, int vertexOffset, int indexOffset, ChunkPrimitiveType primitiveType) {
                for (int facing = 0; facing < NUM_FACINGS; facing++) {
                    LWJGL.memPutInt(pVertexOffset(ptr, facing), vertexOffset);
                    LWJGL.memPutInt(pIndexOffset(ptr, facing), indexOffset);

                    int elementCount = LWJGL.memGetInt(pElementCount(ptr, facing));
                    vertexOffset += verticesForElements(elementCount, primitiveType);
                    indexOffset += elementCount * 4;
                }
            }
        },
        /**
         * The "fence post" layout, valid only for unsorted passes.
         * <pre>
         * u32 slice_mask;
         * u32 posts[8];
         * </pre>
         * It drops index offsets entirely (unnecessary for unsorted passes) and compresses vertex info by exploiting
         * the fact that facing vertex ranges are always contiguous in memory (in facing order), with empty facings having
         * zero-length spans.
         */
        COMPACT {
            private static final long OFFSET_POSTS = 4;
            private static final int NUM_POSTS = NUM_FACINGS + 1;

            private static long pPost(long ptr, int post) {
                return ptr + OFFSET_POSTS + ((long) post << 2);
            }

            @Override
            public long getStride() {
                return OFFSET_POSTS + (4L * NUM_POSTS);
            }

            @Override
            public int getVertexOffset(long ptr, int facing) {
                return LWJGL.memGetInt(pPost(ptr, facing));
            }

            @Override
            public int getElementCount(long ptr, int facing, ChunkPrimitiveType primitiveType) {
                int start = LWJGL.memGetInt(pPost(ptr, facing));
                int end = LWJGL.memGetInt(pPost(ptr, facing + 1));

                return elementsForVertices(end - start, primitiveType);
            }

            @Override
            public int getIndexOffset(long ptr, int facing) {
                // Unsorted passes always draw through the shared index buffer, which starts at pointer zero.
                return 0;
            }

            @Override
            public int getRunVertexEnd(long ptr, int lastFacing, ChunkPrimitiveType primitiveType) {
                return LWJGL.memGetInt(pPost(ptr, lastFacing + 1));
            }

            @Override
            public void writeMeshes(long ptr, int vertexOffset, int indexOffset,
                                    Map<ModelQuadFacing, VertexRange> ranges, ChunkPrimitiveType primitiveType) {
                int sliceMask = 0;

                for (int facing = 0; facing < NUM_FACINGS; facing++) {
                    int vertexCount = vertexCountOf(ranges, facing);

                    LWJGL.memPutInt(pPost(ptr, facing), vertexOffset);

                    if (vertexCount > 0) {
                        sliceMask |= 1 << facing;
                    }

                    vertexOffset += vertexCount;
                }

                LWJGL.memPutInt(pPost(ptr, NUM_FACINGS), vertexOffset);

                setSliceMask(ptr, sliceMask);
            }

            @Override
            public void writeIndexOffsets(long ptr, int indexOffset, ChunkPrimitiveType primitiveType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void rebase(long ptr, int vertexOffset, int indexOffset, ChunkPrimitiveType primitiveType) {
                int delta = vertexOffset - LWJGL.memGetInt(pPost(ptr, 0));

                if (delta == 0) {
                    return;
                }

                for (int post = 0; post < NUM_POSTS; post++) {
                    long pPost = pPost(ptr, post);
                    LWJGL.memPutInt(pPost, LWJGL.memGetInt(pPost) + delta);
                }
            }
        };

        public abstract long getStride();

        public abstract int getVertexOffset(long ptr, int facing);

        /**
         * @param primitiveType ignored by layouts which store explicit element counts
         */
        public abstract int getElementCount(long ptr, int facing, ChunkPrimitiveType primitiveType);

        /**
         * {@return the byte offset of this facing's indices, or zero if the layout draws from the shared index buffer}
         */
        public abstract int getIndexOffset(long ptr, int facing);

        /**
         * {@return the exclusive end of facing {@code lastFacing}'s vertex range}
         * <p>
         * Paired with {@link #getVertexOffset} on a run's first facing, this yields the run's whole vertex span in two
         * loads, and leaves the caller holding the start it needs as the command's base vertex. Merging a run into one
         * command is only valid under {@link #COMPACT}, whose facings all draw from the shared index buffer.
         */
        public abstract int getRunVertexEnd(long ptr, int lastFacing, ChunkPrimitiveType primitiveType);

        /**
         * Populates an entire row, including its slice mask, from a freshly uploaded mesh. {@code ranges} must be
         * contiguous in facing order starting at zero, as produced by the chunk build buffers.
         *
         * @param vertexOffset the section's base offset into the vertex arena, in vertices
         * @param indexOffset  the section's base offset into its index buffer, in bytes; layouts which draw from the
         *                     shared index buffer ignore this
         */
        public abstract void writeMeshes(long ptr, int vertexOffset, int indexOffset,
                                         Map<ModelQuadFacing, VertexRange> ranges, ChunkPrimitiveType primitiveType);

        /**
         * Rewrites every facing's index offset against a newly allocated index buffer, preserving element counts.
         *
         * @throws UnsupportedOperationException if the layout does not store index offsets
         */
        public abstract void writeIndexOffsets(long ptr, int indexOffset, ChunkPrimitiveType primitiveType);

        /**
         * Rewrites the row against new vertex and index arena offsets, preserving the per-facing sizes. Used when an
         * arena grows and existing allocations move.
         */
        public abstract void rebase(long ptr, int vertexOffset, int indexOffset, ChunkPrimitiveType primitiveType);

        public final long allocateHeap(int count) {
            return LWJGL.nmemCalloc(count, this.getStride());
        }

        public final void freeHeap(long pointer) {
            LWJGL.nmemFree(pointer);
        }

        public final void clear(long pointer) {
            LWJGL.memSet(pointer, 0x0, this.getStride());
        }

        public final long heapPointer(long ptr, int index) {
            return ptr + (index * this.getStride());
        }

        private static int vertexCountOf(Map<ModelQuadFacing, VertexRange> ranges, int facing) {
            VertexRange range = ranges.get(ModelQuadFacing.VALUES[facing]);

            return range != null ? range.vertexCount() : 0;
        }
    }
}
