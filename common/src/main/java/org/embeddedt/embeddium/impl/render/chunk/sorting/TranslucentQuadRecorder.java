package org.embeddedt.embeddium.impl.render.chunk.sorting;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.QuadUtil;
import org.joml.Vector3f;

public class TranslucentQuadRecorder {
    private static final int EXPECTED_QUADS = 1000;

    private final FloatArrayList quadCenters = new FloatArrayList(EXPECTED_QUADS * 3);
    private final FloatArrayList quadBounds = new FloatArrayList(EXPECTED_QUADS * 6);
    private final FloatArrayList quadNormals = new FloatArrayList(EXPECTED_QUADS * 3);
    private final FloatArrayList quadDots = new FloatArrayList(EXPECTED_QUADS);
    private final ByteArrayList quadFacings = new ByteArrayList(EXPECTED_QUADS);

    private final Vector3f[] vertexPositions = new Vector3f[4];
    private final Vector3f currentNormal = new Vector3f();
    private int currentVertex;

    public TranslucentQuadRecorder() {
        for(int i = 0; i < 4; i++) {
            vertexPositions[i] = new Vector3f();
        }
    }

    public SortState getSortState() {
        return TranslucentSorter.analyze(this.toQuadSet());
    }

    public void clear() {
        quadCenters.clear();
        quadBounds.clear();
        quadNormals.clear();
        quadDots.clear();
        quadFacings.clear();
        currentVertex = 0;
    }

    private void calculateNormal() {
        final Vector3f v0 = vertexPositions[0];

        final float x0 = v0.x;
        final float y0 = v0.y;
        final float z0 = v0.z;

        final Vector3f v1 = vertexPositions[1];

        final float x1 = v1.x;
        final float y1 = v1.y;
        final float z1 = v1.z;

        final Vector3f v2 = vertexPositions[2];

        final float x2 = v2.x;
        final float y2 = v2.y;
        final float z2 = v2.z;

        final Vector3f v3 = vertexPositions[3];

        final float x3 = v3.x;
        final float y3 = v3.y;
        final float z3 = v3.z;

        final float dx0 = x2 - x0;
        final float dy0 = y2 - y0;
        final float dz0 = z2 - z0;
        final float dx1 = x3 - x1;
        final float dy1 = y3 - y1;
        final float dz1 = z3 - z1;

        float normX = dy0 * dz1 - dz0 * dy1;
        float normY = dz0 * dx1 - dx0 * dz1;
        float normZ = dx0 * dy1 - dy0 * dx1;

        float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

        if (l != 0) {
            normX /= l;
            normY /= l;
            normZ /= l;
        }

        currentNormal.set(normX, normY, normZ);
    }

    private void captureQuad() {
        // The four positions in vertexPositions form a quad. Find its center and axis-aligned bounds
        float totalX = 0, totalY = 0, totalZ = 0;
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        for (Vector3f vertex : vertexPositions) {
            totalX += vertex.x;
            totalY += vertex.y;
            totalZ += vertex.z;

            minX = Math.min(minX, vertex.x);
            minY = Math.min(minY, vertex.y);
            minZ = Math.min(minZ, vertex.z);
            maxX = Math.max(maxX, vertex.x);
            maxY = Math.max(maxY, vertex.y);
            maxZ = Math.max(maxZ, vertex.z);
        }

        float centerX = totalX / 4, centerY = totalY / 4, centerZ = totalZ / 4;

        quadCenters.add(centerX);
        quadCenters.add(centerY);
        quadCenters.add(centerZ);

        quadBounds.add(minX);
        quadBounds.add(minY);
        quadBounds.add(minZ);
        quadBounds.add(maxX);
        quadBounds.add(maxY);
        quadBounds.add(maxZ);

        calculateNormal();

        quadNormals.add(currentNormal.x);
        quadNormals.add(currentNormal.y);
        quadNormals.add(currentNormal.z);

        quadDots.add(currentNormal.x * centerX + currentNormal.y * centerY + currentNormal.z * centerZ);

        quadFacings.add((byte) QuadUtil.findNormalFace(currentNormal.x, currentNormal.y, currentNormal.z).ordinal());
    }

    public void capture(ChunkVertexEncoder.Vertex vertex) {
        int i = currentVertex;
        vertexPositions[i].set(vertex.x, vertex.y, vertex.z);
        i++;
        if(i == 4) {
            captureQuad();
            i = 0;
        }
        currentVertex = i;
    }

    public QuadSet toQuadSet() {
        return new QuadSet(quadCenters.size() / 3, quadCenters.toFloatArray(), quadBounds.toFloatArray(),
                quadNormals.toFloatArray(), quadDots.toFloatArray(), quadFacings.toByteArray());
    }
}
