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

public class Constants {
	public static final int UP = 1 << 1;
	public static final int DOWN = 1 << 0;
	public static final int EAST = 1 << 5;
	public static final int WEST = 1 << 4;
	public static final int NORTH = 1 << 2;
	public static final int SOUTH = 1 << 3;
	public static final int TILE_AXIS_SHIFT = 3;
	static final int TILE_AXIS_MASK = ~((1 << TILE_AXIS_SHIFT) - 1);
	static final int TILE_PIXEL_DIAMETER = 1 << TILE_AXIS_SHIFT;
	static final int TILE_PIXEL_INDEX_MASK = TILE_PIXEL_DIAMETER - 1;
	static final int TILE_PIXEL_INVERSE_MASK = ~TILE_PIXEL_INDEX_MASK;
	static final int PRECISION_BITS = 4;
	static final int PRECISE_FRACTION_MASK = (1 << PRECISION_BITS) - 1;
	static final int PRECISE_INTEGER_MASK = ~PRECISE_FRACTION_MASK;
	static final int PRECISE_PIXEL_SIZE = 1 << PRECISION_BITS;
	static final int PRECISE_PIXEL_CENTER = PRECISE_PIXEL_SIZE / 2;
	static final int SCANT_PRECISE_PIXEL_CENTER = PRECISE_PIXEL_CENTER - 1;
	// CELERITAS: the buffer size is chosen per rasterizer and per axis (see AbstractRasterizer.resize); upstream
	// fixed it at 2048px square.
	/**
	 * Occlusion buffer size along one axis in tiles of eight pixels. Any width in range is allowed, not just
	 * powers of two, so an axis can be sized to what the search distance needs without rounding up to double it.
	 */
	public static final int MIN_SIZE_TILES = 64;
	public static final int DEFAULT_SIZE_TILES = 128;
	public static final int MAX_SIZE_TILES = 256;

	// CELERITAS: pixels added on every side of a tested polygon so that a visible strip narrower than the pixel
	// pitch still reaches a sampled pixel centre. One pixel covers centre sampling exactly; occluders are never grown.
	static final int TEST_DILATION_PIXELS = 1;

	public static final int CAMERA_PRECISION_BITS = 12;
	static final int CAMERA_PRECISION_UNITY = 1 << CAMERA_PRECISION_BITS;
	static final int CAMERA_PRECISION_CHUNK_MAX = 18 * CAMERA_PRECISION_UNITY;
	static final int CAMERA_PRECISION_HALF = CAMERA_PRECISION_UNITY / 2;
	static final int BOUNDS_IN = 0;
	static final int BOUNDS_OUTSIDE_OR_TOO_SMALL = 1;
	static final int B_NEGATIVE = 8;
	static final int B_ZERO = 16;
	static final int B_POSITIVE = 32;
	static final int A_NEGATIVE = 1;
	static final int A_ZERO = 2;
	static final int A_POSITIVE = 4;
	static final int EDGE_TOP = B_NEGATIVE | A_ZERO;
	static final int EDGE_BOTTOM = B_POSITIVE | A_ZERO;
	static final int EDGE_RIGHT = B_ZERO | A_NEGATIVE;
	static final int EDGE_LEFT = B_ZERO | A_POSITIVE;
	static final int EDGE_TOP_RIGHT = B_NEGATIVE | A_NEGATIVE;
	static final int EDGE_TOP_LEFT = B_NEGATIVE | A_POSITIVE;
	static final int EDGE_BOTTOM_RIGHT = B_POSITIVE | A_NEGATIVE;
	static final int EDGE_BOTTOM_LEFT = B_POSITIVE | A_POSITIVE;
	static final int EDGE_POINT = A_ZERO | B_ZERO;
	// subtract 1 to fit in 2 bits
	static final int EVENT_0_LEFT = A_POSITIVE - 1;
	static final int EVENT_0_FLAT = A_ZERO - 1;
	static final int EVENT_0_RIGHT = A_NEGATIVE - 1;
	static final int EVENT_POSITION_MASK = 3;
	static final int EVENT_1_LEFT = EVENT_0_LEFT << 2;
	static final int EVENT_1_FLAT = EVENT_0_FLAT << 2;
	static final int EVENT_1_RIGHT = EVENT_0_RIGHT << 2;
	static final int EVENT_2_LEFT = EVENT_1_LEFT << 2;
	static final int EVENT_2_FLAT = EVENT_1_FLAT << 2;
	static final int EVENT_2_RIGHT = EVENT_1_RIGHT << 2;
	static final int EVENT_3_LEFT = EVENT_2_LEFT << 2;
	static final int EVENT_3_FLAT = EVENT_2_FLAT << 2;
	static final int EVENT_3_RIGHT = EVENT_2_RIGHT << 2;
	static final int EVENT_012_RRR = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_RIGHT;
	static final int EVENT_012_LRR = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_RIGHT;
	static final int EVENT_012_FRR = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_RIGHT;
	static final int EVENT_012_RLR = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_RIGHT;
	static final int EVENT_012_LLR = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_RIGHT;
	static final int EVENT_012_FLR = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_RIGHT;
	static final int EVENT_012_RFR = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_RIGHT;
	static final int EVENT_012_LFR = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_RIGHT;
	static final int EVENT_012_FFR = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_RIGHT;
	static final int EVENT_012_RRL = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_LEFT;
	static final int EVENT_012_LRL = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_LEFT;
	static final int EVENT_012_FRL = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_LEFT;
	static final int EVENT_012_RLL = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_LEFT;
	static final int EVENT_012_LLL = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_LEFT;
	static final int EVENT_012_FLL = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_LEFT;
	static final int EVENT_012_RFL = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_LEFT;
	static final int EVENT_012_LFL = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_LEFT;
	static final int EVENT_012_FFL = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_LEFT;
	static final int EVENT_012_LRF = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_FLAT;
	static final int EVENT_012_RRF = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_FLAT;
	static final int EVENT_012_FRF = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_FLAT;

	////
	static final int EVENT_012_RLF = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_FLAT;
	static final int EVENT_012_LLF = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_FLAT;
	static final int EVENT_012_FLF = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_FLAT;
	static final int EVENT_012_RFF = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_FLAT;
	static final int EVENT_012_LFF = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_FLAT;
	static final int EVENT_012_FFF = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_FLAT;
	static final int EVENT_0123_RRRR = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_RIGHT | EVENT_3_RIGHT;
	static final int EVENT_0123_LRRR = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_RIGHT | EVENT_3_RIGHT;
	static final int EVENT_0123_FRRR = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_RIGHT | EVENT_3_RIGHT;
	static final int EVENT_0123_RLRR = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_RIGHT | EVENT_3_RIGHT;
	static final int EVENT_0123_LLRR = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_RIGHT | EVENT_3_RIGHT;
	static final int EVENT_0123_FLRR = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_RIGHT | EVENT_3_RIGHT;
	static final int EVENT_0123_RFRR = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_RIGHT | EVENT_3_RIGHT;
	static final int EVENT_0123_LFRR = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_RIGHT | EVENT_3_RIGHT;
	static final int EVENT_0123_FFRR = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_RIGHT | EVENT_3_RIGHT;
	static final int EVENT_0123_RRLR = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_LEFT | EVENT_3_RIGHT;
	static final int EVENT_0123_LRLR = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_LEFT | EVENT_3_RIGHT;
	static final int EVENT_0123_FRLR = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_LEFT | EVENT_3_RIGHT;
	static final int EVENT_0123_RLLR = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_LEFT | EVENT_3_RIGHT;
	static final int EVENT_0123_LLLR = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_LEFT | EVENT_3_RIGHT;
	static final int EVENT_0123_FLLR = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_LEFT | EVENT_3_RIGHT;
	static final int EVENT_0123_RFLR = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_LEFT | EVENT_3_RIGHT;
	static final int EVENT_0123_LFLR = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_LEFT | EVENT_3_RIGHT;
	static final int EVENT_0123_FFLR = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_LEFT | EVENT_3_RIGHT;
	static final int EVENT_0123_LRFR = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_FLAT | EVENT_3_RIGHT;
	static final int EVENT_0123_RRFR = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_FLAT | EVENT_3_RIGHT;
	static final int EVENT_0123_FRFR = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_FLAT | EVENT_3_RIGHT;
	static final int EVENT_0123_RLFR = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_FLAT | EVENT_3_RIGHT;
	static final int EVENT_0123_LLFR = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_FLAT | EVENT_3_RIGHT;
	static final int EVENT_0123_FLFR = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_FLAT | EVENT_3_RIGHT;
	static final int EVENT_0123_RFFR = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_FLAT | EVENT_3_RIGHT;
	static final int EVENT_0123_LFFR = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_FLAT | EVENT_3_RIGHT;
	static final int EVENT_0123_FFFR = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_FLAT | EVENT_3_RIGHT;
	static final int EVENT_0123_RRRL = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_RIGHT | EVENT_3_LEFT;
	static final int EVENT_0123_LRRL = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_RIGHT | EVENT_3_LEFT;
	static final int EVENT_0123_FRRL = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_RIGHT | EVENT_3_LEFT;
	static final int EVENT_0123_RLRL = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_RIGHT | EVENT_3_LEFT;
	static final int EVENT_0123_LLRL = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_RIGHT | EVENT_3_LEFT;
	static final int EVENT_0123_FLRL = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_RIGHT | EVENT_3_LEFT;
	static final int EVENT_0123_RFRL = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_RIGHT | EVENT_3_LEFT;
	static final int EVENT_0123_LFRL = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_RIGHT | EVENT_3_LEFT;
	static final int EVENT_0123_FFRL = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_RIGHT | EVENT_3_LEFT;
	static final int EVENT_0123_RRLL = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_LEFT | EVENT_3_LEFT;
	static final int EVENT_0123_LRLL = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_LEFT | EVENT_3_LEFT;
	static final int EVENT_0123_FRLL = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_LEFT | EVENT_3_LEFT;
	static final int EVENT_0123_RLLL = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_LEFT | EVENT_3_LEFT;
	static final int EVENT_0123_LLLL = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_LEFT | EVENT_3_LEFT;
	static final int EVENT_0123_FLLL = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_LEFT | EVENT_3_LEFT;
	static final int EVENT_0123_RFLL = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_LEFT | EVENT_3_LEFT;
	static final int EVENT_0123_LFLL = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_LEFT | EVENT_3_LEFT;
	static final int EVENT_0123_FFLL = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_LEFT | EVENT_3_LEFT;
	static final int EVENT_0123_LRFL = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_FLAT | EVENT_3_LEFT;
	static final int EVENT_0123_RRFL = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_FLAT | EVENT_3_LEFT;
	static final int EVENT_0123_FRFL = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_FLAT | EVENT_3_LEFT;
	static final int EVENT_0123_RLFL = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_FLAT | EVENT_3_LEFT;
	static final int EVENT_0123_LLFL = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_FLAT | EVENT_3_LEFT;
	static final int EVENT_0123_FLFL = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_FLAT | EVENT_3_LEFT;
	static final int EVENT_0123_RFFL = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_FLAT | EVENT_3_LEFT;
	static final int EVENT_0123_LFFL = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_FLAT | EVENT_3_LEFT;
	static final int EVENT_0123_FFFL = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_FLAT | EVENT_3_LEFT;
	static final int EVENT_0123_RRRF = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_RIGHT | EVENT_3_FLAT;
	static final int EVENT_0123_LRRF = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_RIGHT | EVENT_3_FLAT;
	static final int EVENT_0123_FRRF = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_RIGHT | EVENT_3_FLAT;
	static final int EVENT_0123_RLRF = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_RIGHT | EVENT_3_FLAT;
	static final int EVENT_0123_LLRF = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_RIGHT | EVENT_3_FLAT;
	static final int EVENT_0123_FLRF = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_RIGHT | EVENT_3_FLAT;
	static final int EVENT_0123_RFRF = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_RIGHT | EVENT_3_FLAT;
	static final int EVENT_0123_LFRF = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_RIGHT | EVENT_3_FLAT;
	static final int EVENT_0123_FFRF = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_RIGHT | EVENT_3_FLAT;
	static final int EVENT_0123_RRLF = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_LEFT | EVENT_3_FLAT;
	static final int EVENT_0123_LRLF = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_LEFT | EVENT_3_FLAT;
	static final int EVENT_0123_FRLF = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_LEFT | EVENT_3_FLAT;
	static final int EVENT_0123_RLLF = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_LEFT | EVENT_3_FLAT;
	static final int EVENT_0123_LLLF = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_LEFT | EVENT_3_FLAT;
	static final int EVENT_0123_FLLF = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_LEFT | EVENT_3_FLAT;
	static final int EVENT_0123_RFLF = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_LEFT | EVENT_3_FLAT;
	static final int EVENT_0123_LFLF = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_LEFT | EVENT_3_FLAT;
	static final int EVENT_0123_FFLF = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_LEFT | EVENT_3_FLAT;
	static final int EVENT_0123_LRFF = EVENT_0_LEFT | EVENT_1_RIGHT | EVENT_2_FLAT | EVENT_3_FLAT;
	static final int EVENT_0123_RRFF = EVENT_0_RIGHT | EVENT_1_RIGHT | EVENT_2_FLAT | EVENT_3_FLAT;
	static final int EVENT_0123_FRFF = EVENT_0_FLAT | EVENT_1_RIGHT | EVENT_2_FLAT | EVENT_3_FLAT;
	static final int EVENT_0123_RLFF = EVENT_0_RIGHT | EVENT_1_LEFT | EVENT_2_FLAT | EVENT_3_FLAT;
	static final int EVENT_0123_LLFF = EVENT_0_LEFT | EVENT_1_LEFT | EVENT_2_FLAT | EVENT_3_FLAT;
	static final int EVENT_0123_FLFF = EVENT_0_FLAT | EVENT_1_LEFT | EVENT_2_FLAT | EVENT_3_FLAT;
	static final int EVENT_0123_RFFF = EVENT_0_RIGHT | EVENT_1_FLAT | EVENT_2_FLAT | EVENT_3_FLAT;
	static final int EVENT_0123_LFFF = EVENT_0_LEFT | EVENT_1_FLAT | EVENT_2_FLAT | EVENT_3_FLAT;
	static final int EVENT_0123_FFFF = EVENT_0_FLAT | EVENT_1_FLAT | EVENT_2_FLAT | EVENT_3_FLAT;

	static final int PV_PX = 0;
	static final int PV_PY = 1;
	static final int PV_X = 2;
	static final int PV_Y = 3;
	static final int PV_Z = 4;
	static final int PV_W = 5;
	static final int PROJECTED_VERTEX_STRIDE = 6;

	static final int V000 = 0;
	static final int V001 = V000 + PROJECTED_VERTEX_STRIDE;
	static final int V010 = V001 + PROJECTED_VERTEX_STRIDE;
	static final int V011 = V010 + PROJECTED_VERTEX_STRIDE;
	static final int V100 = V011 + PROJECTED_VERTEX_STRIDE;
	static final int V101 = V100 + PROJECTED_VERTEX_STRIDE;
	static final int V110 = V101 + PROJECTED_VERTEX_STRIDE;
	static final int V111 = V110 + PROJECTED_VERTEX_STRIDE;
	static final int IDX_AX0 = V111 + PROJECTED_VERTEX_STRIDE;
	static final int IDX_AY0 = IDX_AX0 + 1;
	static final int IDX_AX1 = IDX_AY0 + 1;
	static final int IDX_AY1 = IDX_AX1 + 1;

	static final int IDX_BX0 = IDX_AY1 + 1;
	static final int IDX_BY0 = IDX_BX0 + 1;
	static final int IDX_BX1 = IDX_BY0 + 1;
	static final int IDX_BY1 = IDX_BX1 + 1;

	static final int IDX_CX0 = IDX_BY1 + 1;
	static final int IDX_CY0 = IDX_CX0 + 1;
	static final int IDX_CX1 = IDX_CY0 + 1;
	static final int IDX_CY1 = IDX_CX1 + 1;

	static final int IDX_DX0 = IDX_CY1 + 1;
	static final int IDX_DY0 = IDX_DX0 + 1;
	static final int IDX_DX1 = IDX_DY0 + 1;
	static final int IDX_DY1 = IDX_DX1 + 1;

	static final int IDX_EX0 = IDX_DY1 + 1;
	static final int IDX_EY0 = IDX_EX0 + 1;
	static final int IDX_EX1 = IDX_EY0 + 1;
	static final int IDX_EY1 = IDX_EX1 + 1;
	static final int IDX_FX0 = IDX_EY1 + 1;
	static final int IDX_FY0 = IDX_FX0 + 1;
	static final int IDX_FX1 = IDX_FY0 + 1;
	static final int IDX_FY1 = IDX_FX1 + 1;
	static final int VERTEX_DATA_LENGTH = IDX_FY1 + 1;

}
