package org.embeddedt.embeddium.impl.modern.sorting;

//? if >=1.20 {
import com.mojang.blaze3d.vertex.VertexSorting;
import org.embeddedt.embeddium.impl.util.sorting.MergeSort;
import org.joml.Vector3f;

public class VertexSorters {
    public static VertexSorting sortByDistance(Vector3f origin) {
        return new SortByDistance(origin);
    }

    private static class SortByDistance extends AbstractVertexSorter {
        private final Vector3f origin;

        private SortByDistance(Vector3f origin) {
            this.origin = origin;
        }

        @Override
        protected float getKey(Vector3f position) {
            return this.origin.distanceSquared(position);
        }
    }

    private static abstract class AbstractVertexSorter implements VertexSorting {
        @Override
        public final int[] sort(Vector3f[] positions) {
            return this.mergeSort(positions);
        }

        private int[] mergeSort(Vector3f[] positions) {
            final var keys = new float[positions.length];

            for (int index = 0; index < positions.length; index++) {
                keys[index] = this.getKey(positions[index]);
            }

            return MergeSort.mergeSort(keys);
        }

        protected abstract float getKey(Vector3f object);
    }
}
//?}
