package org.embeddedt.embeddium.impl.render.chunk.occlusion.geometry;

import grondag.bitraster.PackedBox;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class OccluderBoxes {
    /** Below this a box costs a rasterizer draw for negligible screen coverage. */
    private static final int MIN_VOLUME = 8;

    /** {@link BoxFinder} is far too large to allocate per section; build threads are pooled */
    private static final ThreadLocal<BoxFinder> FINDER = ThreadLocal.withInitial(() -> new BoxFinder(new AreaFinder()));

    private OccluderBoxes() {
    }

    public static int @Nullable [] from(long[] opacityBits) {
        BoxFinder finder = FINDER.get();
        finder.findBoxes(opacityBits, 0);

        var found = finder.boxes;
        int count = found.size();

        if (count == 0) {
            return null;
        }

        int[] boxes = new int[count];
        int kept = 0;

        for (int range = PackedBox.RANGE_EXTREME; range >= PackedBox.RANGE_NEAR; range--) {
            for (int i = 0; i < count; i++) {
                int box = found.getInt(i);

                if (PackedBox.range(box) == range && volume(box) >= MIN_VOLUME) {
                    boxes[kept++] = toWorldAxes(box);
                }
            }
        }

        if (kept == 0) {
            return null;
        }

        return kept == count ? boxes : Arrays.copyOf(boxes, kept);
    }

    private static int toWorldAxes(int box) {
        return PackedBox.pack(
                PackedBox.x0(box), PackedBox.z0(box), PackedBox.y0(box),
                PackedBox.x1(box), PackedBox.z1(box), PackedBox.y1(box),
                PackedBox.range(box)
        );
    }

    private static int volume(int box) {
        return (PackedBox.x1(box) - PackedBox.x0(box))
                * (PackedBox.y1(box) - PackedBox.y0(box))
                * (PackedBox.z1(box) - PackedBox.z0(box));
    }
}
