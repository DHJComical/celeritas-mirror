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

import java.util.Arrays;

import static grondag.bitraster.Constants.MIN_SIZE_TILES;
import static grondag.bitraster.Constants.MAX_SIZE_TILES;
import static grondag.bitraster.Constants.PRECISE_PIXEL_CENTER;
import static grondag.bitraster.Constants.BOUNDS_IN;
import static grondag.bitraster.Constants.BOUNDS_OUTSIDE_OR_TOO_SMALL;
import static grondag.bitraster.Constants.EDGE_BOTTOM;
import static grondag.bitraster.Constants.EDGE_POINT;
import static grondag.bitraster.Constants.EDGE_TOP;
import static grondag.bitraster.Constants.EVENT_0123_FFFF;
import static grondag.bitraster.Constants.EVENT_0123_FFFL;
import static grondag.bitraster.Constants.EVENT_0123_FFFR;
import static grondag.bitraster.Constants.EVENT_0123_FFLF;
import static grondag.bitraster.Constants.EVENT_0123_FFLL;
import static grondag.bitraster.Constants.EVENT_0123_FFLR;
import static grondag.bitraster.Constants.EVENT_0123_FFRF;
import static grondag.bitraster.Constants.EVENT_0123_FFRL;
import static grondag.bitraster.Constants.EVENT_0123_FFRR;
import static grondag.bitraster.Constants.EVENT_0123_FLFF;
import static grondag.bitraster.Constants.EVENT_0123_FLFL;
import static grondag.bitraster.Constants.EVENT_0123_FLFR;
import static grondag.bitraster.Constants.EVENT_0123_FLLF;
import static grondag.bitraster.Constants.EVENT_0123_FLLL;
import static grondag.bitraster.Constants.EVENT_0123_FLLR;
import static grondag.bitraster.Constants.EVENT_0123_FLRF;
import static grondag.bitraster.Constants.EVENT_0123_FLRL;
import static grondag.bitraster.Constants.EVENT_0123_FLRR;
import static grondag.bitraster.Constants.EVENT_0123_FRFF;
import static grondag.bitraster.Constants.EVENT_0123_FRFL;
import static grondag.bitraster.Constants.EVENT_0123_FRFR;
import static grondag.bitraster.Constants.EVENT_0123_FRLF;
import static grondag.bitraster.Constants.EVENT_0123_FRLL;
import static grondag.bitraster.Constants.EVENT_0123_FRLR;
import static grondag.bitraster.Constants.EVENT_0123_FRRF;
import static grondag.bitraster.Constants.EVENT_0123_FRRL;
import static grondag.bitraster.Constants.EVENT_0123_FRRR;
import static grondag.bitraster.Constants.EVENT_0123_LFFF;
import static grondag.bitraster.Constants.EVENT_0123_LFFL;
import static grondag.bitraster.Constants.EVENT_0123_LFFR;
import static grondag.bitraster.Constants.EVENT_0123_LFLF;
import static grondag.bitraster.Constants.EVENT_0123_LFLL;
import static grondag.bitraster.Constants.EVENT_0123_LFLR;
import static grondag.bitraster.Constants.EVENT_0123_LFRF;
import static grondag.bitraster.Constants.EVENT_0123_LFRL;
import static grondag.bitraster.Constants.EVENT_0123_LFRR;
import static grondag.bitraster.Constants.EVENT_0123_LLFF;
import static grondag.bitraster.Constants.EVENT_0123_LLFL;
import static grondag.bitraster.Constants.EVENT_0123_LLFR;
import static grondag.bitraster.Constants.EVENT_0123_LLLF;
import static grondag.bitraster.Constants.EVENT_0123_LLLL;
import static grondag.bitraster.Constants.EVENT_0123_LLLR;
import static grondag.bitraster.Constants.EVENT_0123_LLRF;
import static grondag.bitraster.Constants.EVENT_0123_LLRL;
import static grondag.bitraster.Constants.EVENT_0123_LLRR;
import static grondag.bitraster.Constants.EVENT_0123_LRFF;
import static grondag.bitraster.Constants.EVENT_0123_LRFL;
import static grondag.bitraster.Constants.EVENT_0123_LRFR;
import static grondag.bitraster.Constants.EVENT_0123_LRLF;
import static grondag.bitraster.Constants.EVENT_0123_LRLL;
import static grondag.bitraster.Constants.EVENT_0123_LRLR;
import static grondag.bitraster.Constants.EVENT_0123_LRRF;
import static grondag.bitraster.Constants.EVENT_0123_LRRL;
import static grondag.bitraster.Constants.EVENT_0123_LRRR;
import static grondag.bitraster.Constants.EVENT_0123_RFFF;
import static grondag.bitraster.Constants.EVENT_0123_RFFL;
import static grondag.bitraster.Constants.EVENT_0123_RFFR;
import static grondag.bitraster.Constants.EVENT_0123_RFLF;
import static grondag.bitraster.Constants.EVENT_0123_RFLL;
import static grondag.bitraster.Constants.EVENT_0123_RFLR;
import static grondag.bitraster.Constants.EVENT_0123_RFRF;
import static grondag.bitraster.Constants.EVENT_0123_RFRL;
import static grondag.bitraster.Constants.EVENT_0123_RFRR;
import static grondag.bitraster.Constants.EVENT_0123_RLFF;
import static grondag.bitraster.Constants.EVENT_0123_RLFL;
import static grondag.bitraster.Constants.EVENT_0123_RLFR;
import static grondag.bitraster.Constants.EVENT_0123_RLLF;
import static grondag.bitraster.Constants.EVENT_0123_RLLL;
import static grondag.bitraster.Constants.EVENT_0123_RLLR;
import static grondag.bitraster.Constants.EVENT_0123_RLRF;
import static grondag.bitraster.Constants.EVENT_0123_RLRL;
import static grondag.bitraster.Constants.EVENT_0123_RLRR;
import static grondag.bitraster.Constants.EVENT_0123_RRFF;
import static grondag.bitraster.Constants.EVENT_0123_RRFL;
import static grondag.bitraster.Constants.EVENT_0123_RRFR;
import static grondag.bitraster.Constants.EVENT_0123_RRLF;
import static grondag.bitraster.Constants.EVENT_0123_RRLL;
import static grondag.bitraster.Constants.EVENT_0123_RRLR;
import static grondag.bitraster.Constants.EVENT_0123_RRRF;
import static grondag.bitraster.Constants.EVENT_0123_RRRL;
import static grondag.bitraster.Constants.EVENT_0123_RRRR;
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
import static grondag.bitraster.Constants.TEST_DILATION_PIXELS;
import static grondag.bitraster.Constants.EVENT_0_FLAT;
import static grondag.bitraster.Constants.EVENT_0_LEFT;
import static grondag.bitraster.Constants.TILE_PIXEL_INDEX_MASK;
import static grondag.bitraster.Constants.A_ZERO;
import static grondag.bitraster.Constants.A_POSITIVE;
import static grondag.bitraster.Constants.PRECISION_BITS;
import static grondag.bitraster.Constants.PV_PX;
import static grondag.bitraster.Constants.PV_PY;
import static grondag.bitraster.Constants.PV_W;
import static grondag.bitraster.Constants.PV_Z;
import static grondag.bitraster.Constants.SCANT_PRECISE_PIXEL_CENTER;
import static grondag.bitraster.Constants.TILE_AXIS_MASK;
import static grondag.bitraster.Constants.TILE_AXIS_SHIFT;
import static grondag.bitraster.Constants.VERTEX_DATA_LENGTH;

// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.
public abstract class AbstractRasterizer {
	final Matrix4L mvpMatrix = new Matrix4L();

	/** Float copy of the scene's view-projection matrix, without any per-region translation. */
	float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;

	/** Region origin relative to the camera, in blocks. Added to vertex positions before transformation. */
	float offsetX, offsetY, offsetZ;
	final int[] vertexData = new int[VERTEX_DATA_LENGTH];
	// CELERITAS: buffer geometry is per instance so each axis can follow render distance and field of view.
	// Widths need not be powers of two: rows are addressed by multiplication, which measured no slower than
	// the shifts and masks it replaced, and it lets an axis be sized to its requirement instead of double it.
	int tileWidth, tileHeight, tileCount;
	int pixelWidth, pixelHeight, lastPixelX, lastPixelY, halfPixelWidth, halfPixelHeight;
	int preciseWidth, preciseHeight, halfPreciseWidth, halfPreciseHeight, preciseWidthClamp, preciseHeightClamp;

	int[] eventData;
	long[] tiles;

	/** One bit per tile, set once the tile is fully covered, so scans can skip covered runs 64 tiles at a time. */
	long[] fullBits;

	/**
	 * One bit per cell of 8x8 tiles in each cell row, set once anything is drawn in the cell. A box whose
	 * tiles lie in untouched cells alone cannot be occluded, and {@link #touchedVersion} changes whenever a
	 * cell is first touched, so a caller can cache that verdict and revalidate it with one compare.
	 */
	int[] touchedCellRows;
	int touchedVersion;
	final EventFiller[] EVENT_FILLERS = new EventFiller[0x1000];

	/** Bounds of current triangle - pixel coordinates. */
	protected int minPixelX, minPixelY, maxPixelX, maxPixelY;

	/** Classifies each edge, holding results of {@link #edgePosition(int, int, int, int)}. */
	protected int pos0, pos1, pos2, pos3;

	protected int saveTileIndex;
	/** Control iteration in populateEvents_ methods. */
	protected int eventY0, eventLimit;

	/** Edge classification of the current quad, selecting its {@link #EVENT_FILLERS} entry. */
	protected int eventKey;

	/** Inclusive range of tile columns fully covered by the quad on every row of the current band; empty when first > last. */
	protected int bandInnerFirst, bandInnerLast;

	/** Tile columns of the widest row of the current band; tiles outside them are untouched by the polygon. */
	protected int bandOuterFirst, bandOuterLast;

	/** Per-edge intercept at y = 0 and step per pixel row, 20-bit fixed point. See {@link #prepareEvents}. */
	protected boolean edgesReady;
	protected final long[] edgeX0 = new long[6];
	protected final long[] edgeStep = new long[6];

	/** Vertex-data slot of each edge of the current polygon, in order. */

	/**
	 * True when the current polygon was set up by {@link #preparePolygonNoClip} and its events come from the
	 * generic filler rather than the quad {@link #EVENT_FILLERS} table.
	 */
	protected boolean genericEvents;

	/** Vertices of the polygon set up by {@link #preparePolygonNoClip}, whose edges are classified on demand. */
	private int[] polyVertices;
	private int polyCount;

	/** First and last pixel rows whose centres lie within the current polygon's vertical extent. */
	private int polyMinRow, polyMaxRow;

	/**
	 * True while testing: the polygon is grown by one pixel on every side so a visible strip thinner than the
	 * pixel pitch, which pixel-centre sampling would miss, still reaches a sampled pixel. Drawing never dilates,
	 * or abutting occluders would double-cover and hide the seams between them.
	 */
	protected boolean dilate;
	/**
	 * The current polygon's left and right edge chains in ascending row order: intercept at row zero and step
	 * per row in 20-bit fixed point, and the last pixel row on which the edge is the one that bounds the span.
	 */
	private int leftCount, rightCount;
	private final long[] leftX0 = new long[6], leftStep = new long[6], rightX0 = new long[6], rightStep = new long[6];
	private final int[] leftEnd = new int[6], rightEnd = new int[6];
	private final int[] chainEdges = new int[6];

	{
		EVENT_FILLERS[EVENT_0123_RRRR] = () -> {
			populateLeftEvents();
			populateRightEvents4(IDX_AX0, IDX_BX0, IDX_CX0, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LRRR] = () -> {
			populateLeftEvents(IDX_AX0);
			populateRightEvents3(IDX_BX0, IDX_CX0, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FRRR] = () -> {
			populateLeftEvents();
			populateRightEvents3(IDX_BX0, IDX_CX0, IDX_DX0);
			populateFlatEvents(pos0, IDX_AX0);
		};
		EVENT_FILLERS[EVENT_0123_RLRR] = () -> {
			populateLeftEvents(IDX_BX0);
			populateRightEvents3(IDX_AX0, IDX_CX0, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LLRR] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_BX0);
			populateRightEvents2(IDX_CX0, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FLRR] = () -> {
			populateLeftEvents(IDX_BX0);
			populateRightEvents2(IDX_CX0, IDX_DX0);
			populateFlatEvents(pos0, IDX_AX0);
		};
		EVENT_FILLERS[EVENT_0123_RFRR] = () -> {
			populateLeftEvents();
			populateRightEvents3(IDX_AX0, IDX_CX0, IDX_DX0);
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_LFRR] = () -> {
			populateLeftEvents(IDX_AX0);
			populateRightEvents2(IDX_CX0, IDX_DX0);
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_FFRR] = () -> {
			populateLeftEvents();
			populateRightEvents2(IDX_CX0, IDX_DX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
		};

		EVENT_FILLERS[EVENT_0123_RRLR] = () -> {
			populateLeftEvents(IDX_CX0);
			populateRightEvents3(IDX_AX0, IDX_BX0, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LRLR] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_CX0);
			populateRightEvents2(IDX_BX0, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FRLR] = () -> {
			populateLeftEvents(IDX_CX0);
			populateRightEvents2(IDX_BX0, IDX_DX0);
			populateFlatEvents(pos0, IDX_AX0);
		};
		EVENT_FILLERS[EVENT_0123_RLLR] = () -> {
			populateLeftEvents2(IDX_BX0, IDX_CX0);
			populateRightEvents2(IDX_AX0, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LLLR] = () -> {
			populateLeftEvents3(IDX_AX0, IDX_BX0, IDX_CX0);
			populateRightEvents(IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FLLR] = () -> {
			populateLeftEvents2(IDX_BX0, IDX_CX0);
			populateRightEvents(IDX_DX0);
			populateFlatEvents(pos0, IDX_AX0);
		};
		EVENT_FILLERS[EVENT_0123_RFLR] = () -> {
			populateLeftEvents(IDX_CX0);
			populateRightEvents2(IDX_AX0, IDX_DX0);
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_LFLR] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_CX0);
			populateRightEvents(IDX_DX0);
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_FFLR] = () -> {
			populateLeftEvents(IDX_CX0);
			populateRightEvents(IDX_DX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
		};

		EVENT_FILLERS[EVENT_0123_LRFR] = () -> {
			populateLeftEvents(IDX_AX0);
			populateRightEvents2(IDX_BX0, IDX_DX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_RRFR] = () -> {
			populateLeftEvents();
			populateRightEvents3(IDX_AX0, IDX_BX0, IDX_DX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_FRFR] = () -> {
			populateLeftEvents();
			populateRightEvents2(IDX_BX0, IDX_DX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_RLFR] = () -> {
			populateLeftEvents(IDX_BX0);
			populateRightEvents2(IDX_AX0, IDX_DX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_LLFR] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_BX0);
			populateRightEvents(IDX_DX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_FLFR] = () -> {
			populateLeftEvents(IDX_BX0);
			populateRightEvents(IDX_DX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_RFFR] = () -> {
			populateLeftEvents();
			populateRightEvents2(IDX_AX0, IDX_DX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_LFFR] = () -> {
			populateLeftEvents(IDX_AX0);
			populateRightEvents(IDX_DX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_FFFR] = () -> {
			populateLeftEvents();
			populateRightEvents(IDX_DX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
		};

		EVENT_FILLERS[EVENT_0123_RRRL] = () -> {
			populateLeftEvents(IDX_DX0);
			populateRightEvents3(IDX_AX0, IDX_BX0, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_LRRL] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_DX0);
			populateRightEvents2(IDX_BX0, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_FRRL] = () -> {
			populateLeftEvents(IDX_DX0);
			populateRightEvents2(IDX_BX0, IDX_CX0);
			populateFlatEvents(pos0, IDX_AX0);
		};
		EVENT_FILLERS[EVENT_0123_RLRL] = () -> {
			populateLeftEvents2(IDX_BX0, IDX_DX0);
			populateRightEvents2(IDX_AX0, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_LLRL] = () -> {
			populateLeftEvents3(IDX_AX0, IDX_BX0, IDX_DX0);
			populateRightEvents(IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_FLRL] = () -> {
			populateLeftEvents2(IDX_BX0, IDX_DX0);
			populateRightEvents(IDX_CX0);
			populateFlatEvents(pos0, IDX_AX0);
		};
		EVENT_FILLERS[EVENT_0123_RFRL] = () -> {
			populateLeftEvents(IDX_DX0);
			populateRightEvents2(IDX_AX0, IDX_CX0);
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_LFRL] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_DX0);
			populateRightEvents(IDX_CX0);
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_FFRL] = () -> {
			populateLeftEvents(IDX_DX0);
			populateRightEvents(IDX_CX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_RRLL] = () -> {
			populateLeftEvents2(IDX_CX0, IDX_DX0);
			populateRightEvents2(IDX_AX0, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_LRLL] = () -> {
			populateLeftEvents3(IDX_AX0, IDX_CX0, IDX_DX0);
			populateRightEvents(IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_FRLL] = () -> {
			populateLeftEvents2(IDX_CX0, IDX_DX0);
			populateRightEvents(IDX_BX0);
			populateFlatEvents(pos0, IDX_AX0);
		};
		EVENT_FILLERS[EVENT_0123_RLLL] = () -> {
			populateLeftEvents3(IDX_BX0, IDX_CX0, IDX_DX0);
			populateRightEvents(IDX_AX0);
		};
		EVENT_FILLERS[EVENT_0123_LLLL] = () -> {
			populateLeftEvents4(IDX_AX0, IDX_BX0, IDX_CX0, IDX_DX0);
			populateRightEvents();
		};
		EVENT_FILLERS[EVENT_0123_FLLL] = () -> {
			populateLeftEvents3(IDX_BX0, IDX_CX0, IDX_DX0);
			populateRightEvents();
			populateFlatEvents(pos0, IDX_AX0);
		};
		EVENT_FILLERS[EVENT_0123_RFLL] = () -> {
			populateLeftEvents2(IDX_CX0, IDX_DX0);
			populateRightEvents(IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_LFLL] = () -> {
			populateLeftEvents3(IDX_AX0, IDX_CX0, IDX_DX0);
			populateRightEvents();
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_FFLL] = () -> {
			populateLeftEvents2(IDX_CX0, IDX_DX0);
			populateRightEvents();
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
		};
		EVENT_FILLERS[EVENT_0123_LRFL] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_DX0);
			populateRightEvents(IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_RRFL] = () -> {
			populateLeftEvents(IDX_DX0);
			populateRightEvents2(IDX_AX0, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_FRFL] = () -> {
			populateLeftEvents(IDX_DX0);
			populateRightEvents(IDX_BX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_RLFL] = () -> {
			populateLeftEvents2(IDX_BX0, IDX_DX0);
			populateRightEvents(IDX_AX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_LLFL] = () -> {
			populateLeftEvents3(IDX_AX0, IDX_BX0, IDX_DX0);
			populateRightEvents();
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_FLFL] = () -> {
			populateLeftEvents2(IDX_BX0, IDX_DX0);
			populateRightEvents();
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_RFFL] = () -> {
			populateLeftEvents(IDX_DX0);
			populateRightEvents(IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_LFFL] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_DX0);
			populateRightEvents();
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_FFFL] = () -> {
			populateLeftEvents(IDX_DX0);
			populateRightEvents();
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
		};
		EVENT_FILLERS[EVENT_0123_RRRF] = () -> {
			populateLeftEvents();
			populateRightEvents3(IDX_AX0, IDX_BX0, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LRRF] = () -> {
			populateLeftEvents(IDX_AX0);
			populateRightEvents2(IDX_BX0, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FRRF] = () -> {
			populateLeftEvents();
			populateRightEvents2(IDX_BX0, IDX_CX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_RLRF] = () -> {
			populateLeftEvents(IDX_BX0);
			populateRightEvents2(IDX_AX0, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LLRF] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_BX0);
			populateRightEvents(IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FLRF] = () -> {
			populateLeftEvents(IDX_BX0);
			populateRightEvents(IDX_CX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_RFRF] = () -> {
			populateLeftEvents();
			populateRightEvents2(IDX_AX0, IDX_CX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LFRF] = () -> {
			populateLeftEvents(IDX_AX0);
			populateRightEvents(IDX_CX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FFRF] = () -> {
			populateLeftEvents();
			populateRightEvents(IDX_CX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_RRLF] = () -> {
			populateLeftEvents(IDX_CX0);
			populateRightEvents2(IDX_AX0, IDX_BX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LRLF] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_CX0);
			populateRightEvents(IDX_BX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FRLF] = () -> {
			populateLeftEvents(IDX_CX0);
			populateRightEvents(IDX_BX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_RLLF] = () -> {
			populateLeftEvents2(IDX_BX0, IDX_CX0);
			populateRightEvents(IDX_AX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LLLF] = () -> {
			populateLeftEvents3(IDX_AX0, IDX_BX0, IDX_CX0);
			populateRightEvents();
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FLLF] = () -> {
			populateLeftEvents2(IDX_BX0, IDX_CX0);
			populateRightEvents();
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_RFLF] = () -> {
			populateLeftEvents(IDX_CX0);
			populateRightEvents(IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LFLF] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_CX0);
			populateRightEvents();
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FFLF] = () -> {
			populateLeftEvents(IDX_CX0);
			populateRightEvents();
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LRFF] = () -> {
			populateLeftEvents(IDX_AX0);
			populateRightEvents(IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_RRFF] = () -> {
			populateLeftEvents();
			populateRightEvents2(IDX_AX0, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FRFF] = () -> {
			populateLeftEvents();
			populateRightEvents(IDX_BX0);
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos2, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_RLFF] = () -> {
			populateLeftEvents(IDX_BX0);
			populateRightEvents(IDX_AX0);
			populateFlatEvents(pos2, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LLFF] = () -> {
			populateLeftEvents2(IDX_AX0, IDX_BX0);
			populateRightEvents();
			populateFlatEvents(pos2, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FLFF] = () -> {
			populateLeftEvents(IDX_BX0);
			populateRightEvents();
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos2, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_RFFF] = () -> {
			populateLeftEvents();
			populateRightEvents(IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_LFFF] = () -> {
			populateLeftEvents(IDX_AX0);
			populateRightEvents();
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
		EVENT_FILLERS[EVENT_0123_FFFF] = () -> {
			// fill it
			populateLeftEvents();
			populateRightEvents();
			populateFlatEvents(pos0, IDX_AX0);
			populateFlatEvents(pos1, IDX_BX0);
			populateFlatEvents(pos2, IDX_CX0);
			populateFlatEvents(pos3, IDX_DX0);
		};
	}

	AbstractRasterizer(int tileWidth, int tileHeight) {
		resize(tileWidth, tileHeight);
	}

	/** {@return the buffer width in tiles; pixels are eight times that} */
	public final int tileWidth() {
		return tileWidth;
	}

	/** {@return the buffer height in tiles; pixels are eight times that} */
	public final int tileHeight() {
		return tileHeight;
	}

	/**
	 * Sets the buffer to {@code 8 * tileWidth} by {@code 8 * tileHeight} pixels, reallocating and clearing it
	 * if the size changed. Safe between frames only: the current polygon and scene contents are discarded.
	 */
	public final void resize(int tileWidth, int tileHeight) {
		if (tileWidth < MIN_SIZE_TILES || tileWidth > MAX_SIZE_TILES || tileHeight < MIN_SIZE_TILES || tileHeight > MAX_SIZE_TILES) {
			throw new IllegalArgumentException("unsupported buffer size in tiles " + tileWidth + "x" + tileHeight);
		}

		if (this.tileWidth == tileWidth && this.tileHeight == tileHeight && tiles != null) {
			return;
		}

		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		tileCount = tileWidth * tileHeight;
		pixelWidth = tileWidth << TILE_AXIS_SHIFT;
		pixelHeight = tileHeight << TILE_AXIS_SHIFT;
		lastPixelX = pixelWidth - 1;
		lastPixelY = pixelHeight - 1;
		halfPixelWidth = pixelWidth >> 1;
		halfPixelHeight = pixelHeight >> 1;
		preciseWidth = pixelWidth << PRECISION_BITS;
		preciseHeight = pixelHeight << PRECISION_BITS;
		halfPreciseWidth = preciseWidth >> 1;
		halfPreciseHeight = preciseHeight >> 1;
		// clamp to this to ensure value + half pixel rounds down to last pixel
		preciseWidthClamp = preciseWidth - PRECISE_PIXEL_CENTER;
		preciseHeightClamp = preciseHeight - PRECISE_PIXEL_CENTER;

		eventData = new int[pixelHeight * 2];
		tiles = new long[tileCount];
		fullBits = new long[(tileCount + 63) >> 6];
		touchedCellRows = new int[(tileHeight + CELL_TILE_MASK) >> CELL_TILE_SHIFT];
	}

	/** Marks every pixel clear. */
	final void clear() {
		Arrays.fill(tiles, 0L);
		Arrays.fill(fullBits, 0L);
		Arrays.fill(touchedCellRows, 0);
		touchedVersion++;
	}

	/** Cells of the touched map are this many tiles square; 256 tiles across fit one int of cell bits. */
	static final int CELL_TILE_SHIFT = 3;
	static final int CELL_TILE_MASK = (1 << CELL_TILE_SHIFT) - 1;

	/** Bits of the cells spanning tile columns {@code first..last} inclusive. */
	private static int cellMask(int first, int last) {
		return (-1 >>> (31 - (last >> CELL_TILE_SHIFT))) & (-1 << (first >> CELL_TILE_SHIFT));
	}

	/** Records that the tiles {@code first..last} of tile row {@code tileY} may have been drawn into. */
	private void markTouched(int tileY, int first, int last) {
		final int[] rows = touchedCellRows;
		final int cellY = tileY >> CELL_TILE_SHIFT;
		final int mask = cellMask(first, last);
		final int row = rows[cellY];

		if ((row & mask) != mask) {
			rows[cellY] = row | mask;
			touchedVersion++;
		}
	}

	/** Changes whenever a cell of the touched map is first drawn into; see {@link #isTileBoxUntouched}. */
	public final int touchedVersion() {
		return touchedVersion;
	}

	/** True if nothing has been drawn in any cell overlapping the given inclusive tile bounds. */
	public final boolean isTileBoxUntouched(int packedTileBounds) {
		final int[] rows = touchedCellRows;
		final int mask = cellMask(packedTileBounds & 0xFF, (packedTileBounds >>> 16) & 0xFF);
		final int lastCellY = ((packedTileBounds >>> 24) & 0xFF) >> CELL_TILE_SHIFT;

		for (int cellY = ((packedTileBounds >>> 8) & 0xFF) >> CELL_TILE_SHIFT; cellY <= lastCellY; cellY++) {
			if ((rows[cellY] & mask) != 0) {
				return false;
			}
		}

		return true;
	}

	/** Result of {@link #boxTileBounds} for a box that reaches the near plane or lies wholly off screen. */
	public static final long NO_TILE_BOUNDS = -1L;

	/** Set in a {@link #boxTileBounds} result when every point of the box projects on screen. */
	public static final long TILE_BOUNDS_ON_SCREEN = 1L << 32;

	/**
	 * Inclusive tile bounds of the screen rectangle around a camera-relative box, packed as x0, y0, x1, y1 in
	 * successive bytes of the low word, grown by a pixel and clamped to the screen. This rectangle contains the
	 * projection of every point in the box, so every polygon a box inside it is tested with. The result carries
	 * {@link #TILE_BOUNDS_ON_SCREEN} when no clamping was needed, so that a box inside it is on screen too.
	 * {@link #NO_TILE_BOUNDS} if any corner is behind the near plane or the whole box is off screen.
	 */
	final long boxTileBounds(float fx, float fy, float fz, float ex, float ey, float ez) {
		final float bx = m00 * fx + m01 * fy + m02 * fz + m03;
		final float by = m10 * fx + m11 * fy + m12 * fz + m13;
		final float bz = m20 * fx + m21 * fy + m22 * fz + m23;
		final float bw = m30 * fx + m31 * fy + m32 * fz + m33;

		final float dxx = m00 * ex, dxy = m10 * ex, dxz = m20 * ex, dxw = m30 * ex;
		final float dyx = m01 * ey, dyy = m11 * ey, dyz = m21 * ey, dyw = m31 * ey;
		final float dzx = m02 * ez, dzy = m12 * ez, dzz = m22 * ez, dzw = m32 * ez;

		float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;

		for (int corner = 0; corner < 8; corner++) {
			float x = bx, y = by, z = bz, w = bw;

			if ((corner & 4) != 0) { x += dxx; y += dxy; z += dxz; w += dxw; }
			if ((corner & 2) != 0) { x += dyx; y += dyy; z += dyz; w += dyw; }
			if ((corner & 1) != 0) { x += dzx; y += dzy; z += dzz; w += dzw; }

			// same acceptance as needsNearClip
			if (!(w > 0 && z > 0 && z <= w)) {
				return NO_TILE_BOUNDS;
			}

			final float iw = 1f / w;
			final float sx = x * iw;
			final float sy = y * iw;
			if (sx < minX) minX = sx;
			if (sx > maxX) maxX = sx;
			if (sy < minY) minY = sy;
			if (sy > maxY) maxY = sy;
		}

		// same acceptance as isProjectedPointClear, so that every point of the box passes it
		final long onScreen = (minX >= -1f && maxX < 1f && minY >= -1f && maxY < 1f) ? TILE_BOUNDS_ON_SCREEN : 0L;

		// to pixels, with a pixel of margin over the rounding of the polygon path and the test dilation
		final float px0 = (minX + 1f) * halfPixelWidth - 1f;
		final float px1 = (maxX + 1f) * halfPixelWidth + 1f;
		final float py0 = (minY + 1f) * halfPixelHeight - 1f;
		final float py1 = (maxY + 1f) * halfPixelHeight + 1f;

		if (!(px1 >= 0f && py1 >= 0f && px0 <= lastPixelX && py0 <= lastPixelY)) {
			return NO_TILE_BOUNDS;
		}

		final int tx0 = Math.max(0, (int) px0) >> TILE_AXIS_SHIFT;
		final int ty0 = Math.max(0, (int) py0) >> TILE_AXIS_SHIFT;
		final int tx1 = Math.min(lastPixelX, (int) px1) >> TILE_AXIS_SHIFT;
		final int ty1 = Math.min(lastPixelY, (int) py1) >> TILE_AXIS_SHIFT;

		return onScreen | (tx0 | (ty0 << 8) | (tx1 << 16) | (ty1 << 24));
	}

	final int tileIndexFromPixelXY(int x, int y) {
		return (x >>> TILE_AXIS_SHIFT) + ((y >>> TILE_AXIS_SHIFT) * tileWidth);
	}

	/** Loads the scene matrix; regions are then positioned with {@link #setRegionOffset}. */
	final void setMatrix(Matrix4L m) {
		mvpMatrix.copyFrom(m);
		m00 = m.a00f(); m01 = m.a01f(); m02 = m.a02f(); m03 = m.a03f();
		m10 = m.a10f(); m11 = m.a11f(); m12 = m.a12f(); m13 = m.a13f();
		m20 = m.a20f(); m21 = m.a21f(); m22 = m.a22f(); m23 = m.a23f();
		m30 = m.a30f(); m31 = m.a31f(); m32 = m.a32f(); m33 = m.a33f();

		final float[] step = sectionStep;
		step[0] = 16f * m00; step[1] = 16f * m10; step[2] = 16f * m30;
		step[3] = 16f * m01; step[4] = 16f * m11; step[5] = 16f * m31;
		step[6] = 16f * m02; step[7] = 16f * m12; step[8] = 16f * m32;
	}

	/**
	 * Clip-space change in x, y and w per section along each world axis, as three triples. Clip space is
	 * affine in world space, so a section centre is a base point plus whole steps, and whether it lies on
	 * screen is a comparison of x and y against w with no divide.
	 */
	final float[] sectionStep = new float[9];

	/** Writes the clip-space x, y and w of a camera-relative point. */
	final void clipPoint(float fx, float fy, float fz, float[] out, int offset) {
		out[offset] = m00 * fx + m01 * fy + m02 * fz + m03;
		out[offset + 1] = m10 * fx + m11 * fy + m12 * fz + m13;
		out[offset + 2] = m30 * fx + m31 * fy + m32 * fz + m33;
	}

	/**
	 * True if the section centre {@code i, j, k} sections from the given clip-space base point is on screen
	 * by the same acceptance as {@link #isProjectedPointClear}. The base must be in front of the near plane.
	 */
	final boolean isSectionCenterOnScreen(float[] base, int offset, int i, int j, int k) {
		final float[] step = sectionStep;
		final float x = base[offset] + i * step[0] + j * step[3] + k * step[6];
		final float y = base[offset + 1] + i * step[1] + j * step[4] + k * step[7];
		final float w = base[offset + 2] + i * step[2] + j * step[5] + k * step[8];
		return w > 0 && x >= -w && x < w && y >= -w && y < w;
	}

	/** @param x region origin relative to the camera, in {@link Constants#CAMERA_PRECISION_BITS} fixed point */
	final void setRegionOffset(int x, int y, int z) {
		final float scale = 1f / Constants.CAMERA_PRECISION_UNITY;
		offsetX = x * scale;
		offsetY = y * scale;
		offsetZ = z * scale;
	}

	final void copyFrom(AbstractRasterizer source) {
		resize(source.tileWidth, source.tileHeight);
		setMatrix(source.mvpMatrix);
		offsetX = source.offsetX;
		offsetY = source.offsetY;
		offsetZ = source.offsetZ;
		System.arraycopy(source.vertexData, 0, vertexData, 0, VERTEX_DATA_LENGTH);
		System.arraycopy(source.eventData, 0, eventData, 0, eventData.length);
		System.arraycopy(source.tiles, 0, tiles, 0, tileCount);
		System.arraycopy(source.fullBits, 0, fullBits, 0, fullBits.length);
	}

	// CELERITAS: work counters for RasterStatsReport. STATS is a constant so the JIT drops the increments when off.
	public static final boolean STATS = Boolean.getBoolean("bitraster.stats");
	public static long STAT_D_NOOP, STAT_D_RANGE0, STAT_D_RANGE1, STAT_D_RANGE2, STAT_D_RANGE3;
	public static long STAT_T_EMPTY, STAT_T_INNER, STAT_T_COVER, STAT_T_HIDDEN, STAT_T_VERTEX, STAT_T_BOX_HIDDEN, STAT_D_INNER, STAT_D_COVER;
	public static long STAT_DRAW_QUADS, STAT_TEST_QUADS, STAT_TEST_TILES, STAT_EVENT_ROWS, STAT_DRAW_CALLS, STAT_TEST_CALLS;

	final void drawQuad(int v0, int v1, int v2, int v3) {
		if (STATS) STAT_DRAW_CALLS++;
		dilate = false;
		if (prepareBounds(v0, v1, v2, v3) == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return;
		}
		if (STATS) STAT_DRAW_QUADS++;

		// Don't draw single points
		if (minPixelX == maxPixelX && minPixelY == maxPixelY) {
			return;
		}

		drawQuad();
	}

	final boolean isQuadPartiallyClear(int v0, int v1, int v2, int v3) {
		if (STATS) STAT_TEST_CALLS++;
		dilate = true;
		if (prepareBounds(v0, v1, v2, v3) == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return false;
		}
		if (STATS) STAT_TEST_QUADS++;

		dilateBounds();
		return isQuadPartiallyClear();
	}

	/** Grows the pixel bounds of a test polygon by one on every side, within the screen. */
	private void dilateBounds() {
		minPixelX = Math.max(0, minPixelX - TEST_DILATION_PIXELS);
		minPixelY = Math.max(0, minPixelY - TEST_DILATION_PIXELS);
		maxPixelX = Math.min(lastPixelX, maxPixelX + TEST_DILATION_PIXELS);
		maxPixelY = Math.min(lastPixelY, maxPixelY + TEST_DILATION_PIXELS);
	}

	final boolean isQuadPartiallyClear() {
		final int minTileX = minPixelX >> TILE_AXIS_SHIFT;
		final int maxTileX = maxPixelX >> TILE_AXIS_SHIFT;
		final int minTileY = minPixelY >> TILE_AXIS_SHIFT;
		final int maxTileY = maxPixelY >> TILE_AXIS_SHIFT;
		final long[] tiles = this.tiles;
		final long[] fullBits = this.fullBits;

		for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
			final int rowBase = tileY * tileWidth;
			final int last = rowBase + maxTileX;
			boolean populated = false;
			int innerFirst = 0, innerLast = -1, outerFirst = 0, outerLast = -1;

			for (int tileIndex = rowBase + minTileX; tileIndex <= last; tileIndex++) {
				// jump to the next tile that is not already fully covered
				final long open = ~fullBits[tileIndex >> 6] >>> (tileIndex & 63);

				if (open == 0) {
					tileIndex |= 63;
					continue;
				}

				tileIndex += Long.numberOfTrailingZeros(open);

				if (tileIndex > last) {
					break;
				}

				if (STATS) STAT_TEST_TILES++;
				final long word = tiles[tileIndex];

				// nothing drawn here yet; the quad's bounds reach this tile, so treat it as clear without
				// rasterizing. slightly optimistic at silhouette corners, which only ever under-culls.
				if (word == 0L) {
					if (STATS) STAT_T_EMPTY++;
					return true;
				}

				if (!populated) {
					populateBand(tileY);
					populated = true;
					innerFirst = rowBase + bandInnerFirst;
					innerLast = rowBase + bandInnerLast;
					outerFirst = rowBase + bandOuterFirst;
					outerLast = rowBase + bandOuterLast;
				}

				// interior tiles are fully covered by the quad, and this one is not fully drawn
				if (tileIndex >= innerFirst && tileIndex <= innerLast) {
					if (STATS) STAT_T_INNER++;
					return true;
				}

				// outside the band's widest row the polygon covers nothing
				if (tileIndex < outerFirst) {
					tileIndex = outerFirst - 1;
					continue;
				} else if (tileIndex > outerLast) {
					break;
				}

				if ((~word & computeTileCoverage(tileIndex - rowBase)) != 0) {
					if (STATS) STAT_T_COVER++;
					return true;
				}
			}
		}

		if (STATS) STAT_T_HIDDEN++;
		return false;
	}

	final boolean isQuadPartiallyOccluded(int v0, int v1, int v2, int v3) {
		dilate = false;
		if (prepareBounds(v0, v1, v2, v3) == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return false;
		}

		final int px = minPixelX;
		final int py = minPixelY;

		if (px == maxPixelX && py == maxPixelY) {
			return px >= 0 && py >= 0 && px < pixelWidth && py < pixelHeight && isPixelSet(px, py);
		} else {
			return isQuadPartiallyOccluded();
		}
	}

	final boolean isQuadPartiallyOccluded() {
		final int minTileX = minPixelX >> TILE_AXIS_SHIFT;
		final int maxTileX = maxPixelX >> TILE_AXIS_SHIFT;
		final int minTileY = minPixelY >> TILE_AXIS_SHIFT;
		final int maxTileY = maxPixelY >> TILE_AXIS_SHIFT;
		final long[] tiles = this.tiles;

		for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
			final int rowBase = tileY * tileWidth;
			boolean populated = false;

			for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
				final int tileIndex = rowBase + tileX;
				final long word = tiles[tileIndex];

				if (word == 0L) {
					continue;
				}

				if (!populated) {
					populateBand(tileY);
					populated = true;
				}

				if ((word & computeTileCoverage(tileIndex - rowBase)) != 0) {
					return true;
				}
			}
		}

		return false;
	}

	final void drawQuad() {
		final int minTileX = minPixelX >> TILE_AXIS_SHIFT;
		final int maxTileX = maxPixelX >> TILE_AXIS_SHIFT;
		final int minTileY = minPixelY >> TILE_AXIS_SHIFT;
		final int maxTileY = maxPixelY >> TILE_AXIS_SHIFT;
		final long[] tiles = this.tiles;
		final long[] fullBits = this.fullBits;
		for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
			final int rowBase = tileY * tileWidth;
			final int last = rowBase + maxTileX;
			boolean populated = false;
			int innerFirst = 0, innerLast = -1, outerFirst = 0, outerLast = -1;

			for (int tileIndex = rowBase + minTileX; tileIndex <= last; tileIndex++) {
				// jump to the next tile that is not already fully covered
				final int wordIndex = tileIndex >> 6;
				final long open = ~fullBits[wordIndex] >>> (tileIndex & 63);

				if (open == 0) {
					tileIndex |= 63;
					continue;
				}

				tileIndex += Long.numberOfTrailingZeros(open);

				if (tileIndex > last) {
					break;
				}

				if (!populated) {
					populateBand(tileY);
					populated = true;
					innerFirst = rowBase + bandInnerFirst;
					innerLast = Math.min(last, rowBase + bandInnerLast);
					outerFirst = rowBase + bandOuterFirst;
					outerLast = Math.min(last, rowBase + bandOuterLast);

					final int firstDrawn = Math.max(tileIndex, outerFirst) - rowBase;
					final int lastDrawn = outerLast - rowBase;

					if (firstDrawn <= lastDrawn) {
						markTouched(tileY, firstDrawn, lastDrawn);
					}
				}

				// outside the band's widest row the polygon covers nothing
				if (tileIndex < outerFirst) {
					tileIndex = outerFirst - 1;
					continue;
				} else if (tileIndex > outerLast) {
					break;
				}

				if (tileIndex >= innerFirst && tileIndex <= innerLast) {
					// every tile through the end of the interior run is covered on all rows; filling the ones
					// already full again is harmless and cheaper than testing them one by one
					if (STATS) STAT_D_INNER += innerLast - tileIndex + 1;
					Arrays.fill(tiles, tileIndex, innerLast + 1, -1L);
					markFull(fullBits, tileIndex, innerLast);
					tileIndex = innerLast;
					continue;
				}

				if (STATS) STAT_D_COVER++;
				final long word = tiles[tileIndex] | computeTileCoverage(tileIndex - rowBase);
				tiles[tileIndex] = word;

				if (word == -1L) {
					fullBits[wordIndex] |= 1L << (tileIndex & 63);
				}
			}
		}

		if (STATS && !edgesReady) STAT_D_NOOP++;
	}

	/** Sets the full bits of tiles {@code from} through {@code to} inclusive. */
	private static void markFull(long[] fullBits, int from, int to) {
		final int firstWord = from >> 6;
		final int lastWord = to >> 6;
		final long head = -1L << (from & 63);
		final long tail = -1L >>> (63 - (to & 63));

		if (firstWord == lastWord) {
			fullBits[firstWord] |= head & tail;
		} else {
			fullBits[firstWord] |= head;

			for (int w = firstWord + 1; w < lastWord; w++) {
				fullBits[w] = -1L;
			}

			fullBits[lastWord] |= tail;
		}
	}

	abstract int prepareBounds(int v0, int v1, int v2, int v3);

	/**
	 * Marks the rows beyond a flat edge as empty. Under {@link #dilate} the first such row instead takes the
	 * edge's span, since the sloped neighbours extrapolated past a flat edge would not cover it.
	 */
	private void populateFlatEvents(int position, int slot) {
		final int[] eventData = this.eventData;
		final int[] data = this.vertexData;
		final int y0In = data[slot + 1];
		final int bandFirst = eventY0;
		final int bandLast = eventLimit >> 1;
		final int grow = dilate ? TEST_DILATION_PIXELS : 0;

		if (position == EDGE_TOP) {
			final int edgeRow = (y0In + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS;
			final int py = edgeRow + 1 + grow;

			if (py > lastPixelY) return;

			final int start = (py < bandFirst ? bandFirst : py) << 1;
			final int limit = bandLast << 1;

			assert limit < eventData.length;

			for (int y = start; y <= limit; ) {
				eventData[y++] = pixelWidth;
				eventData[y++] = -1;
			}

			for (int i = 1; i <= grow; i++) {
				spanRow(edgeRow + i, slot, bandFirst, bandLast);
			}
		} else if (position == EDGE_BOTTOM) {
			final int edgeRow = y0In >> PRECISION_BITS;
			final int py = edgeRow - grow;

			if (py <= 0) return;

			final int start = bandFirst << 1;
			final int end = py > bandLast + 1 ? bandLast + 1 : py;
			final int limit = end > lastPixelY ? (lastPixelY << 1) : (end << 1);

			assert limit < eventData.length;

			for (int y = start; y < limit; ) {
				eventData[y++] = pixelWidth;
				eventData[y++] = -1;
			}

			for (int i = 1; i <= grow; i++) {
				spanRow(edgeRow - i, slot, bandFirst, bandLast);
			}
		} else {
			assert position == EDGE_POINT;
		}
	}

	/** Gives one row the horizontal extent of a flat edge, if the row lies in the current band. */
	private void spanRow(int row, int slot, int bandFirst, int bandLast) {
		if (row < bandFirst || row > bandLast || row < 0 || row > lastPixelY) {
			return;
		}

		final int[] data = this.vertexData;
		final int xa = data[slot];
		final int xb = data[slot + 2];
		final int[] eventData = this.eventData;
		eventData[row << 1] = ((xa < xb ? xa : xb) + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS;
		eventData[(row << 1) + 1] = ((xa < xb ? xb : xa) + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS;
	}

	/**
	 * Puts left edge at screen boundary.
	 */
	private void populateLeftEvents() {
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		final int limit = eventLimit;

		for (int y = y0 << 1; y <= limit; y += 2) {
			eventData[y] = 0;
		}
	}

	private void populateLeftEvents(int a) {
		final long[] edgeX0 = this.edgeX0;
		final long[] edgeStep = this.edgeStep;
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		final int limit = eventLimit;
		final int ea = (a - IDX_AX0) >> 2;
		final long aStep = edgeStep[ea];
		long ax = edgeX0[ea] + aStep * y0;

		for (int y = (y0 << 1); y <= limit; y += 2) {
			eventData[y] = (int) (ax >> 20);
			ax += aStep;
		}
	}

	private void populateRightEvents() {
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		// difference from left: is high index in pairs
		final int limit = eventLimit + 1;

		// difference from left: is high index in pairs
		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			eventData[y] = pixelWidth;
		}
	}

	private void populateRightEvents(int a) {
		final long[] edgeX0 = this.edgeX0;
		final long[] edgeStep = this.edgeStep;
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		// right events are the high index in each pair
		final int limit = eventLimit + 1;
		final int ea = (a - IDX_AX0) >> 2;
		final long aStep = edgeStep[ea];
		long ax = edgeX0[ea] + aStep * y0;

		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			eventData[y] = (int) (ax >> 20);
			ax += aStep;
		}
	}

	private void populateLeftEvents2(int a, int b) {
		final long[] edgeX0 = this.edgeX0;
		final long[] edgeStep = this.edgeStep;
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		final int limit = eventLimit;
		final int ea = (a - IDX_AX0) >> 2;
		final int eb = (b - IDX_AX0) >> 2;
		final long aStep = edgeStep[ea];
		long ax = edgeX0[ea] + aStep * y0;
		final long bStep = edgeStep[eb];
		long bx = edgeX0[eb] + bStep * y0;

		for (int y = (y0 << 1); y <= limit; y += 2) {
			eventData[y] = (int) ((ax > bx ? ax : bx) >> 20);
			ax += aStep;
			bx += bStep;
		}
	}

	private void populateLeftEvents3(int a, int b, int c) {
		final long[] edgeX0 = this.edgeX0;
		final long[] edgeStep = this.edgeStep;
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		final int limit = eventLimit;
		final int ea = (a - IDX_AX0) >> 2;
		final int eb = (b - IDX_AX0) >> 2;
		final int ec = (c - IDX_AX0) >> 2;
		final long aStep = edgeStep[ea];
		long ax = edgeX0[ea] + aStep * y0;
		final long bStep = edgeStep[eb];
		long bx = edgeX0[eb] + bStep * y0;
		final long cStep = edgeStep[ec];
		long cx = edgeX0[ec] + cStep * y0;

		for (int y = (y0 << 1); y <= limit; y += 2) {
			final long i = ax > bx ? ax : bx;
			eventData[y] = (int) ((cx > i ? cx : i) >> 20);
			ax += aStep;
			bx += bStep;
			cx += cStep;
		}
	}

	private void populateLeftEvents4(int a, int b, int c, int d) {
		final long[] edgeX0 = this.edgeX0;
		final long[] edgeStep = this.edgeStep;
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		final int limit = eventLimit;
		final int ea = (a - IDX_AX0) >> 2;
		final int eb = (b - IDX_AX0) >> 2;
		final int ec = (c - IDX_AX0) >> 2;
		final int ed = (d - IDX_AX0) >> 2;
		final long aStep = edgeStep[ea];
		long ax = edgeX0[ea] + aStep * y0;
		final long bStep = edgeStep[eb];
		long bx = edgeX0[eb] + bStep * y0;
		final long cStep = edgeStep[ec];
		long cx = edgeX0[ec] + cStep * y0;
		final long dStep = edgeStep[ed];
		long dx = edgeX0[ed] + dStep * y0;

		for (int y = (y0 << 1); y <= limit; y += 2) {
			final long i = ax > bx ? ax : bx;
			final long j = cx > dx ? cx : dx;
			eventData[y] = (int) ((i > j ? i : j) >> 20);
			ax += aStep;
			bx += bStep;
			cx += cStep;
			dx += dStep;
		}
	}

	private void populateRightEvents2(int a, int b) {
		final long[] edgeX0 = this.edgeX0;
		final long[] edgeStep = this.edgeStep;
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		// right events are the high index in each pair
		final int limit = eventLimit + 1;
		final int ea = (a - IDX_AX0) >> 2;
		final int eb = (b - IDX_AX0) >> 2;
		final long aStep = edgeStep[ea];
		long ax = edgeX0[ea] + aStep * y0;
		final long bStep = edgeStep[eb];
		long bx = edgeX0[eb] + bStep * y0;

		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			eventData[y] = (int) ((ax < bx ? ax : bx) >> 20);
			ax += aStep;
			bx += bStep;
		}
	}

	private void populateRightEvents3(int a, int b, int c) {
		final long[] edgeX0 = this.edgeX0;
		final long[] edgeStep = this.edgeStep;
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		// right events are the high index in each pair
		final int limit = eventLimit + 1;
		final int ea = (a - IDX_AX0) >> 2;
		final int eb = (b - IDX_AX0) >> 2;
		final int ec = (c - IDX_AX0) >> 2;
		final long aStep = edgeStep[ea];
		long ax = edgeX0[ea] + aStep * y0;
		final long bStep = edgeStep[eb];
		long bx = edgeX0[eb] + bStep * y0;
		final long cStep = edgeStep[ec];
		long cx = edgeX0[ec] + cStep * y0;

		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			final long i = ax < bx ? ax : bx;
			eventData[y] = (int) ((cx < i ? cx : i) >> 20);
			ax += aStep;
			bx += bStep;
			cx += cStep;
		}
	}

	private void populateRightEvents4(int a, int b, int c, int d) {
		final long[] edgeX0 = this.edgeX0;
		final long[] edgeStep = this.edgeStep;
		final int[] eventData = this.eventData;
		final int y0 = eventY0;
		// right events are the high index in each pair
		final int limit = eventLimit + 1;
		final int ea = (a - IDX_AX0) >> 2;
		final int eb = (b - IDX_AX0) >> 2;
		final int ec = (c - IDX_AX0) >> 2;
		final int ed = (d - IDX_AX0) >> 2;
		final long aStep = edgeStep[ea];
		long ax = edgeX0[ea] + aStep * y0;
		final long bStep = edgeStep[eb];
		long bx = edgeX0[eb] + bStep * y0;
		final long cStep = edgeStep[ec];
		long cx = edgeX0[ec] + cStep * y0;
		final long dStep = edgeStep[ed];
		long dx = edgeX0[ed] + dStep * y0;

		for (int y = (y0 << 1) + 1; y <= limit; y += 2) {
			final long i = ax < bx ? ax : bx;
			final long j = cx < dx ? cx : dx;
			eventData[y] = (int) ((i < j ? i : j) >> 20);
			ax += aStep;
			bx += bStep;
			cx += cStep;
			dx += dStep;
		}
	}

	abstract void setupVertex(int baseIndex, int x, int y, int z);

	/**
	 * True if the pixel under the given region-relative point is known to be clear. False when the point is
	 * covered, off screen, or too close to the camera to project, so callers must fall back to a full test.
	 */
	abstract boolean isPointClear(float x, float y, float z);

	/**
	 * Projects the listed corners of a region-relative box. Corner slots are laid out as V<x><y><z>, so the
	 * slot index selects which extent each axis takes. Returns false if any listed corner needs near clipping.
	 */
	abstract boolean setupBoxVertices(int[] corners, int x0, int y0, int z0, int x1, int y1, int z1);

	/** Scratch clip-space coordinates of a box's eight corners, indexed by corner in slot order. */
	protected final float[] cornerX = new float[8], cornerY = new float[8], cornerZ = new float[8], cornerW = new float[8];

	/** Fills the corner scratch arrays from one transformed corner and the three axis deltas. */
	final void transformBoxCorners(int x0, int y0, int z0, int x1, int y1, int z1) {
		final float fx = x0 + offsetX;
		final float fy = y0 + offsetY;
		final float fz = z0 + offsetZ;
		final float ex = x1 - x0;
		final float ey = y1 - y0;
		final float ez = z1 - z0;

		final float[] cx = cornerX, cy = cornerY, cz = cornerZ, cw = cornerW;

		// V000
		final float bx = m00 * fx + m01 * fy + m02 * fz + m03;
		final float by = m10 * fx + m11 * fy + m12 * fz + m13;
		final float bz = m20 * fx + m21 * fy + m22 * fz + m23;
		final float bw = m30 * fx + m31 * fy + m32 * fz + m33;

		final float dxx = m00 * ex, dxy = m10 * ex, dxz = m20 * ex, dxw = m30 * ex;
		final float dyx = m01 * ey, dyy = m11 * ey, dyz = m21 * ey, dyw = m31 * ey;
		final float dzx = m02 * ez, dzy = m12 * ez, dzz = m22 * ez, dzw = m32 * ez;

		cx[0] = bx; cy[0] = by; cz[0] = bz; cw[0] = bw;
		cx[1] = bx + dzx; cy[1] = by + dzy; cz[1] = bz + dzz; cw[1] = bw + dzw;
		cx[2] = bx + dyx; cy[2] = by + dyy; cz[2] = bz + dyz; cw[2] = bw + dyw;
		cx[3] = cx[2] + dzx; cy[3] = cy[2] + dzy; cz[3] = cz[2] + dzz; cw[3] = cw[2] + dzw;
		cx[4] = bx + dxx; cy[4] = by + dxy; cz[4] = bz + dxz; cw[4] = bw + dxw;
		cx[5] = cx[4] + dzx; cy[5] = cy[4] + dzy; cz[5] = cz[4] + dzz; cw[5] = cw[4] + dzw;
		cx[6] = cx[4] + dyx; cy[6] = cy[4] + dyy; cz[6] = cz[4] + dyz; cw[6] = cw[4] + dyw;
		cx[7] = cx[6] + dzx; cy[7] = cy[6] + dzy; cz[7] = cz[6] + dzz; cw[7] = cw[6] + dzw;
	}

	/**
	 * {@link Math#round(float)} exactly, via the floor intrinsic: the double sum is exact, so the tie and the
	 * 0.49999997f cases come out the same, without the library call the JIT may otherwise emit.
	 */
	static int roundToInt(float v) {
		return (int) Math.floor(v + 0.5);
	}

	/** Shared tail of {@link #isPointClear}: normalized device position to pixel to tile bit. */
	final boolean isProjectedPointClear(float tx, float ty, float iw) {
		final float sx = tx * iw;
		final float sy = ty * iw;

		// NaN fails these too; the integer checks below catch the float rounding of (s + 1) up to exactly 2
		if (!(sx >= -1f && sx < 1f && sy >= -1f && sy < 1f)) {
			return false;
		}

		final int px = (int) ((sx + 1f) * halfPixelWidth);
		final int py = (int) ((sy + 1f) * halfPixelHeight);

		if (px >= pixelWidth || py >= pixelHeight) {
			return false;
		}

		final int tileIndex = (px >> TILE_AXIS_SHIFT) + ((py >> TILE_AXIS_SHIFT) * tileWidth);

		return (tiles[tileIndex] & (1L << (((py & TILE_PIXEL_INDEX_MASK) << TILE_AXIS_SHIFT) | (px & TILE_PIXEL_INDEX_MASK)))) == 0;
	}

	int needsNearClip(final int baseIndex) {
		final int[] data = vertexData;
		final float w = Float.intBitsToFloat(data[baseIndex + PV_W]);
		final float z = Float.intBitsToFloat(data[baseIndex + PV_Z]);

		if (w == 0) {
			return 1;
		} else if (w > 0) {
			return (z > 0 && z <= w) ? 0 : 1;
		} else {
			// w < 0
			return (z < 0 && z >= w) ? 0 : 1;
		}
	}

	/**
	 * Coverage of the tile in the given column of the band last passed to {@link #populateBand}. Each row's
	 * bits are the row's span clipped to the tile: shifts clamped to eight give an empty byte when the span
	 * misses the tile on that side, so no row needs a branch and the eight rows overlap in the pipeline.
	 */
	long computeTileCoverage(int tileX) {
		final int[] data = eventData;
		final int tileFirstX = tileX << TILE_AXIS_SHIFT;
		final int tileLastX = tileFirstX + 7;

		// events are in pairs, so the band's first row is at twice its pixel row
		final int b = eventY0 << 1;

		return rowCoverage(data[b], data[b + 1], tileFirstX, tileLastX)
				| rowCoverage(data[b + 2], data[b + 3], tileFirstX, tileLastX) << 8
				| rowCoverage(data[b + 4], data[b + 5], tileFirstX, tileLastX) << 16
				| rowCoverage(data[b + 6], data[b + 7], tileFirstX, tileLastX) << 24
				| rowCoverage(data[b + 8], data[b + 9], tileFirstX, tileLastX) << 32
				| rowCoverage(data[b + 10], data[b + 11], tileFirstX, tileLastX) << 40
				| rowCoverage(data[b + 12], data[b + 13], tileFirstX, tileLastX) << 48
				| rowCoverage(data[b + 14], data[b + 15], tileFirstX, tileLastX) << 56;
	}

	/** Bits of one tile row covered by the span {@code left..right} inclusive, in the low byte. */
	private static long rowCoverage(int left, int right, int tileFirstX, int tileLastX) {
		// pixels cut off the left and right of the tile, clamped to 0..8; 8 empties the byte
		final int l = Math.min(Math.max(left - tileFirstX, 0), 8);
		final int r = Math.min(Math.max(tileLastX - right, 0), 8);
		return (0xFFL << l) & (0xFFL >>> r);
	}

	public boolean isPixelClear(int x, int y) {
		return (tiles[tileIndexFromPixelXY(x, y)] & (1L << (Indexer.pixelIndex(x, y)))) == 0;
	}

	public boolean isPixelSet(int x, int y) {
		return (tiles[tileIndexFromPixelXY(x, y)] & (1L << (Indexer.pixelIndex(x, y)))) != 0;
	}

	void drawPixel(int x, int y) {
		tiles[tileIndexFromPixelXY(x, y)] |= (1L << (Indexer.pixelIndex(x, y)));
	}

	@FunctionalInterface
	interface EventFiller {
		void apply();
	}

	/**
	 * Classifies the input segment, returning one of the EDGE_ constants.
	 */
	final int edgePosition(int x0In, int y0In, int x1In, int y1In) {
		final int dy = y1In - y0In;
		final int dx = x1In - x0In;
		// signum of dx and dy, with shifted masks to derive the edge constant directly
		// the edge constants are specifically formulated to allow this, inline, avoids any pointer chases
		// sign of dy is inverted for historical reasons
		return (1 << (((-dy >> 31) | (dy >>> 31)) + 1)) | (1 << (((dx >> 31) | (-dx >>> 31)) + 4));
	}

	final int prepareBoundsNoClip(int v0, int v1, int v2, int v3) {
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

	void prepareEvents(int eventKey) {
		this.eventKey = eventKey;
		this.edgesReady = false;
		this.genericEvents = false;
	}

	/**
	 * Prepares an unclipped convex polygon of 3..6 projected vertices with consistent winding. Only the pixel
	 * bounds are computed here; the edges are prepared by {@link #prepareEdgesGeneric} the first time a
	 * band needs them, which a test that finds an untouched tile or a draw onto fully covered tiles never does.
	 */
	final int preparePolygonNoClip(int[] vertices, int count) {
		final int[] data = vertexData;

		int minX = data[vertices[0] + PV_PX];
		int maxX = minX;
		int minY = data[vertices[0] + PV_PY];
		int maxY = minY;

		for (int i = 1; i < count; i++) {
			final int x = data[vertices[i] + PV_PX];
			final int y = data[vertices[i] + PV_PY];
			if (x < minX) minX = x; else if (x > maxX) maxX = x;
			if (y < minY) minY = y; else if (y > maxY) maxY = y;
		}

		if (maxY <= 0 || minY >= preciseHeight || maxX <= 0 || minX >= preciseWidth) {
			return BOUNDS_OUTSIDE_OR_TOO_SMALL;
		}

		if (minX < 0) minX = 0;

		if (maxX >= preciseWidthClamp) {
			maxX = preciseWidthClamp;
			if (minX > preciseWidthClamp) minX = preciseWidthClamp;
		}

		if (minY < 0) minY = 0;

		if (maxY >= preciseHeightClamp) {
			maxY = preciseHeightClamp;
			if (minY > preciseHeightClamp) minY = preciseHeightClamp;
		}

		minPixelX = (minX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS;
		minPixelY = (minY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS;
		maxPixelX = (maxX + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS;
		maxPixelY = (maxY + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS;

		// rows whose centres fall inside [minY, maxY]; the bounds above may overshoot by a row, which is fine
		// for a scan range but not for deciding which rows the polygon actually spans
		polyMinRow = minPixelY;
		polyMaxRow = (maxY - PRECISE_PIXEL_CENTER) >> PRECISION_BITS;

		this.polyVertices = vertices;
		this.polyCount = count;
		this.genericEvents = true;
		this.edgesReady = false;
		return BOUNDS_IN;
	}

	final void drawPolygon(int[] vertices, int count) {
		if (STATS) STAT_DRAW_CALLS++;
		dilate = false;

		if (preparePolygonNoClip(vertices, count) == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return;
		}

		if (STATS) STAT_DRAW_QUADS++;

		// Don't draw single points
		if (minPixelX == maxPixelX && minPixelY == maxPixelY) {
			return;
		}

		drawQuad();
	}

	/**
	 * True if the pixel under any on-screen vertex of a projected polygon is clear. A sufficient test only:
	 * that pixel's centre is within half a pixel of the polygon, which the test dilation reaches except across
	 * a nearly flat edge, so this can call a polygon visible that the full test would not, never the reverse.
	 * Costs a few loads and no setup, and settles most sections whose centre is covered but that show at a
	 * corner, as sections cut by the horizon do.
	 */
	final boolean isAnyVertexPixelClear(int[] vertices, int count) {
		final int[] data = vertexData;
		final long[] tiles = this.tiles;
		final int lastPixelX = this.lastPixelX;
		final int lastPixelY = this.lastPixelY;

		for (int i = 0; i < count; i++) {
			final int v = vertices[i];
			final int px = data[v + PV_PX] >> PRECISION_BITS;
			final int py = data[v + PV_PY] >> PRECISION_BITS;

			if (px < 0 || py < 0 || px > lastPixelX || py > lastPixelY) {
				continue;
			}

			final int tileIndex = (px >> TILE_AXIS_SHIFT) + ((py >> TILE_AXIS_SHIFT) * tileWidth);

			if ((tiles[tileIndex] & (1L << (((py & TILE_PIXEL_INDEX_MASK) << TILE_AXIS_SHIFT) | (px & TILE_PIXEL_INDEX_MASK)))) == 0) {
				if (STATS) STAT_T_VERTEX++;
				return true;
			}
		}

		return false;
	}

	final boolean isPolygonPartiallyClear(int[] vertices, int count) {
		if (STATS) STAT_TEST_CALLS++;
		dilate = true;

		if (preparePolygonNoClip(vertices, count) == BOUNDS_OUTSIDE_OR_TOO_SMALL) {
			return false;
		}

		if (STATS) STAT_TEST_QUADS++;

		dilateBounds();

		if (isSmallPixelBoxCovered()) {
			if (STATS) STAT_T_BOX_HIDDEN++;
			return false;
		}

		return isQuadPartiallyClear();
	}

	/** Bounding rectangles of at most this many tiles are checked for full coverage before any edge setup. */
	private static final int COVERED_BOX_MAX_TILES = 16;

	/**
	 * True if every pixel of the current polygon's bounding rectangle is covered, which hides the polygon
	 * without preparing its edges. Only tried on small rectangles: a far section's is a few tiles, and
	 * those are the tests that end hidden, while a near section's would cost more than it could save.
	 */
	private boolean isSmallPixelBoxCovered() {
		final int minTileX = minPixelX >> TILE_AXIS_SHIFT;
		final int maxTileX = maxPixelX >> TILE_AXIS_SHIFT;
		final int minTileY = minPixelY >> TILE_AXIS_SHIFT;
		final int maxTileY = maxPixelY >> TILE_AXIS_SHIFT;

		if ((maxTileX - minTileX + 1) * (maxTileY - minTileY + 1) > COVERED_BOX_MAX_TILES) {
			return false;
		}

		final long[] tiles = this.tiles;
		final int tileWidth = this.tileWidth;

		for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
			// rows of this tile inside the rectangle, as a mask of whole tile rows
			final int r0 = tileY == minTileY ? minPixelY & TILE_PIXEL_INDEX_MASK : 0;
			final int r1 = tileY == maxTileY ? maxPixelY & TILE_PIXEL_INDEX_MASK : 7;
			final long rowsMask = (-1L << (r0 << TILE_AXIS_SHIFT)) & (-1L >>> ((7 - r1) << TILE_AXIS_SHIFT));
			final int rowBase = tileY * tileWidth;

			for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
				final int c0 = tileX == minTileX ? minPixelX & TILE_PIXEL_INDEX_MASK : 0;
				final int c1 = tileX == maxTileX ? maxPixelX & TILE_PIXEL_INDEX_MASK : 7;
				final long mask = (((0xFFL << c0) & (0xFFL >>> (7 - c1))) * 0x0101010101010101L) & rowsMask;

				if ((tiles[rowBase + tileX] & mask) != mask) {
					return false;
				}
			}
		}

		return true;
	}

	/** Slope and intercept of each non-flat edge of the current quad, in 20-bit fixed point, so bands only step them. */
	private void prepareEdges() {
		final int eventKey = this.eventKey;
		final int[] data = vertexData;
		final long[] edgeX0 = this.edgeX0;
		final long[] edgeStep = this.edgeStep;

		for (int e = 0; e < 4; e++) {
			final int role = (eventKey >> (e << 1)) & EVENT_POSITION_MASK;

			if (role == EVENT_0_FLAT) {
				continue;
			}

			final int i = IDX_AX0 + (e << 2);
			final int x0 = data[i];
			final int y0 = data[i + 1];
			final int dx = data[i + 2] - x0;

			if (dx == 0) {
				edgeStep[e] = 0;
				edgeX0[e] = (long) ((x0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			} else {
				// exact for these magnitudes and far cheaper than a 64-bit integer divide
				final long n = (long) ((double) (((long) dx) << 16) / (data[i + 3] - y0));
				edgeStep[e] = n << PRECISION_BITS;
				// left edges round up; right edges lose the tie
				edgeX0[e] = ((long) x0 << 16) - n * y0 + (role == EVENT_0_LEFT ? 0x100000L : 0x7FFFFL);
			}
		}
	}

	/**
	 * Builds the current polygon's left and right edge chains. With consistent winding an edge's vertical run
	 * tells its side, and each side's edges run monotonically in y, so each chain is collected in polygon order
	 * from wherever it starts and then read in ascending row order. An edge bounds the span only on the rows
	 * whose centres lie within its vertical extent; a nearly flat edge that contains no row centre is dropped,
	 * and the neighbouring edges are never evaluated outside their own extent. Deferred until a band needs
	 * it, which a test that finds an untouched tile or a draw onto fully covered tiles never does.
	 */
	private void prepareEdgesGeneric() {
		final int[] data = vertexData;
		final int[] vertices = this.polyVertices;
		final int count = this.polyCount;

		// which edges descend in polygon order; the rest ascend or are flat
		int descending = 0;
		int y0 = data[vertices[0] + PV_PY];

		for (int e = 0; e < count; e++) {
			final int y1 = data[vertices[e + 1 == count ? 0 : e + 1] + PV_PY];

			if (y1 < y0) {
				descending |= 1 << e;
			} else if (y1 == y0) {
				// flat edges belong to neither chain; they only ever join two sloped edges
				descending |= 1 << (e + 8);
			}

			y0 = y1;
		}

		final int flat = descending >>> 8;
		final int all = (1 << count) - 1;
		descending &= all;
		leftCount = prepareChain(descending, true, leftX0, leftStep, leftEnd);
		rightCount = prepareChain(all & ~descending & ~flat, false, rightX0, rightStep, rightEnd);
	}

	/**
	 * Emits the edges in {@code members}, which form one contiguous run around the polygon, in ascending row
	 * order. Left chains descend in polygon order, so they are read backwards.
	 */
	private int prepareChain(int members, boolean left, long[] x0Out, long[] stepOut, int[] endOut) {
		final int[] data = vertexData;
		final int[] vertices = this.polyVertices;
		final int count = this.polyCount;
		final int[] chain = this.chainEdges;

		if (members == 0) {
			return 0;
		}

		// the run starts at a member whose predecessor is not one; a run of every edge is impossible
		final int predecessors = ((members << 1) | (members >>> (count - 1))) & ((1 << count) - 1);
		int start = Integer.numberOfTrailingZeros(members & ~predecessors);
		int chained = 0;

		for (int e = start; (members & (1 << e)) != 0; ) {
			chain[chained++] = e;

			if (++e == count) {
				e = 0;
			}
		}

		int emitted = 0;

		for (int i = 0; i < chained; i++) {
			final int e = chain[left ? chained - 1 - i : i];
			final int v0 = vertices[e];
			final int v1 = vertices[e + 1 == count ? 0 : e + 1];
			final int ex0 = data[v0 + PV_PX], ey0 = data[v0 + PV_PY];
			final int ex1 = data[v1 + PV_PX], ey1 = data[v1 + PV_PY];
			final int yLow = Math.min(ey0, ey1), yHigh = Math.max(ey0, ey1);

			// rows whose centres lie within the edge; a nearly flat edge may hold none
			final int rowLow = (yLow + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS;
			final int rowHigh = (yHigh - PRECISE_PIXEL_CENTER) >> PRECISION_BITS;

			if (rowLow > rowHigh) {
				continue;
			}

			final int dx = ex1 - ex0;

			if (dx == 0) {
				stepOut[emitted] = 0;
				x0Out[emitted] = (long) ((ex0 + SCANT_PRECISE_PIXEL_CENTER) >> PRECISION_BITS) << 20;
			} else {
				// exact for these magnitudes and far cheaper than a 64-bit integer divide
				final long n = (long) ((double) (((long) dx) << 16) / (ey1 - ey0));
				stepOut[emitted] = n << PRECISION_BITS;
				// left edges round up; right edges lose the tie
				x0Out[emitted] = ((long) ex0 << 16) - n * ey0 + (left ? 0x100000L : 0x7FFFFL);
			}

			endOut[emitted++] = rowHigh;
		}

		return emitted;
	}

	/**
	 * Band filler for polygons set up by {@link #preparePolygonNoClip}. Rows outside the polygon's vertical
	 * extent are empty; every other row's span runs from the left chain's edge for that row to the right
	 * chain's. Under {@link #dilate} the rows just past either end take the end row's span, grown like every
	 * row by the dilation. The band's interior and extent tile ranges are gathered in the same pass.
	 */
	private void populateBandGeneric(int y0) {
		final int[] eventData = this.eventData;
		final long[] leftX0 = this.leftX0;
		final long[] leftStep = this.leftStep;
		final int[] leftEnd = this.leftEnd;
		final long[] rightX0 = this.rightX0;
		final long[] rightStep = this.rightStep;
		final int[] rightEnd = this.rightEnd;
		final int lastLeft = leftCount - 1;
		final int lastRight = rightCount - 1;
		final int grow = dilate ? TEST_DILATION_PIXELS : 0;
		final int rowMin = polyMinRow;
		final int rowMax = polyMaxRow;
		final int firstRow = rowMin - grow;
		final int lastRow = rowMax + grow;
		final int emptyLeft = pixelWidth;
		final int screenRight = lastPixelX;

		// the chains ascend, as do the rows, so the active edge only ever moves forward
		final int firstActive = y0 < rowMin ? rowMin : (y0 > rowMax ? rowMax : y0);
		int li = 0, ri = 0;
		while (li < lastLeft && firstActive > leftEnd[li]) li++;
		while (ri < lastRight && firstActive > rightEnd[ri]) ri++;
		long lx0 = lastLeft < 0 ? 0 : leftX0[li], ls = lastLeft < 0 ? 0 : leftStep[li];
		long rx0 = lastRight < 0 ? 0 : rightX0[ri], rs = lastRight < 0 ? 0 : rightStep[ri];
		int lEnd = li < lastLeft ? leftEnd[li] : Integer.MAX_VALUE;
		int rEnd = ri < lastRight ? rightEnd[ri] : Integer.MAX_VALUE;

		int maxLeft = Integer.MIN_VALUE, minRight = Integer.MAX_VALUE;
		int minLeft = Integer.MAX_VALUE, maxRight = Integer.MIN_VALUE;

		for (int y = y0, i = y0 << 1, end = y0 + 8; y < end; y++, i += 2) {
			int l, r;

			if (y < firstRow || y > lastRow) {
				l = emptyLeft;
				r = -1;
			} else {
				final int ey = y < rowMin ? rowMin : (y > rowMax ? rowMax : y);

				if (ey > lEnd) {
					li++;
					lx0 = leftX0[li];
					ls = leftStep[li];
					lEnd = li < lastLeft ? leftEnd[li] : Integer.MAX_VALUE;
				}

				if (ey > rEnd) {
					ri++;
					rx0 = rightX0[ri];
					rs = rightStep[ri];
					rEnd = ri < lastRight ? rightEnd[ri] : Integer.MAX_VALUE;
				}

				l = (lastLeft < 0 ? 0 : (int) ((lx0 + ls * ey) >> 20)) - grow;
				r = (lastRight < 0 ? screenRight : (int) ((rx0 + rs * ey) >> 20)) + grow;

				if (l < minLeft) minLeft = l;
				if (r > maxRight) maxRight = r;
			}

			eventData[i] = l;
			eventData[i + 1] = r;
			if (l > maxLeft) maxLeft = l;
			if (r < minRight) minRight = r;
		}

		setBandRanges(maxLeft, minRight, minLeft, maxRight);
	}

	/** Derives the band's interior and extent tile columns from its narrowest and widest row spans. */
	private void setBandRanges(int maxLeft, int minRight, int minLeft, int maxRight) {
		bandInnerFirst = (maxLeft + 7) >> TILE_AXIS_SHIFT;
		bandInnerLast = (minRight - 7) >> TILE_AXIS_SHIFT;

		if (minLeft <= maxRight) {
			bandOuterFirst = Math.max(minLeft, 0) >> TILE_AXIS_SHIFT;
			bandOuterLast = Math.min(maxRight, lastPixelX) >> TILE_AXIS_SHIFT;
		} else {
			// an empty band: its extent starts past the row's last tile and ends before its first
			bandOuterFirst = tileWidth;
			bandOuterLast = -1;
		}
	}

	final void populateBand(int tileY) {
		final boolean generic = this.genericEvents;

		if (!edgesReady) {
			if (generic) {
				prepareEdgesGeneric();
			} else {
				prepareEdges();
			}

			edgesReady = true;
		}

		final int y0 = tileY << TILE_AXIS_SHIFT;
		eventY0 = y0;
		eventLimit = (y0 + 7) << 1;
		if (STATS) STAT_EVENT_ROWS += 8;

		if (generic) {
			populateBandGeneric(y0);
			return;
		}

		EVENT_FILLERS[eventKey].apply();

		// one pass over the rows dilates test polygons and finds the band's narrowest and widest spans: tiles
		// between the widest left event and the narrowest right event are covered on every row, and tiles
		// outside the narrowest left and widest right events on none
		final int[] data = eventData;
		final int grow = dilate ? TEST_DILATION_PIXELS : 0;
		int maxLeft = Integer.MIN_VALUE, minRight = Integer.MAX_VALUE;
		int minLeft = Integer.MAX_VALUE, maxRight = Integer.MIN_VALUE;

		for (int i = y0 << 1, limit = i + 16; i < limit; i += 2) {
			int l = data[i];
			int r = data[i + 1];

			if (l <= r) {
				l -= grow;
				r += grow;
				data[i] = l;
				data[i + 1] = r;
				if (l < minLeft) minLeft = l;
				if (r > maxRight) maxRight = r;
			}

			if (l > maxLeft) maxLeft = l;
			if (r < minRight) minRight = r;
		}

		setBandRanges(maxLeft, minRight, minLeft, maxRight);
	}
}
