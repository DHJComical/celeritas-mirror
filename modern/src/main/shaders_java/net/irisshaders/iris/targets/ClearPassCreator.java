package net.irisshaders.iris.targets;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.irisshaders.iris.shaderpack.properties.PackRenderTargetDirectives;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL21C;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

public class ClearPassCreator {
	public static ImmutableList<ClearPass> createClearPasses(RenderTargets renderTargets, boolean fullClear,
															 PackRenderTargetDirectives renderTargetDirectives) {
		final int maxDrawBuffers = GlStateManager._getInteger(GL21C.GL_MAX_DRAW_BUFFERS);

		// Sort buffers by their clear color so we can group up glClear calls.
		Map<Vector2i, Map<ClearPassInformation, IntList>> clearByColor = new HashMap<>();

		renderTargetDirectives.getRenderTargetSettings().forEach((bufferI, settings) -> {
			// unboxed
			final int buffer = bufferI;

			if (fullClear || settings.shouldClear()) {
				Vector4f defaultClearColor;

				if (buffer == 0) {
					// colortex0 is cleared to the fog color (with 1.0 alpha) by default.
					defaultClearColor = null;
				} else if (buffer == 1) {
					// colortex1 is cleared to solid white (with 1.0 alpha) by default.
					defaultClearColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
				} else {
					// all other buffers are cleared to solid black (with 0.0 alpha) by default.
					defaultClearColor = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
				}

				RenderTarget target = renderTargets.get(buffer);
				if (target == null) return;
				Vector4f clearColor = settings.getClearColor().orElse(defaultClearColor);
				clearByColor.computeIfAbsent(new Vector2i(target.getWidth(), target.getHeight()), size -> new HashMap<>()).computeIfAbsent(new ClearPassInformation(clearColor, target.getWidth(), target.getHeight()), color -> new IntArrayList()).add(buffer);
			}
		});

		List<ClearPass> clearPasses = new ArrayList<>();

		clearByColor.forEach((passSize, vector4fIntListMap) -> vector4fIntListMap.forEach((clearInfo, buffers) ->
			addClearPasses(buffers, maxDrawBuffers, renderTargets::isAltUsed,
				(clearBuffers, alt) -> clearPasses.add(new ClearPass(clearInfo.getColor(), clearInfo::getWidth,
					clearInfo::getHeight, renderTargets.createClearFramebuffer(alt, clearBuffers),
					GL21C.GL_COLOR_BUFFER_BIT)))));

		return ImmutableList.copyOf(clearPasses);
	}

	public static ImmutableList<ClearPass> createShadowClearPasses(ShadowRenderTargets renderTargets, boolean fullClear,
																   PackShadowDirectives renderTargetDirectives) {
		if (renderTargets == null) {
			return ImmutableList.of();
		}

		final int maxDrawBuffers = GlStateManager._getInteger(GL21C.GL_MAX_DRAW_BUFFERS);
		final IntSupplier resolution = renderTargets::getResolution;

		// Sort buffers by their clear color so we can group up glClear calls.
		Map<Vector4f, IntList> clearByColor = new HashMap<>();

		for (int i = 0; i < renderTargets.getRenderTargetCount(); i++) {
			if (renderTargets.get(i) != null) {
				// unboxed
				PackShadowDirectives.SamplingSettings settings = renderTargetDirectives.getColorSamplingSettings().get(i);

				if (fullClear || settings.getClear()) {
					Vector4f clearColor = settings.getClearColor();
					clearByColor.computeIfAbsent(clearColor, color -> new IntArrayList()).add(i);
				}
			}
		}

		List<ClearPass> clearPasses = new ArrayList<>();

		clearByColor.forEach((clearColor, buffers) ->
			addClearPasses(buffers, maxDrawBuffers, renderTargets::isAltUsed,
				(clearBuffers, alt) -> clearPasses.add(new ClearPass(clearColor, resolution, resolution,
					renderTargets.createClearFramebuffer(alt, clearBuffers), GL21C.GL_COLOR_BUFFER_BIT))));

		return ImmutableList.copyOf(clearPasses);
	}

	/**
	 * Emits the clear passes for one group of same-colored buffers: one set writing to the alt textures and one
	 * writing to the main textures, each split so that no pass exceeds the GPU's maximum draw buffer count.
	 * <p>
	 * Buffers that are never flipped have no alt texture, so clearing it would just be a full-resolution write into
	 * a texture nothing ever reads. Note that this has to be decided here rather than inside {@code factory}, since
	 * asking for an alt framebuffer is itself what allocates the alt texture.
	 *
	 * @param altUsed whether the given buffer's alt texture is ever read
	 * @param factory builds and records a clear pass over the given buffers, writing to either alt or main
	 */
	private static void addClearPasses(IntList buffers, int maxDrawBuffers, IntPredicate altUsed,
									   ClearPassFactory factory) {
		IntList altBuffers = new IntArrayList();

		for (int buffer : buffers) {
			if (altUsed.test(buffer)) {
				altBuffers.add(buffer);
			}
		}

		forEachChunk(altBuffers, maxDrawBuffers, clearBuffers -> factory.create(clearBuffers, true));
		forEachChunk(buffers, maxDrawBuffers, clearBuffers -> factory.create(clearBuffers, false));
	}

	/**
	 * Splits the buffers into groups of at most {@code maxDrawBuffers}. This allows us to handle having more than 8
	 * buffers with the same clear color on systems with a max draw buffers of 8 (ie, most systems).
	 */
	private static void forEachChunk(IntList buffers, int maxDrawBuffers, Consumer<int[]> consumer) {
		int startIndex = 0;

		while (startIndex < buffers.size()) {
			int[] chunk = new int[Math.min(buffers.size() - startIndex, maxDrawBuffers)];

			for (int i = 0; i < chunk.length; i++) {
				chunk[i] = buffers.getInt(startIndex);
				startIndex++;
			}

			consumer.accept(chunk);
		}
	}

	@FunctionalInterface
	private interface ClearPassFactory {
		void create(int[] clearBuffers, boolean alt);
	}
}
