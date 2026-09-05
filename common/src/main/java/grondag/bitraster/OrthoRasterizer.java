/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.bitraster;

import static grondag.bitraster.Constants.PROJECTED_VERTEX_STRIDE;
import static grondag.bitraster.Constants.DEFAULT_SIZE_TILES;
import static grondag.bitraster.Constants.PV_PX;
import static grondag.bitraster.Constants.PV_PY;
import static grondag.bitraster.Constants.PV_X;
import static grondag.bitraster.Constants.PV_Y;

public final class OrthoRasterizer extends AbstractRasterizer {
	public OrthoRasterizer() {
		this(DEFAULT_SIZE_TILES, DEFAULT_SIZE_TILES);
	}

	public OrthoRasterizer(int tileWidth, int tileHeight) {
		super(tileWidth, tileHeight);
	}

	@Override
	int prepareBounds(int v0, int v1, int v2, int v3) {
		return prepareBoundsNoClip(v0, v1, v2, v3);
	}

	@Override
	boolean setupBoxVertices(int[] corners, int x0, int y0, int z0, int x1, int y1, int z1) {
		transformBoxCorners(x0, y0, z0, x1, y1, z1);

		final int[] data = vertexData;
		final float[] cx = cornerX, cy = cornerY;

		for (int baseIndex : corners) {
			final int corner = baseIndex / PROJECTED_VERTEX_STRIDE;
			final float tx = cx[corner];
			final float ty = cy[corner];

			data[baseIndex + PV_X] = Float.floatToRawIntBits(tx);
			data[baseIndex + PV_Y] = Float.floatToRawIntBits(ty);
			data[baseIndex + PV_PX] = Math.round(tx * halfPreciseWidth) + halfPreciseWidth;
			data[baseIndex + PV_PY] = Math.round(ty * halfPreciseHeight) + halfPreciseHeight;
		}

		return true;
	}

	@Override
	boolean isPointClear(float x, float y, float z) {
		final float fx = x + offsetX;
		final float fy = y + offsetY;
		final float fz = z + offsetZ;

		final float tx = m00 * fx + m01 * fy + m02 * fz + m03;
		final float ty = m10 * fx + m11 * fy + m12 * fz + m13;

		return isProjectedPointClear(tx, ty, 1f);
	}

	@Override void setupVertex(final int baseIndex, final int x, final int y, final int z) {
		final int[] data = vertexData;

		final float fx = x + offsetX;
		final float fy = y + offsetY;
		final float fz = z + offsetZ;

		final float tx = m00 * fx + m01 * fy + m02 * fz + m03;
		final float ty = m10 * fx + m11 * fy + m12 * fz + m13;

		data[baseIndex + PV_X] = Float.floatToRawIntBits(tx);
		data[baseIndex + PV_Y] = Float.floatToRawIntBits(ty);

		final int px = Math.round(tx * halfPreciseWidth) + halfPreciseWidth;
		final int py = Math.round(ty * halfPreciseHeight) + halfPreciseHeight;

		data[baseIndex + PV_PX] = px;
		data[baseIndex + PV_PY] = py;
	}
}
