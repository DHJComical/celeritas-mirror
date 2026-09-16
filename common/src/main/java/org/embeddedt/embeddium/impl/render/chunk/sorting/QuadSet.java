package org.embeddedt.embeddium.impl.render.chunk.sorting;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;

public record QuadSet(int count, float[] centers, float[] bounds, float[] normals, float[] dots, byte[] facings) {
    static final float EPSILON = 1e-4f;

    public QuadSet {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }

        if (centers.length != count * 3) {
            throw new IllegalArgumentException("centers length must be count * 3");
        }

        if (bounds.length != count * 6) {
            throw new IllegalArgumentException("bounds length must be count * 6");
        }

        if (normals.length != count * 3) {
            throw new IllegalArgumentException("normals length must be count * 3");
        }

        if (dots.length != count) {
            throw new IllegalArgumentException("dots length must be count");
        }

        if (facings.length != count) {
            throw new IllegalArgumentException("facings length must be count");
        }
    }

    /**
     * The smallest projection of the quad's bounds onto {@code (nx, ny, nz)}. The nearest corner is the one that is
     * lowest along each axis the direction points up, and highest along each axis it points down, so no corner
     * enumeration is needed.
     */
    public float minDot(int quad, float nx, float ny, float nz) {
        int boundOffset = quad * 6;

        return nx * (nx >= 0 ? bounds[boundOffset + 0] : bounds[boundOffset + 3])
                + ny * (ny >= 0 ? bounds[boundOffset + 1] : bounds[boundOffset + 4])
                + nz * (nz >= 0 ? bounds[boundOffset + 2] : bounds[boundOffset + 5]);
    }

    /**
     * The largest projection of the quad's bounds onto {@code (nx, ny, nz)}: {@link #minDot} with the corner choices
     * reversed.
     */
    public float maxDot(int quad, float nx, float ny, float nz) {
        int boundOffset = quad * 6;

        return nx * (nx >= 0 ? bounds[boundOffset + 3] : bounds[boundOffset + 0])
                + ny * (ny >= 0 ? bounds[boundOffset + 4] : bounds[boundOffset + 1])
                + nz * (nz >= 0 ? bounds[boundOffset + 5] : bounds[boundOffset + 2]);
    }

    public boolean isCoplanar(int a, int b) {
        float normalDot = normalDot(a, b);

        if (Math.abs(Math.abs(normalDot) - 1.0f) > EPSILON) {
            return false;
        }

        float offsetB = normalDot < 0 ? -dots[b] : dots[b];
        return Math.abs(dots[a] - offsetB) < EPSILON;
    }

    public float normalDot(int a, int b) {
        int ao = a * 3, bo = b * 3;

        return normals[ao] * normals[bo] + normals[ao + 1] * normals[bo + 1] + normals[ao + 2] * normals[bo + 2];
    }

    public boolean isDegenerate(int quad) {
        int normalOffset = quad * 3;

        return normals[normalOffset] == 0 && normals[normalOffset + 1] == 0 && normals[normalOffset + 2] == 0;
    }

    public float normalX(int quad) {
        return normals[quad * 3];
    }

    public float normalY(int quad) {
        return normals[quad * 3 + 1];
    }

    public float normalZ(int quad) {
        return normals[quad * 3 + 2];
    }

    public ModelQuadFacing facing(int quad) {
        return ModelQuadFacing.VALUES[facings[quad]];
    }

    public Bounds union() {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        for (int quad = 0; quad < count; quad++) {
            int boundOffset = quad * 6;

            minX = Math.min(minX, bounds[boundOffset + 0]);
            minY = Math.min(minY, bounds[boundOffset + 1]);
            minZ = Math.min(minZ, bounds[boundOffset + 2]);
            maxX = Math.max(maxX, bounds[boundOffset + 3]);
            maxY = Math.max(maxY, bounds[boundOffset + 4]);
            maxZ = Math.max(maxZ, bounds[boundOffset + 5]);
        }

        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        public float minDot(float nx, float ny, float nz) {
            return nx * (nx >= 0 ? minX : maxX) + ny * (ny >= 0 ? minY : maxY) + nz * (nz >= 0 ? minZ : maxZ);
        }

        public float maxDot(float nx, float ny, float nz) {
            return nx * (nx >= 0 ? maxX : minX) + ny * (ny >= 0 ? maxY : minY) + nz * (nz >= 0 ? maxZ : minZ);
        }
    }

    /** if p must be drawn before q */
    public boolean isSeenThrough(int p, int q) {
        return !isCoplanar(p, q) && minDot(p, normalX(q), normalY(q), normalZ(q)) < dots[q] - EPSILON
                && maxDot(q, normalX(p), normalY(p), normalZ(p)) > dots[p] + EPSILON;
    }

    public float boundsMin(int quad, int axis) {
        return bounds[quad * 6 + axis];
    }

    public float boundsMax(int quad, int axis) {
        return bounds[quad * 6 + 3 + axis];
    }
}
