package org.embeddedt.embeddium.impl.render.chunk.map;

import it.unimi.dsi.fastutil.longs.*;
import org.embeddedt.embeddium.impl.util.PositionUtil;

public class ChunkTracker implements ClientChunkEventListener {
    private final Long2IntOpenHashMap chunkStatus = new Long2IntOpenHashMap();
    private final LongOpenHashSet chunkReady = new LongOpenHashSet();

    private final LongSet unloadQueue = new LongOpenHashSet();
    private final LongSet loadQueue = new LongOpenHashSet();

    private int requiredNeighborRadius;

    public ChunkTracker() {
        this(1);
    }

    /**
     * Constructs a new chunk tracker.
     * @param requiredNeighborRadius the radius of chunks around a given chunk that must be available before the chunk
     *                               itself is considered loaded (0 requires no chunks to be loaded, 1 requires
     *                               all adjacent chunks). Note that a radius of 0 is guaranteed to produce incorrect
     *                               rendering of the edge chunks for blocks that rely on data from the adjacent chunk
     *                               (e.g. fences, fluids). Vanilla handles this by updating the chunks again as
     *                               neighbors load, but this wastes CPU time and looks bad
     */
    public ChunkTracker(int requiredNeighborRadius) {
        if (requiredNeighborRadius < 0) {
            throw new IllegalArgumentException("requiredNeighborRadius must be nonnegative");
        }
        this.requiredNeighborRadius = requiredNeighborRadius;
    }

    public void setRequiredNeighborRadius(int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("radius must be nonnegative");
        }
        if (this.requiredNeighborRadius == radius) {
            return;
        }
        boolean fullUpdate = radius > this.requiredNeighborRadius;
        if (fullUpdate) {
            // The requirement is now stricter; so we must clear chunkReady
            var readyIterator = this.chunkReady.iterator();
            while (readyIterator.hasNext()) {
                long key = readyIterator.nextLong();
                if (!this.loadQueue.remove(key)) {
                    this.unloadQueue.add(key);
                }
            }
            this.chunkReady.clear();
        }
        this.requiredNeighborRadius = radius;
        // Recompute status of each chunk; this will repopulate chunkReady
        var trackedChunksIterator = this.chunkStatus.keySet().iterator();
        while (trackedChunksIterator.hasNext()) {
            long pos = trackedChunksIterator.nextLong();
            if (!fullUpdate && this.chunkReady.contains(pos)) {
                continue;
            }
            var x = PositionUtil.unpackChunkX(pos);
            var z = PositionUtil.unpackChunkZ(pos);
            this.updateMerged(x, z);
        }
    }

    @Override
    public void updateMapCenter(int chunkX, int chunkZ) {

    }

    @Override
    public void updateLoadDistance(int loadDistance) {

    }

    @Override
    public void onChunkStatusAdded(int x, int z, int flags) {
        var key = PositionUtil.packChunk(x, z);

        var prev = this.chunkStatus.get(key);
        var cur = prev | flags;

        if (prev == cur) {
            return;
        }

        this.chunkStatus.put(key, cur);

        this.updateNeighbors(x, z);
    }

    @Override
    public void onChunkStatusRemoved(int x, int z, int flags) {
        var key = PositionUtil.packChunk(x, z);

        var prev = this.chunkStatus.get(key);
        int cur = prev & ~flags;

        if (prev == cur) {
            return;
        }

        if (cur == this.chunkStatus.defaultReturnValue()) {
            this.chunkStatus.remove(key);
        } else {
            this.chunkStatus.put(key, cur);
        }

        this.updateNeighbors(x, z);
    }

    private void updateNeighbors(int x, int z) {
        int r = this.requiredNeighborRadius;
        for (int ox = -r; ox <= r; ox++) {
            for (int oz = -r; oz <= r; oz++) {
                this.updateMerged(ox + x, oz + z);
            }
        }
    }

    private void updateMerged(int x, int z) {
        long key = PositionUtil.packChunk(x, z);

        int r = this.requiredNeighborRadius;
        int flags = this.chunkStatus.get(key);

        for (int ox = -r; ox <= r; ox++) {
            for (int oz = -r; oz <= r; oz++) {
                flags &= this.chunkStatus.get(PositionUtil.packChunk(ox + x, oz + z));
            }
        }

        if (flags == ChunkStatus.FLAG_ALL) {
            if (this.chunkReady.add(key) && !this.unloadQueue.remove(key)) {
                this.loadQueue.add(key);
            }
        } else {
            if (this.chunkReady.remove(key) && !this.loadQueue.remove(key)) {
                this.unloadQueue.add(key);
            }
        }
    }

    public LongCollection getReadyChunks() {
        return LongSets.unmodifiable(this.chunkReady);
    }

    public void forEachEvent(ChunkEventHandler loadEventHandler, ChunkEventHandler unloadEventHandler) {
        forEachChunk(this.unloadQueue, unloadEventHandler);
        this.unloadQueue.clear();

        forEachChunk(this.loadQueue, loadEventHandler);
        this.loadQueue.clear();
    }

    public static void forEachChunk(LongCollection queue, ChunkEventHandler handler) {
        var iterator = queue.iterator();

        while (iterator.hasNext()) {
            var pos = iterator.nextLong();

            var x = PositionUtil.unpackChunkX(pos);
            var z = PositionUtil.unpackChunkZ(pos);

            handler.apply(x, z);
        }
    }

    public interface ChunkEventHandler {
        void apply(int x, int z);
    }
}