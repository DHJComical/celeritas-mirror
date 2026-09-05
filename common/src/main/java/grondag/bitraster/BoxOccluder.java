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

import static grondag.bitraster.Constants.CAMERA_PRECISION_BITS;
import static grondag.bitraster.Constants.CAMERA_PRECISION_UNITY;
import static grondag.bitraster.Constants.DOWN;
import static grondag.bitraster.Constants.EAST;
import static grondag.bitraster.Constants.NORTH;
import static grondag.bitraster.Constants.PROJECTED_VERTEX_STRIDE;
import static grondag.bitraster.Constants.SOUTH;
import static grondag.bitraster.Constants.UP;
import static grondag.bitraster.Constants.V000;
import static grondag.bitraster.Constants.V001;
import static grondag.bitraster.Constants.V010;
import static grondag.bitraster.Constants.V011;
import static grondag.bitraster.Constants.V100;
import static grondag.bitraster.Constants.V101;
import static grondag.bitraster.Constants.V110;
import static grondag.bitraster.Constants.V111;
import static grondag.bitraster.Constants.WEST;

import java.util.function.Consumer;

public abstract class BoxOccluder {
	/** How close face must be to trigger aggressive refresh of occlusion. */
	private static final int NEAR_RANGE = 8 << CAMERA_PRECISION_BITS;

	private final Matrix4L baseMvpMatrix = new Matrix4L();

	protected final AbstractRasterizer raster;
	private int occlusionVersion = 1;
	protected final BoxTest[] partiallyClearTests = new BoxTest[128];
	protected final BoxTest[] partiallyOccludedTests = new BoxTest[128];
	protected final BoxDraw[] boxDraws = new BoxDraw[128];
	private long viewX;
	private long viewY;
	private long viewZ;
	// Add these to region-relative box coordinates to get camera-relative coordinates
	// They are in camera fixed precision.
	protected int offsetX;
	protected int offsetY;
	protected int offsetZ;
	private int occlusionRange;
	private int regionSquaredChunkDist;
	private int viewVersion = -1;
	private int regionVersion = -1;
	private volatile boolean forceRedraw = false;
	private int maxSquaredChunkDistance;
	private boolean hasNearOccluders = false;
	private boolean drawNearOccluders = true;

	/**
	 * Outline of a box as seen from the camera side encoded by the face-visibility outcome: the union of the
	 * visible faces' quads with their shared edges removed, wound consistently with the quad tables below.
	 * Drawing or testing this one convex polygon replaces two or three overlapping quads. The quad tables
	 * remain as the fallback when a corner needs near-plane clipping.
	 */
	private static final int[][] SILHOUETTES = new int[64][];

	static {
		SILHOUETTES[UP] = new int[] { V110, V010, V011, V111 };
		SILHOUETTES[DOWN] = new int[] { V000, V100, V101, V001 };
		SILHOUETTES[EAST] = new int[] { V101, V100, V110, V111 };
		SILHOUETTES[WEST] = new int[] { V000, V001, V011, V010 };
		SILHOUETTES[NORTH] = new int[] { V100, V000, V010, V110 };
		SILHOUETTES[SOUTH] = new int[] { V001, V101, V111, V011 };
		SILHOUETTES[UP | EAST] = new int[] { V110, V010, V011, V111, V101, V100 };
		SILHOUETTES[UP | WEST] = new int[] { V011, V111, V110, V010, V000, V001 };
		SILHOUETTES[UP | NORTH] = new int[] { V010, V011, V111, V110, V100, V000 };
		SILHOUETTES[UP | SOUTH] = new int[] { V111, V110, V010, V011, V001, V101 };
		SILHOUETTES[DOWN | EAST] = new int[] { V101, V001, V000, V100, V110, V111 };
		SILHOUETTES[DOWN | WEST] = new int[] { V000, V100, V101, V001, V011, V010 };
		SILHOUETTES[DOWN | NORTH] = new int[] { V100, V101, V001, V000, V010, V110 };
		SILHOUETTES[DOWN | SOUTH] = new int[] { V001, V000, V100, V101, V111, V011 };
		SILHOUETTES[NORTH | EAST] = new int[] { V100, V000, V010, V110, V111, V101 };
		SILHOUETTES[NORTH | WEST] = new int[] { V010, V110, V100, V000, V001, V011 };
		SILHOUETTES[SOUTH | EAST] = new int[] { V111, V011, V001, V101, V100, V110 };
		SILHOUETTES[SOUTH | WEST] = new int[] { V001, V101, V111, V011, V010, V000 };
		SILHOUETTES[UP | EAST | NORTH] = new int[] { V011, V111, V101, V100, V000, V010 };
		SILHOUETTES[UP | WEST | NORTH] = new int[] { V111, V110, V100, V000, V001, V011 };
		SILHOUETTES[UP | EAST | SOUTH] = new int[] { V010, V011, V001, V101, V100, V110 };
		SILHOUETTES[UP | WEST | SOUTH] = new int[] { V110, V010, V000, V001, V101, V111 };
		SILHOUETTES[DOWN | EAST | NORTH] = new int[] { V001, V000, V010, V110, V111, V101 };
		SILHOUETTES[DOWN | WEST | NORTH] = new int[] { V101, V001, V011, V010, V110, V100 };
		SILHOUETTES[DOWN | EAST | SOUTH] = new int[] { V000, V100, V110, V111, V011, V001 };
		SILHOUETTES[DOWN | WEST | SOUTH] = new int[] { V100, V101, V111, V011, V010, V000 };
	}

	public BoxOccluder(AbstractRasterizer raster) {
		this.raster = raster;

		partiallyClearTests[0] = (x0, y0, z0, x1, y1, z1) -> {
			return false;
		};

		partiallyClearTests[UP] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V110, V010, V011, V111);
		};

		partiallyClearTests[DOWN] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			return raster.isQuadPartiallyClear(V000, V100, V101, V001);
		};

		partiallyClearTests[EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V101, V100, V110, V111);
		};

		partiallyClearTests[WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			return raster.isQuadPartiallyClear(V000, V001, V011, V010);
		};

		partiallyClearTests[NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyClear(V100, V000, V010, V110);
		};

		partiallyClearTests[SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V001, V101, V111, V011);
		};

		// NB: Split across two quads to give more evenly-sized test regions vs potentially one big and one very small
		partiallyClearTests[UP | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V110, V010, V011, V111)
					|| raster.isQuadPartiallyClear(V101, V100, V110, V111);
		};

		partiallyClearTests[UP | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V110, V010, V011, V111)
					|| raster.isQuadPartiallyClear(V000, V001, V011, V010);
		};

		partiallyClearTests[UP | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V110, V010, V011, V111)
					|| raster.isQuadPartiallyClear(V100, V000, V010, V110);
		};

		partiallyClearTests[UP | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V110, V010, V011, V111)
					|| raster.isQuadPartiallyClear(V001, V101, V111, V011);
		};

		partiallyClearTests[DOWN | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V000, V100, V101, V001)
					|| raster.isQuadPartiallyClear(V101, V100, V110, V111);
		};

		partiallyClearTests[DOWN | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			return raster.isQuadPartiallyClear(V000, V100, V101, V001)
					|| raster.isQuadPartiallyClear(V000, V001, V011, V010);
		};

		partiallyClearTests[DOWN | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyClear(V000, V100, V101, V001)
					|| raster.isQuadPartiallyClear(V100, V000, V010, V110);
		};

		partiallyClearTests[DOWN | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V000, V100, V101, V001)
					|| raster.isQuadPartiallyClear(V001, V101, V111, V011);
		};

		partiallyClearTests[NORTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V100, V000, V010, V110)
					|| raster.isQuadPartiallyClear(V101, V100, V110, V111);
		};

		partiallyClearTests[NORTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyClear(V100, V000, V010, V110)
					|| raster.isQuadPartiallyClear(V000, V001, V011, V010);
		};

		partiallyClearTests[SOUTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V001, V101, V111, V011)
					|| raster.isQuadPartiallyClear(V101, V100, V110, V111);
		};

		partiallyClearTests[SOUTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V001, V101, V111, V011)
					|| raster.isQuadPartiallyClear(V000, V001, V011, V010);
		};

		// NB: When three faces are visible, omit nearest vertex and draw two quads instead of three.

		partiallyClearTests[UP | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V011, V111, V101, V100)
					|| raster.isQuadPartiallyClear(V100, V000, V010, V011);
		};

		partiallyClearTests[UP | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V111, V110, V100, V000)
					|| raster.isQuadPartiallyClear(V000, V001, V011, V111);
		};

		partiallyClearTests[UP | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyClear(V010, V011, V001, V101)
					|| raster.isQuadPartiallyClear(V101, V100, V110, V010);
		};

		partiallyClearTests[UP | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V110, V010, V000, V001)
					|| raster.isQuadPartiallyClear(V001, V101, V111, V110);
		};

		partiallyClearTests[DOWN | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V001, V000, V010, V110)
					|| raster.isQuadPartiallyClear(V110, V111, V101, V001);
		};

		partiallyClearTests[DOWN | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyClear(V101, V001, V011, V010)
					|| raster.isQuadPartiallyClear(V010, V110, V100, V101);
		};

		partiallyClearTests[DOWN | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V000, V100, V110, V111)
					|| raster.isQuadPartiallyClear(V111, V011, V001, V000);
		};

		partiallyClearTests[DOWN | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyClear(V100, V101, V111, V011)
					|| raster.isQuadPartiallyClear(V011, V010, V000, V100);
		};

		////

		partiallyOccludedTests[0] = (x0, y0, z0, x1, y1, z1) -> {
			return false;
		};

		partiallyOccludedTests[UP] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V110, V010, V011, V111);
		};

		partiallyOccludedTests[DOWN] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			return raster.isQuadPartiallyOccluded(V000, V100, V101, V001);
		};

		partiallyOccludedTests[EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V101, V100, V110, V111);
		};

		partiallyOccludedTests[WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			return raster.isQuadPartiallyOccluded(V000, V001, V011, V010);
		};

		partiallyOccludedTests[NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyOccluded(V100, V000, V010, V110);
		};

		partiallyOccludedTests[SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V001, V101, V111, V011);
		};

		// NB: Split across two quads to give more evenly-sized test regions vs potentially one big and one very small
		partiallyOccludedTests[UP | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V110, V010, V011, V111)
					|| raster.isQuadPartiallyOccluded(V101, V100, V110, V111);
		};

		partiallyOccludedTests[UP | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V110, V010, V011, V111)
					|| raster.isQuadPartiallyOccluded(V000, V001, V011, V010);
		};

		partiallyOccludedTests[UP | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V110, V010, V011, V111)
					|| raster.isQuadPartiallyOccluded(V100, V000, V010, V110);
		};

		partiallyOccludedTests[UP | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V110, V010, V011, V111)
					|| raster.isQuadPartiallyOccluded(V001, V101, V111, V011);
		};

		partiallyOccludedTests[DOWN | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V000, V100, V101, V001)
					|| raster.isQuadPartiallyOccluded(V101, V100, V110, V111);
		};

		partiallyOccludedTests[DOWN | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			return raster.isQuadPartiallyOccluded(V000, V100, V101, V001)
					|| raster.isQuadPartiallyOccluded(V000, V001, V011, V010);
		};

		partiallyOccludedTests[DOWN | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyOccluded(V000, V100, V101, V001)
					|| raster.isQuadPartiallyOccluded(V100, V000, V010, V110);
		};

		partiallyOccludedTests[DOWN | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V000, V100, V101, V001)
					|| raster.isQuadPartiallyOccluded(V001, V101, V111, V011);
		};

		partiallyOccludedTests[NORTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V100, V000, V010, V110)
					|| raster.isQuadPartiallyOccluded(V101, V100, V110, V111);
		};

		partiallyOccludedTests[NORTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyOccluded(V100, V000, V010, V110)
					|| raster.isQuadPartiallyOccluded(V000, V001, V011, V010);
		};

		partiallyOccludedTests[SOUTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V001, V101, V111, V011)
					|| raster.isQuadPartiallyOccluded(V101, V100, V110, V111);
		};

		partiallyOccludedTests[SOUTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V001, V101, V111, V011)
					|| raster.isQuadPartiallyOccluded(V000, V001, V011, V010);
		};

		// NB: When three faces are visible, omit nearest vertex and draw two quads instead of three.

		partiallyOccludedTests[UP | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V011, V111, V101, V100)
					|| raster.isQuadPartiallyOccluded(V100, V000, V010, V011);
		};

		partiallyOccludedTests[UP | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V111, V110, V100, V000)
					|| raster.isQuadPartiallyOccluded(V000, V001, V011, V111);
		};

		partiallyOccludedTests[UP | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyOccluded(V010, V011, V001, V101)
					|| raster.isQuadPartiallyOccluded(V101, V100, V110, V010);
		};

		partiallyOccludedTests[UP | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V110, V010, V000, V001)
					|| raster.isQuadPartiallyOccluded(V001, V101, V111, V110);
		};

		partiallyOccludedTests[DOWN | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V001, V000, V010, V110)
					|| raster.isQuadPartiallyOccluded(V110, V111, V101, V001);
		};

		partiallyOccludedTests[DOWN | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.isQuadPartiallyOccluded(V101, V001, V011, V010)
					|| raster.isQuadPartiallyOccluded(V010, V110, V100, V101);
		};

		partiallyOccludedTests[DOWN | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V000, V100, V110, V111)
					|| raster.isQuadPartiallyOccluded(V111, V011, V001, V000);
		};

		partiallyOccludedTests[DOWN | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.isQuadPartiallyOccluded(V100, V101, V111, V011)
					|| raster.isQuadPartiallyOccluded(V011, V010, V000, V100);
		};

		////

		boxDraws[0] = (x0, y0, z0, x1, y1, z1) -> {
			// NOOP
		};

		boxDraws[UP] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V110, V010, V011, V111);
		};

		boxDraws[DOWN] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.drawQuad(V000, V100, V101, V001);
		};

		boxDraws[EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V101, V100, V110, V111);
		};

		boxDraws[WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.drawQuad(V000, V001, V011, V010);
		};

		boxDraws[NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V100, V000, V010, V110);
		};

		boxDraws[SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V001, V101, V111, V011);
		};

		// NB: Split across two quads to give more evenly-sized test regions vs potentially one big and one very small
		boxDraws[UP | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V110, V010, V011, V111);
			raster.drawQuad(V101, V100, V110, V111);
		};

		boxDraws[UP | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V110, V010, V011, V111);
			raster.drawQuad(V000, V001, V011, V010);
		};

		boxDraws[UP | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V110, V010, V011, V111);
			raster.drawQuad(V100, V000, V010, V110);
		};

		boxDraws[UP | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V110, V010, V011, V111);
			raster.drawQuad(V001, V101, V111, V011);
		};

		boxDraws[DOWN | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V000, V100, V101, V001);
			raster.drawQuad(V101, V100, V110, V111);
		};

		boxDraws[DOWN | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.drawQuad(V000, V100, V101, V001);
			raster.drawQuad(V000, V001, V011, V010);
		};

		boxDraws[DOWN | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V000, V100, V101, V001);
			raster.drawQuad(V100, V000, V010, V110);
		};

		boxDraws[DOWN | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V000, V100, V101, V001);
			raster.drawQuad(V001, V101, V111, V011);
		};

		boxDraws[NORTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V100, V000, V010, V110);
			raster.drawQuad(V101, V100, V110, V111);
		};

		boxDraws[NORTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V100, V000, V010, V110);
			raster.drawQuad(V000, V001, V011, V010);
		};

		boxDraws[SOUTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V001, V101, V111, V011);
			raster.drawQuad(V101, V100, V110, V111);
		};

		boxDraws[SOUTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V001, V101, V111, V011);
			raster.drawQuad(V000, V001, V011, V010);
		};

		// NB: When three faces are visible, omit nearest vertex and draw two quads instead of three.

		boxDraws[UP | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V011, V111, V101, V100);
			raster.drawQuad(V100, V000, V010, V011);
		};

		boxDraws[UP | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V111, V110, V100, V000);
			raster.drawQuad(V000, V001, V011, V111);
		};

		boxDraws[UP | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V010, V011, V001, V101);
			raster.drawQuad(V101, V100, V110, V010);
		};

		boxDraws[UP | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V110, V010, V000, V001);
			raster.drawQuad(V001, V101, V111, V110);
		};

		boxDraws[DOWN | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V001, V000, V010, V110);
			raster.drawQuad(V110, V111, V101, V001);
		};

		boxDraws[DOWN | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V101, V001, V011, V010);
			raster.drawQuad(V010, V110, V100, V101);
		};

		boxDraws[DOWN | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V000, V100, V110, V111);
			raster.drawQuad(V111, V011, V001, V000);
		};

		boxDraws[DOWN | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V100, V101, V111, V011);
			raster.drawQuad(V011, V010, V000, V100);
		};
	}

	@Override
	public final String toString() {
		return String.format("OcclusionVersion:%d  viewX:%d  viewY:%d  viewZ:%d  offsetX:%d  offsetY:%d  offsetZ:%d viewVersion:%d  regionVersion:%d  forceRedraw:%b  matrix:%s",
				occlusionVersion, viewX, viewY, viewZ, offsetX, offsetY, offsetZ, viewVersion, regionVersion, forceRedraw, raster.mvpMatrix.toString());
	}

	public void copyFrom(BoxOccluder source) {
		baseMvpMatrix.copyFrom(source.baseMvpMatrix);
		raster.copyFrom(source.raster);
		viewX = source.viewX;
		viewY = source.viewY;
		viewZ = source.viewZ;

		offsetX = source.offsetX;
		offsetY = source.offsetY;
		offsetZ = source.offsetZ;

		occlusionRange = source.occlusionRange;

		viewVersion = source.viewVersion;
		regionVersion = source.regionVersion;
		occlusionVersion = source.occlusionVersion;
		maxSquaredChunkDistance = source.maxSquaredChunkDistance;

		forceRedraw = source.forceRedraw;
	}

	/**
	 * Incremented each time the occluder is cleared and redrawn.
	 * Previously tested regions can reuse test results if their version matches.
	 * However, they must still be drawn (if visible) if indicated by {@link #clearSceneIfNeeded(int, int)}.
	 */
	public final int occlusionVersion() {
		return occlusionVersion;
	}

	/**
	 * For perspective occluders, controls if near occluders are drawn.
	 * Has no effect on testing.  Meant to reduce flickering and gaps
	 * when the camera is moving around nearby terrain. Defaults to true
	 * and setting persists until it is changed again.
	 */
	public void drawNearOccluders(boolean val) {
		drawNearOccluders = val;
	}

	/** Current buffer width in pixels. */
	public final int bufferWidth() {
		return raster.pixelWidth;
	}

	/** Current buffer height in pixels. */
	public final int bufferHeight() {
		return raster.pixelHeight;
	}

	/**
	 * Changes the buffer size; call before {@link #prepareScene} since the buffer is discarded.
	 * @param tileWidth tiles across, see {@link Constants#DEFAULT_SIZE_TILES}
	 * @param tileHeight tiles down, likewise
	 */
	public final void resizeBuffer(int tileWidth, int tileHeight) {
		if (tileWidth != raster.tileWidth() || tileHeight != raster.tileHeight()) {
			raster.resize(tileWidth, tileHeight);
			forceRedraw = true;
		}
	}

	/**
	 * Force update to new version.
	 */
	public final void invalidate() {
		forceRedraw = true;
	}

	public final void prepareRegion(int originX, int originY, int originZ, int occlusionRange, int squaredChunkDistance) {
		this.occlusionRange = occlusionRange;
		regionSquaredChunkDist = squaredChunkDistance;

		// PERF: could perhaps reuse CameraRelativeCenter values in BuildRenderRegion that are used by Frustum
		offsetX = (int) (((long) originX << CAMERA_PRECISION_BITS) - viewX);
		offsetY = (int) (((long) originY << CAMERA_PRECISION_BITS) - viewY);
		offsetZ = (int) (((long) originZ << CAMERA_PRECISION_BITS) - viewZ);

		raster.setRegionOffset(offsetX, offsetY, offsetZ);
	}

	/**
	 * Screen tile bounds of a world-space box for the current scene, as {@link AbstractRasterizer#boxTileBounds}
	 * packs them. Does not disturb the region prepared by {@link #prepareRegion}.
	 */
	public final long boxTileBounds(int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
		final float scale = 1f / CAMERA_PRECISION_UNITY;
		final float fx = (int) (((long) originX << CAMERA_PRECISION_BITS) - viewX) * scale;
		final float fy = (int) (((long) originY << CAMERA_PRECISION_BITS) - viewY) * scale;
		final float fz = (int) (((long) originZ << CAMERA_PRECISION_BITS) - viewZ) * scale;
		return raster.boxTileBounds(fx, fy, fz, sizeX, sizeY, sizeZ);
	}

	/** Writes the clip-space x, y and w of a world-space point for the current scene; see {@link AbstractRasterizer#clipPoint}. */
	public final void clipPoint(int x, int y, int z, float[] out, int offset) {
		final float scale = 1f / CAMERA_PRECISION_UNITY;
		final float fx = (int) (((long) x << CAMERA_PRECISION_BITS) - viewX) * scale;
		final float fy = (int) (((long) y << CAMERA_PRECISION_BITS) - viewY) * scale;
		final float fz = (int) (((long) z << CAMERA_PRECISION_BITS) - viewZ) * scale;
		raster.clipPoint(fx, fy, fz, out, offset);
	}

	/** See {@link AbstractRasterizer#isSectionCenterOnScreen}. */
	public final boolean isSectionCenterOnScreen(float[] base, int offset, int i, int j, int k) {
		return raster.isSectionCenterOnScreen(base, offset, i, j, k);
	}

	/** See {@link AbstractRasterizer#touchedVersion}. */
	public final int touchedVersion() {
		return raster.touchedVersion();
	}

	/** See {@link AbstractRasterizer#isTileBoxUntouched}. */
	public final boolean isTileBoxUntouched(int packedTileBounds) {
		return raster.isTileBoxUntouched(packedTileBounds);
	}

	/**
	 * Check if needs redrawn and prep for redraw if so.
	 * When false, regions should be drawn only if their occluder version is not current.
	 */
	public final boolean prepareScene(int viewVersion, double cameraX, double cameraY, double cameraZ, Consumer<Matrix4L> modelMatrixSetter, Consumer<Matrix4L> projectionMatrixSetter) {
		if (this.viewVersion != viewVersion) {
			final Matrix4L baseMvpMatrix = this.baseMvpMatrix;
			final Matrix4L tempMatrix = raster.mvpMatrix;

			baseMvpMatrix.loadIdentity();

			projectionMatrixSetter.accept(tempMatrix);
			baseMvpMatrix.multiply(tempMatrix);

			modelMatrixSetter.accept(tempMatrix);
			baseMvpMatrix.multiply(tempMatrix);

			viewX = Math.round(cameraX * CAMERA_PRECISION_UNITY);
			viewY = Math.round(cameraY * CAMERA_PRECISION_UNITY);
			viewZ = Math.round(cameraZ * CAMERA_PRECISION_UNITY);

			raster.setMatrix(baseMvpMatrix);
		}

		if (forceRedraw || this.viewVersion != viewVersion) {
			this.viewVersion = viewVersion;
			raster.clear();
			forceRedraw = false;
			hasNearOccluders = false;
			maxSquaredChunkDistance = 0;
			++occlusionVersion;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * True if occlusion includes geometry within the near region.
	 * When true, simple movement distance test isn't sufficient for knowing if redraw is needed.
	 */
	public final boolean hasNearOccluders() {
		return hasNearOccluders;
	}

	//final MicroTimer timer = new MicroTimer("boxTests.apply", 500000);

	public abstract boolean isBoxVisible(int packedBox, int fuzz);

	/**
	 * Does not rely on winding order but instead the distance from
	 * plane with known facing to camera position.
	 *
	 * <p>Not suitable for orthographic perspective.
	 */
	protected final boolean isBoxVisibleFromPerspective(int packedBox, int fuzz) {
		final int x0 = PackedBox.x0(packedBox) - fuzz;
		final int y0 = PackedBox.y0(packedBox) - fuzz;
		final int z0 = PackedBox.z0(packedBox) - fuzz;
		final int x1 = PackedBox.x1(packedBox) + fuzz;
		final int y1 = PackedBox.y1(packedBox) + fuzz;
		final int z1 = PackedBox.z1(packedBox) + fuzz;

		final int offsetX = this.offsetX;
		final int offsetY = this.offsetY;
		final int offsetZ = this.offsetZ;

		int outcome = 0;

		// if camera below top face can't be seen
		if (offsetY < -(y1 << CAMERA_PRECISION_BITS)) {
			outcome |= UP;
		} else if (offsetY > -(y0 << CAMERA_PRECISION_BITS)) {
			outcome |= DOWN;
		}

		if (offsetX < -(x1 << CAMERA_PRECISION_BITS)) {
			outcome |= EAST;
		} else if (offsetX > -(x0 << CAMERA_PRECISION_BITS)) {
			outcome |= WEST;
		}

		if (offsetZ < -(z1 << CAMERA_PRECISION_BITS)) {
			outcome |= SOUTH;
		} else if (offsetZ > -(z0 << CAMERA_PRECISION_BITS)) {
			outcome |= NORTH;
		}

		//timer.start();
		//final boolean result = partiallyClearTests[outcome].apply(x0, y0, z0, x1, y1, z1);
		//timer.stop(false);

		//final int size = (raster.maxPixelY - raster.minPixelY) * (raster.maxPixelX - raster.minPixelX);

		// Small regions ~ 20% of elapsed time, generally fast
		//timer.stop(size <= 576);

		// Very large regions are expensive but rare < 5% of elapsed time
		//timer.stop(size > 500000);

		// Small to moderate are 80% of elapsed time, doesn't seem to be much variation in time
		//timer.stop(size < 100000);

		final int[] silhouette = SILHOUETTES[outcome];

		if (silhouette != null && raster.setupBoxVertices(silhouette, x0, y0, z0, x1, y1, z1)) {
			return raster.isAnyVertexPixelClear(silhouette, silhouette.length)
					|| raster.isPolygonPartiallyClear(silhouette, silhouette.length);
		}

		return partiallyClearTests[outcome].apply(x0, y0, z0, x1, y1, z1);
	}

	/**
	 * Cheap sufficient test: the centre of a convex box projects inside the box's silhouette, so a clear pixel
	 * there proves the box visible with one transform. False only means the full test is still needed.
	 */
	public final boolean isBoxCenterClear(int packedBox) {
		final float cx = (PackedBox.x0(packedBox) + PackedBox.x1(packedBox)) * 0.5f;
		final float cy = (PackedBox.y0(packedBox) + PackedBox.y1(packedBox)) * 0.5f;
		final float cz = (PackedBox.z0(packedBox) + PackedBox.z1(packedBox)) * 0.5f;
		return raster.isPointClear(cx, cy, cz);
	}

	public final boolean isEmptyRegionVisible(int originX, int originY, int originZ, int fuzz) {
		prepareRegion(originX, originY, originZ, 0, 0);
		return isBoxVisible(PackedBox.FULL_BOX, fuzz);
	}

	/**
	 * Does not rely on winding order but instead the distance from
	 * plane with known facing to camera position.
	 */
	public abstract void occludeBox(int packedBox);

	/**
	 * Does not rely on winding order but instead the distance from
	 * plane with known facing to camera position.
	 *
	 * <p>Will not be suitable for orthographic projection.
	 */
	protected void occludeFromPerspective(int packedBox) {
		final int x0 = PackedBox.x0(packedBox);
		final int y0 = PackedBox.y0(packedBox);
		final int z0 = PackedBox.z0(packedBox);
		final int x1 = PackedBox.x1(packedBox);
		final int y1 = PackedBox.y1(packedBox);
		final int z1 = PackedBox.z1(packedBox);

		final int offsetX = this.offsetX;
		final int offsetY = this.offsetY;
		final int offsetZ = this.offsetZ;

		int outcome = 0;

		boolean hasNear = true;

		final int top = (y1 << CAMERA_PRECISION_BITS) + offsetY;

		// NB: entirely possible for neither top or bottom to be visible.
		// This happens when camera is between them.

		if (top < 0) {
			// camera above top face
			outcome |= UP;
			hasNear &= top > -NEAR_RANGE;
		} else {
			final int bottom = (y0 << CAMERA_PRECISION_BITS) + offsetY;

			if (bottom > 0) {
				// camera below bottom face
				outcome |= DOWN;
				hasNear &= bottom < NEAR_RANGE;
			}
		}

		final int east = (x1 << CAMERA_PRECISION_BITS) + offsetX;

		if (east < 0) {
			outcome |= EAST;
			hasNear &= east > -NEAR_RANGE;
		} else {
			final int west = (x0 << CAMERA_PRECISION_BITS) + offsetX;

			if (west > 0) {
				outcome |= WEST;
				hasNear &= west < NEAR_RANGE;
			}
		}

		final int south = (z1 << CAMERA_PRECISION_BITS) + offsetZ;

		if (south < 0) {
			outcome |= SOUTH;
			hasNear &= south > -NEAR_RANGE;
		} else {
			final int north = (z0 << CAMERA_PRECISION_BITS) + offsetZ;

			if (north > 0) {
				outcome |= NORTH;
				hasNear &= north < NEAR_RANGE;
			}
		}

		if (hasNear) {
			if (drawNearOccluders) {
				hasNearOccluders |= hasNear;
			} else {
				return;
			}
		}

		final int[] silhouette = SILHOUETTES[outcome];

		if (silhouette != null && raster.setupBoxVertices(silhouette, x0, y0, z0, x1, y1, z1)) {
			raster.drawPolygon(silhouette, silhouette.length);
		} else {
			// some vertex needs near clipping, which only the quad path handles
			boxDraws[outcome].apply(x0, y0, z0, x1, y1, z1);
		}
	}

	public final void occlude(int[] visData) {
		final int occlusionRange = this.occlusionRange;
		final int limit = visData.length;

		if (limit > 1) {
			boolean updateDist = false;

			for (int i = 1; i < limit; i++) {
				final int box = visData[i];

				if (occlusionRange > PackedBox.range(box)) {
					break;
				}

				updateDist = true;
				if (AbstractRasterizer.STATS) {
					switch (PackedBox.range(box)) {
						case PackedBox.RANGE_NEAR -> AbstractRasterizer.STAT_D_RANGE0++;
						case PackedBox.RANGE_MID -> AbstractRasterizer.STAT_D_RANGE1++;
						case PackedBox.RANGE_FAR -> AbstractRasterizer.STAT_D_RANGE2++;
						default -> AbstractRasterizer.STAT_D_RANGE3++;
					}
				}
				occludeBox(box);
			}

			if (updateDist && maxSquaredChunkDistance < regionSquaredChunkDist) {
				maxSquaredChunkDistance = regionSquaredChunkDist;
			}
		}
	}

	@FunctionalInterface
	protected interface BoxTest {
		boolean apply(int x0, int y0, int z0, int x1, int y1, int z1);
	}

	@FunctionalInterface
	protected interface BoxDraw {
		void apply(int x0, int y0, int z0, int x1, int y1, int z1);
	}

	public final int maxSquaredChunkDistance() {
		return maxSquaredChunkDistance;
	}
}
