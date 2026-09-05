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

import static grondag.bitraster.Constants.DEFAULT_SIZE_TILES;
import static grondag.bitraster.Constants.BOUNDS_IN;
import static grondag.bitraster.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.bitraster.Constants.EVENT_POSITION_MASK;
import static grondag.bitraster.Constants.IDX_AX0;
import static grondag.bitraster.Constants.IDX_AX1;
import static grondag.bitraster.Constants.IDX_AY0;
import static grondag.bitraster.Constants.IDX_AY1;
import static grondag.bitraster.Constants.IDX_BX0;
import static grondag.bitraster.Constants.IDX_BX1;
import static grondag.bitraster.Constants.IDX_BY0;
import static grondag.bitraster.Constants.IDX_BY1;
import static grondag.bitraster.Constants.IDX_CX0;
import static grondag.bitraster.Constants.IDX_CX1;
import static grondag.bitraster.Constants.IDX_CY0;
import static grondag.bitraster.Constants.IDX_CY1;
import static grondag.bitraster.Constants.IDX_DX0;
import static grondag.bitraster.Constants.IDX_DX1;
import static grondag.bitraster.Constants.IDX_DY0;
import static grondag.bitraster.Constants.IDX_DY1;
import static grondag.bitraster.Constants.PRECISION_BITS;
import static grondag.bitraster.Constants.PROJECTED_VERTEX_STRIDE;
import static grondag.bitraster.Constants.PV_PX;
import static grondag.bitraster.Constants.PV_PY;
import static grondag.bitraster.Constants.PV_W;
import static grondag.bitraster.Constants.PV_X;
import static grondag.bitraster.Constants.PV_Y;
import static grondag.bitraster.Constants.PV_Z;
import static grondag.bitraster.Constants.SCANT_PRECISE_PIXEL_CENTER;

public final class PerspectiveRasterizer extends AbstractRasterizer {
	public PerspectiveRasterizer() {
		this(DEFAULT_SIZE_TILES, DEFAULT_SIZE_TILES);
	}

	public PerspectiveRasterizer(int tileWidth, int tileHeight) {
		super(tileWidth, tileHeight);
	}

	/** Holds results of {@link #clipNear(int, int)}. */
	private int clipX, clipY;

	@Override void setupVertex(final int baseIndex, final int x, final int y, final int z) {
		final int[] data = vertexData;

		final float fx = x + offsetX;
		final float fy = y + offsetY;
		final float fz = z + offsetZ;

		final float tx = m00 * fx + m01 * fy + m02 * fz + m03;
		final float ty = m10 * fx + m11 * fy + m12 * fz + m13;
		final float tz = m20 * fx + m21 * fy + m22 * fz + m23;
		final float w = m30 * fx + m31 * fy + m32 * fz + m33;

		data[baseIndex + PV_X] = Float.floatToRawIntBits(tx);
		data[baseIndex + PV_Y] = Float.floatToRawIntBits(ty);
		data[baseIndex + PV_Z] = Float.floatToRawIntBits(tz);
		data[baseIndex + PV_W] = Float.floatToRawIntBits(w);

		if (w != 0) {
			final float iw = 1f / w;
			final int px = roundToInt(tx * iw * halfPreciseWidth) + halfPreciseWidth;
			final int py = roundToInt(ty * iw * halfPreciseHeight) + halfPreciseHeight;

			data[baseIndex + PV_PX] = px;
			data[baseIndex + PV_PY] = py;
		}
	}

	@Override
	boolean setupBoxVertices(int[] corners, int x0, int y0, int z0, int x1, int y1, int z1) {
		transformBoxCorners(x0, y0, z0, x1, y1, z1);

		final int[] data = vertexData;
		final float[] cx = cornerX, cy = cornerY, cw = cornerW, cz = cornerZ;
		final int halfPreciseWidth = this.halfPreciseWidth;
		final int halfPreciseHeight = this.halfPreciseHeight;

		// only the projected position is stored: the clip-space coordinates are needed by the near-clip
		// fallback alone, and that path recomputes every vertex it uses through setupVertex
		for (int baseIndex : corners) {
			final int corner = baseIndex / PROJECTED_VERTEX_STRIDE;
			final float w = cw[corner];
			final float tz = cz[corner];

			// same acceptance as needsNearClip
			if (!(w > 0 && tz > 0 && tz <= w)) {
				return false;
			}

			final float iw = 1f / w;
			data[baseIndex + PV_PX] = roundToInt(cx[corner] * iw * halfPreciseWidth) + halfPreciseWidth;
			data[baseIndex + PV_PY] = roundToInt(cy[corner] * iw * halfPreciseHeight) + halfPreciseHeight;
		}

		return true;
	}

	@Override
	boolean isPointClear(float x, float y, float z) {
		final float fx = x + offsetX;
		final float fy = y + offsetY;
		final float fz = z + offsetZ;

		final float w = m30 * fx + m31 * fy + m32 * fz + m33;

		// behind the eye; callers only ask about points well past the near plane
		if (w <= 0) {
			return false;
		}

		final float tx = m00 * fx + m01 * fy + m02 * fz + m03;
		final float ty = m10 * fx + m11 * fy + m12 * fz + m13;

		return isProjectedPointClear(tx, ty, 1f / w);
	}

	private void clipNear(int internal, int external) {
		final int[] data = vertexData;

		final float intX = Float.intBitsToFloat(data[internal + PV_X]);
		final float intY = Float.intBitsToFloat(data[internal + PV_Y]);
		final float intZ = Float.intBitsToFloat(data[internal + PV_Z]);
		final float intW = Float.intBitsToFloat(data[internal + PV_W]);

		final float extX = Float.intBitsToFloat(data[external + PV_X]);
		final float extY = Float.intBitsToFloat(data[external + PV_Y]);
		final float extZ = Float.intBitsToFloat(data[external + PV_Z]);
		final float extW = Float.intBitsToFloat(data[external + PV_W]);

		// intersection point is the projection plane, at which point Z == 1
		// and w will be 0 but projection division isn't needed, so force output to W = 1
		// see https://www.cs.usfca.edu/~cruse/math202s11/homocoords.pdf

		final float wt = intZ / -(extZ - intZ);

		// note again that projection division isn't needed
		final float x = (intX + (extX - intX) * wt);
		final float y = (intY + (extY - intY) * wt);
		final float w = (intW + (extW - intW) * wt);
		final float iw = 1f / w;

		clipX = roundToInt(iw * x * halfPreciseWidth) + halfPreciseWidth;
		clipY = roundToInt(iw * y * halfPreciseHeight) + halfPreciseHeight;
	}

	@Override
	int prepareBounds(int v0, int v1, int v2, int v3) {
		// puts bits in lexical order
		final int split = needsNearClip(v3) | (needsNearClip(v2) << 1) | (needsNearClip(v1) << 2) | (needsNearClip(v0) << 3);

		switch (split) {
			case 0b0000:
				return prepareBounds0000(v0, v1, v2, v3);

			case 0b0001:
				return prepareBounds0001(v0, v1, v2, v3);

			case 0b0010:
				return prepareBounds0001(v3, v0, v1, v2);

			case 0b0100:
				return prepareBounds0001(v2, v3, v0, v1);

			case 0b1000:
				return prepareBounds0001(v1, v2, v3, v0);

			case 0b0011:
				return prepareBounds0011(v0, v1, v2, v3);

			case 0b1001:
				return prepareBounds0011(v1, v2, v3, v0);

			case 0b1100:
				return prepareBounds0011(v2, v3, v0, v1);

			case 0b0110:
				return prepareBounds0011(v3, v0, v1, v2);

			case 0b0111:
				return prepareBounds0111(v0, v1, v2, v3);

			case 0b1011:
				return prepareBounds0111(v1, v2, v3, v0);

			case 0b1101:
				return prepareBounds0111(v2, v3, v0, v1);

			case 0b1110:
				return prepareBounds0111(v3, v0, v1, v2);

			case 0b1111:
				return BOUNDS_OUTSIDE_OR_TOO_SMALL;

			default:
				assert false : "Occlusion edge case";
				// NOOP
		}

		return BOUNDS_OUTSIDE_OR_TOO_SMALL;
	}

	private int prepareBounds0000(int v0, int v1, int v2, int v3) {
		final int[] data = vertexData;
		int ax0, ay0, ax1, ay1;
		int bx0, by0, bx1, by1;
		int cx0, cy0, cx1, cy1;
		int dx0, dy0, dx1, dy1;
		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = data[v0 + PV_PX];
		ay0 = data[v0 + PV_PY];
		bx0 = data[v1 + PV_PX];
		by0 = data[v1 + PV_PY];
		cx0 = data[v2 + PV_PX];
		cy0 = data[v2 + PV_PY];
		dx0 = data[v3 + PV_PX];
		dy0 = data[v3 + PV_PY];

		ax1 = bx0;
		ay1 = by0;
		bx1 = cx0;
		by1 = cy0;
		cx1 = dx0;
		cy1 = dy0;
		dx1 = ax0;
		dy1 = ay0;

		minX = ax0;
		maxX = ax0;

		if (bx0 < minX) {
			minX = bx0;
		} else if (bx0 > maxX) {
			maxX = bx0;
		}

		if (cx0 < minX) {
			minX = cx0;
		} else if (cx0 > maxX) {
			maxX = cx0;
		}

		if (dx0 < minX) {
			minX = dx0;
		} else if (dx0 > maxX) {
			maxX = dx0;
		}

		minY = ay0;
		maxY = ay0;

		if (by0 < minY) {
			minY = by0;
		} else if (by0 > maxY) {
			maxY = by0;
		}

		if (cy0 < minY) {
			minY = cy0;
		} else if (cy0 > maxY) {
			maxY = cy0;
		}

		if (dy0 < minY) {
			minY = dy0;
		} else if (dy0 > maxY) {
			maxY = dy0;
		}

		if (maxY <= 0 || minY >= preciseHeight) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (maxX <= 0 || minX >= preciseWidth) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (minX < 0) {
			minX = 0;
		}

		if (maxX >= preciseWidthClamp) {
			maxX = preciseWidthClamp;

			if (minX > preciseWidthClamp) {
				minX = preciseWidthClamp;
			}
		}

		if (minY < 0) {
			minY = 0;
		}

		if (maxY >= preciseHeightClamp) {
			maxY = preciseHeightClamp;

			if (minY > preciseHeightClamp) {
				minY = preciseHeightClamp;
			}
		}

		final int minPixelX = ((minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int minPixelY = ((minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelX = ((maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelY = ((maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		final int position0 = edgePosition(ax0, ay0, ax1, ay1);
		final int position1 = edgePosition(bx0, by0, bx1, by1);
		final int position2 = edgePosition(cx0, cy0, cx1, cy1);
		final int position3 = edgePosition(dx0, dy0, dx1, dy1);

		this.minPixelX = minPixelX;
		this.minPixelY = minPixelY;
		this.maxPixelX = maxPixelX;
		this.maxPixelY = maxPixelY;
		data[IDX_AX0] = ax0;
		data[IDX_AY0] = ay0;
		data[IDX_AX1] = ax1;
		data[IDX_AY1] = ay1;
		data[IDX_BX0] = bx0;
		data[IDX_BY0] = by0;
		data[IDX_BX1] = bx1;
		data[IDX_BY1] = by1;
		data[IDX_CX0] = cx0;
		data[IDX_CY0] = cy0;
		data[IDX_CX1] = cx1;
		data[IDX_CY1] = cy1;
		data[IDX_DX0] = dx0;
		data[IDX_DY0] = dy0;
		data[IDX_DX1] = dx1;
		data[IDX_DY1] = dy1;
		pos0 = position0;
		pos1 = position1;
		pos2 = position2;
		pos3 = position3;

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK
				| (((position1 - 1) & EVENT_POSITION_MASK) << 2)
				| (((position2 - 1) & EVENT_POSITION_MASK) << 4)
				| (((position3 - 1) & EVENT_POSITION_MASK) << 6);

		prepareEvents(eventKey);
		return BOUNDS_IN;
	}

	private int prepareBounds0001(int v0, int v1, int v2, int ext3) {
		final int[] data = vertexData;
		int ax0, ay0, ax1, ay1;
		int bx0, by0, bx1, by1;
		int cx0, cy0, cx1, cy1;
		int dx0, dy0, dx1, dy1;
		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = data[v0 + PV_PX];
		ay0 = data[v0 + PV_PY];
		ax1 = data[v1 + PV_PX];
		ay1 = data[v1 + PV_PY];

		bx0 = ax1;
		by0 = ay1;
		bx1 = data[v2 + PV_PX];
		by1 = data[v2 + PV_PY];

		cx0 = bx1;
		cy0 = by1;
		clipNear(v2, ext3);
		cx1 = clipX;
		cy1 = clipY;

		clipNear(v0, ext3);
		dx0 = clipX;
		dy0 = clipY;
		dx1 = ax0;
		dy1 = ay0;

		minX = ax0;
		maxX = ax0;

		// ax1 = bx0 and dx1 = ax0,  so no need to test those
		if (bx0 < minX) {
			minX = bx0;
		} else if (bx0 > maxX) {
			maxX = bx0;
		}

		if (cx0 < minX) {
			minX = cx0;
		} else if (cx0 > maxX) {
			maxX = cx0;
		}

		if (cx1 < minX) {
			minX = cx1;
		} else if (cx1 > maxX) {
			maxX = cx1;
		}

		if (dx0 < minX) {
			minX = dx0;
		} else if (dx0 > maxX) {
			maxX = dx0;
		}

		minY = ay0;
		maxY = ay0;

		if (by0 < minY) {
			minY = by0;
		} else if (by0 > maxY) {
			maxY = by0;
		}

		if (cy0 < minY) {
			minY = cy0;
		} else if (cy0 > maxY) {
			maxY = cy0;
		}

		if (cy1 < minY) {
			minY = cy1;
		} else if (cy1 > maxY) {
			maxY = cy1;
		}

		if (dy0 < minY) {
			minY = dy0;
		} else if (dy0 > maxY) {
			maxY = dy0;
		}

		if (maxY <= 0 || minY >= preciseHeight) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (maxX <= 0 || minX >= preciseWidth) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (minX < 0) {
			minX = 0;
		}

		if (maxX >= preciseWidthClamp) {
			maxX = preciseWidthClamp;

			if (minX > preciseWidthClamp) {
				minX = preciseWidthClamp;
			}
		}

		if (minY < 0) {
			minY = 0;
		}

		if (maxY >= preciseHeightClamp) {
			maxY = preciseHeightClamp;

			if (minY > preciseHeightClamp) {
				minY = preciseHeightClamp;
			}
		}

		final int minPixelX = ((minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int minPixelY = ((minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelX = ((maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelY = ((maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		final int position0 = edgePosition(ax0, ay0, ax1, ay1);
		final int position1 = edgePosition(bx0, by0, bx1, by1);
		final int position2 = edgePosition(cx0, cy0, cx1, cy1);
		final int position3 = edgePosition(dx0, dy0, dx1, dy1);

		this.minPixelX = minPixelX;
		this.minPixelY = minPixelY;
		this.maxPixelX = maxPixelX;
		this.maxPixelY = maxPixelY;
		data[IDX_AX0] = ax0;
		data[IDX_AY0] = ay0;
		data[IDX_AX1] = ax1;
		data[IDX_AY1] = ay1;
		data[IDX_BX0] = bx0;
		data[IDX_BY0] = by0;
		data[IDX_BX1] = bx1;
		data[IDX_BY1] = by1;
		data[IDX_CX0] = cx0;
		data[IDX_CY0] = cy0;
		data[IDX_CX1] = cx1;
		data[IDX_CY1] = cy1;
		data[IDX_DX0] = dx0;
		data[IDX_DY0] = dy0;
		data[IDX_DX1] = dx1;
		data[IDX_DY1] = dy1;
		pos0 = position0;
		pos1 = position1;
		pos2 = position2;
		pos3 = position3;

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK
				| (((position1 - 1) & EVENT_POSITION_MASK) << 2)
				| (((position2 - 1) & EVENT_POSITION_MASK) << 4)
				| (((position3 - 1) & EVENT_POSITION_MASK) << 6);

		prepareEvents(eventKey);
		return BOUNDS_IN;
	}

	private int prepareBounds0011(int v0, int v1, int ext2, int ext3) {
		final int[] data = vertexData;
		int ax0, ay0, ax1, ay1;
		int bx0, by0, bx1, by1;
		int cx0, cy0, cx1, cy1;
		int dx0, dy0, dx1, dy1;

		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = data[v0 + PV_PX];
		ay0 = data[v0 + PV_PY];
		ax1 = data[v1 + PV_PX];
		ay1 = data[v1 + PV_PY];

		bx0 = ax1;
		by0 = ay1;
		clipNear(v1, ext2);
		bx1 = clipX;
		by1 = clipY;

		// force line c to be a single, existing point - entire line is clipped and should not influence anything
		cx0 = ax0;
		cy0 = ay0;
		cx1 = ax0;
		cy1 = ay0;

		clipNear(v0, ext3);
		dx0 = clipX;
		dy0 = clipY;
		dx1 = ax0;
		dy1 = ay0;

		minX = ax0;
		maxX = ax0;

		if (bx0 < minX) {
			minX = bx0;
		} else if (bx0 > maxX) {
			maxX = bx0;
		}

		if (bx1 < minX) {
			minX = bx1;
		} else if (bx1 > maxX) {
			maxX = bx1;
		}

		if (dx0 < minX) {
			minX = dx0;
		} else if (dx0 > maxX) {
			maxX = dx0;
		}

		minY = ay0;
		maxY = ay0;

		if (by0 < minY) {
			minY = by0;
		} else if (by0 > maxY) {
			maxY = by0;
		}

		if (by1 < minY) {
			minY = by1;
		} else if (by1 > maxY) {
			maxY = by1;
		}

		if (dy0 < minY) {
			minY = dy0;
		} else if (dy0 > maxY) {
			maxY = dy0;
		}

		if (maxY <= 0 || minY >= preciseHeight) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (maxX <= 0 || minX >= preciseWidth) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (minX < 0) {
			minX = 0;
		}

		if (maxX >= preciseWidthClamp) {
			maxX = preciseWidthClamp;

			if (minX > preciseWidthClamp) {
				minX = preciseWidthClamp;
			}
		}

		if (minY < 0) {
			minY = 0;
		}

		if (maxY >= preciseHeightClamp) {
			maxY = preciseHeightClamp;

			if (minY > preciseHeightClamp) {
				minY = preciseHeightClamp;
			}
		}

		final int minPixelX = ((minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int minPixelY = ((minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelX = ((maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelY = ((maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		final int position0 = edgePosition(ax0, ay0, ax1, ay1);
		final int position1 = edgePosition(bx0, by0, bx1, by1);
		final int position2 = edgePosition(cx0, cy0, cx1, cy1);
		final int position3 = edgePosition(dx0, dy0, dx1, dy1);

		this.minPixelX = minPixelX;
		this.minPixelY = minPixelY;
		this.maxPixelX = maxPixelX;
		this.maxPixelY = maxPixelY;
		data[IDX_AX0] = ax0;
		data[IDX_AY0] = ay0;
		data[IDX_AX1] = ax1;
		data[IDX_AY1] = ay1;
		data[IDX_BX0] = bx0;
		data[IDX_BY0] = by0;
		data[IDX_BX1] = bx1;
		data[IDX_BY1] = by1;
		data[IDX_CX0] = cx0;
		data[IDX_CY0] = cy0;
		data[IDX_CX1] = cx1;
		data[IDX_CY1] = cy1;
		data[IDX_DX0] = dx0;
		data[IDX_DY0] = dy0;
		data[IDX_DX1] = dx1;
		data[IDX_DY1] = dy1;
		pos0 = position0;
		pos1 = position1;
		pos2 = position2;
		pos3 = position3;

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK
				| (((position1 - 1) & EVENT_POSITION_MASK) << 2)
				| (((position2 - 1) & EVENT_POSITION_MASK) << 4)
				| (((position3 - 1) & EVENT_POSITION_MASK) << 6);

		prepareEvents(eventKey);
		return BOUNDS_IN;
	}

	private int prepareBounds0111(int v0, int ext1, int ext2, int ext3) {
		final int[] data = vertexData;
		int ax0, ay0, ax1, ay1;
		int bx0, by0, bx1, by1;
		int cx0, cy0, cx1, cy1;
		int dx0, dy0, dx1, dy1;
		int minY = 0, maxY = 0, minX = 0, maxX = 0;

		ax0 = data[v0 + PV_PX];
		ay0 = data[v0 + PV_PY];
		clipNear(v0, ext1);
		ax1 = clipX;
		ay1 = clipY;

		// force lines b & c to be a single, existing point - entire line is clipped and should not influence anything
		bx0 = ax0;
		by0 = ay0;
		bx1 = ax0;
		by1 = ay0;
		cx0 = ax0;
		cy0 = ay0;
		cx1 = ax0;
		cy1 = ay0;

		clipNear(v0, ext3);
		dx0 = clipX;
		dy0 = clipY;
		dx1 = ax0;
		dy1 = ay0;

		minX = ax0;
		maxX = ax0;

		if (ax1 < minX) {
			minX = ax1;
		} else if (ax1 > maxX) {
			maxX = ax1;
		}

		if (dx0 < minX) {
			minX = dx0;
		} else if (dx0 > maxX) {
			maxX = dx0;
		}

		minY = ay0;
		maxY = ay0;

		if (ay1 < minY) {
			minY = ay1;
		} else if (ay1 > maxY) {
			maxY = ay1;
		}

		if (dy0 < minY) {
			minY = dy0;
		} else if (dy0 > maxY) {
			maxY = dy0;
		}

		if (maxY <= 0 || minY >= preciseHeight) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (maxX <= 0 || minX >= preciseWidth) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (minX < 0) {
			minX = 0;
		}

		if (maxX >= preciseWidthClamp) {
			maxX = preciseWidthClamp;

			if (minX > preciseWidthClamp) {
				minX = preciseWidthClamp;
			}
		}

		if (minY < 0) {
			minY = 0;
		}

		if (maxY >= preciseHeightClamp) {
			maxY = preciseHeightClamp;

			if (minY > preciseHeightClamp) {
				minY = preciseHeightClamp;
			}
		}

		final int minPixelX = ((minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int minPixelY = ((minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelX = ((maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);
		final int maxPixelY = ((maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS);

		final int position0 = edgePosition(ax0, ay0, ax1, ay1);
		final int position1 = edgePosition(bx0, by0, bx1, by1);
		final int position2 = edgePosition(cx0, cy0, cx1, cy1);
		final int position3 = edgePosition(dx0, dy0, dx1, dy1);

		this.minPixelX = minPixelX;
		this.minPixelY = minPixelY;
		this.maxPixelX = maxPixelX;
		this.maxPixelY = maxPixelY;
		data[IDX_AX0] = ax0;
		data[IDX_AY0] = ay0;
		data[IDX_AX1] = ax1;
		data[IDX_AY1] = ay1;
		data[IDX_BX0] = bx0;
		data[IDX_BY0] = by0;
		data[IDX_BX1] = bx1;
		data[IDX_BY1] = by1;
		data[IDX_CX0] = cx0;
		data[IDX_CY0] = cy0;
		data[IDX_CX1] = cx1;
		data[IDX_CY1] = cy1;
		data[IDX_DX0] = dx0;
		data[IDX_DY0] = dy0;
		data[IDX_DX1] = dx1;
		data[IDX_DY1] = dy1;
		pos0 = position0;
		pos1 = position1;
		pos2 = position2;
		pos3 = position3;

		final int eventKey = (position0 - 1) & EVENT_POSITION_MASK
				| (((position1 - 1) & EVENT_POSITION_MASK) << 2)
				| (((position2 - 1) & EVENT_POSITION_MASK) << 4)
				| (((position3 - 1) & EVENT_POSITION_MASK) << 6);

		prepareEvents(eventKey);
		return BOUNDS_IN;
	}
}
