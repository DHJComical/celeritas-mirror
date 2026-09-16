package org.embeddedt.embeddium.impl.render.chunk.sorting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.function.Consumer;

public final class CutPlaneIndex<T> {
    /** movements larger than this many buckets rescan every owner instead of walking the buckets */
    static final int MAX_BUCKET_SPAN = 64;

    private record Registration<T>(float[][] cuts, int[] origin, long[][] buckets) {
    }

    @SuppressWarnings("unchecked")
    private final Long2ObjectOpenHashMap<ReferenceOpenHashSet<T>>[] bucketsByAxis = new Long2ObjectOpenHashMap[] {
            new Long2ObjectOpenHashMap<>(), new Long2ObjectOpenHashMap<>(), new Long2ObjectOpenHashMap<>()
    };

    private final Reference2ObjectOpenHashMap<T, Registration<T>> registrations = new Reference2ObjectOpenHashMap<>();

    private final ReferenceOpenHashSet<T> reported = new ReferenceOpenHashSet<>();

    private int planeCount;

    public void put(T owner, float[][] cutsByAxis, int originX, int originY, int originZ) {
        remove(owner);

        if (cutsByAxis[0].length == 0 && cutsByAxis[1].length == 0 && cutsByAxis[2].length == 0) {
            // a leaf's order can never change, so it's not worth indexing
            return;
        }

        this.planeCount += cutsByAxis[0].length + cutsByAxis[1].length + cutsByAxis[2].length;

        int[] origin = { originX, originY, originZ };
        long[][] buckets = new long[3][];

        for (int axis = 0; axis < 3; axis++) {
            LongArrayList keys = new LongArrayList();
            long lastKey = 0;
            boolean first = true;

            // cutsByAxis[axis] is sorted, so equal bucket keys are adjacent
            for (float cut : cutsByAxis[axis]) {
                long key = bucketKey(cut, origin[axis]);
                if (first || key != lastKey) {
                    keys.add(key);
                    lastKey = key;
                    first = false;
                }
            }

            buckets[axis] = keys.toLongArray();

            for (long key : buckets[axis]) {
                this.bucketsByAxis[axis].computeIfAbsent(key, k -> new ReferenceOpenHashSet<>()).add(owner);
            }
        }

        this.registrations.put(owner, new Registration<>(cutsByAxis, origin, buckets));
    }

    public void remove(T owner) {
        Registration<T> registration = this.registrations.remove(owner);

        if (registration == null) {
            return;
        }

        this.planeCount -= registration.cuts[0].length + registration.cuts[1].length + registration.cuts[2].length;

        for (int axis = 0; axis < 3; axis++) {
            var map = this.bucketsByAxis[axis];

            for (long key : registration.buckets[axis]) {
                ReferenceOpenHashSet<T> owners = map.get(key);
                if (owners == null) continue;

                owners.remove(owner);

                if (owners.isEmpty()) {
                    map.remove(key);
                }
            }
        }
    }

    public void query(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, Consumer<T> out) {
        if (this.registrations.isEmpty()) {
            return;
        }

        try {
            queryAxis(0, fromX, toX, out);
            queryAxis(1, fromY, toY, out);
            queryAxis(2, fromZ, toZ, out);
        } finally {
            this.reported.clear();
        }
    }

    private void queryAxis(int axis, double from, double to, Consumer<T> out) {
        double lo = Math.min(from, to), hi = Math.max(from, to);

        if (lo == hi) return;

        long loBucket = (long) Math.floor(lo);
        long hiBucket = (long) Math.floor(hi);

        if (hiBucket - loBucket > MAX_BUCKET_SPAN) {
            var iterator = this.registrations.reference2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                considerCandidate(entry.getKey(), entry.getValue(), axis, lo, hi, out);
            }
            return;
        }

        var map = this.bucketsByAxis[axis];

        for (long bucket = loBucket; bucket <= hiBucket; bucket++) {
            ReferenceOpenHashSet<T> candidates = map.get(bucket);

            if (candidates == null) {
                continue;
            }

            for (T candidate : candidates) {
                considerCandidate(candidate, this.registrations.get(candidate), axis, lo, hi, out);
            }
        }
    }

    private void considerCandidate(T owner, Registration<T> registration, int axis, double lo, double hi, Consumer<T> out) {
        if (registration == null || this.reported.contains(owner)) {
            return;
        }

        double loLocal = lo - registration.origin[axis];
        double hiLocal = hi - registration.origin[axis];

        for (float cut : registration.cuts[axis]) {
            if (cut > loLocal && cut <= hiLocal) {
                this.reported.add(owner);
                out.accept(owner);
                return;
            }
        }
    }

    private static long bucketKey(float localCoord, int origin) {
        return (long) Math.floor((double) localCoord + (double) origin);
    }

    public int size() {
        return this.registrations.size();
    }

    public int planeCount() {
        return this.planeCount;
    }
}
