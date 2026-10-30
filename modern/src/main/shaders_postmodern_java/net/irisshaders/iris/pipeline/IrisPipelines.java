package net.irisshaders.iris.pipeline;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pathways.HorizonRenderer;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.neoforge.client.NeoForgeRenderPipelines;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The gbuffer program each render pipeline draws with in the main and shadow passes. A pipeline vanilla doesn't
 * define, such as a mod's, draws with the programs of the vanilla pipeline it most closely matches in shaders, vertex
 * format, defines and color target.
 */
public final class IrisPipelines {
	private static final Function<IrisRenderingPipeline, ShaderKey> NONE = pipeline -> null;

	private static final int NO_MATCH = Integer.MAX_VALUE;

	// Ordered by assignment, which decides between vanilla pipelines that match equally well.
	private static final Map<RenderPipeline, Function<IrisRenderingPipeline, ShaderKey>> MAIN = new Reference2ObjectLinkedOpenHashMap<>();
	private static final Map<RenderPipeline, Function<IrisRenderingPipeline, ShaderKey>> SHADOW = new Reference2ObjectOpenHashMap<>();
	private static final Set<RenderPipeline> VANILLA_PIPELINES = findVanillaPipelines();
	// Each pipeline drawn with, and the one whose assignments it takes.
	private static final Map<RenderPipeline, RenderPipeline> EQUIVALENTS = new Reference2ObjectOpenHashMap<>();

	static {
		// Terrain is drawn by Celeritas, but these keep the assignments complete for pipelines that copy them.
		assign(RenderPipelines.SOLID_TERRAIN, p -> ShaderKey.TERRAIN_SOLID);
		assign(RenderPipelines.CUTOUT_TERRAIN, p -> ShaderKey.TERRAIN_CUTOUT);
		assign(RenderPipelines.TRANSLUCENT_TERRAIN, p -> ShaderKey.TERRAIN_TRANSLUCENT);
		// Vanilla only draws moving blocks, such as pistons' and falling blocks', with the block pipelines.
		assign(RenderPipelines.SOLID_BLOCK, p -> ShaderKey.MOVING_BLOCK);
		assign(RenderPipelines.CUTOUT_BLOCK, p -> ShaderKey.MOVING_BLOCK);
		assign(RenderPipelines.TRANSLUCENT_BLOCK, p -> ShaderKey.MOVING_BLOCK);
		assign(RenderPipelines.WORLD_BORDER, p -> ShaderKey.TEXTURED);
		assign(RenderPipelines.ENTITY_CUTOUT, IrisPipelines::cutout);
		assign(RenderPipelines.ENTITY_CUTOUT_CULL, IrisPipelines::cutout);
		assign(RenderPipelines.ENTITY_CUTOUT_DISSOLVE, IrisPipelines::cutout);
		assign(RenderPipelines.ENTITY_TRANSLUCENT_CULL, IrisPipelines::translucent);
		assign(RenderPipelines.ITEM_TRANSLUCENT, IrisPipelines::translucent);
		assign(RenderPipelines.ITEM_CUTOUT, IrisPipelines::cutout);
		assign(RenderPipelines.ENTITY_TRANSLUCENT, IrisPipelines::translucent);
		assign(RenderPipelines.ENTITY_SHADOW, IrisPipelines::translucent);
		assign(RenderPipelines.LINES, p -> ShaderKey.LINES);
		assign(RenderPipelines.LINES_TRANSLUCENT, p -> ShaderKey.LINES);
		assign(RenderPipelines.SECONDARY_BLOCK_OUTLINE, p -> ShaderKey.LINES);
		assign(RenderPipelines.STARS, p -> ShaderKey.SKY_BASIC);
		assign(RenderPipelines.SUNRISE_SUNSET, p -> ShaderKey.SKY_BASIC_COLOR);
		assign(RenderPipelines.SKY, p -> ShaderKey.SKY_BASIC);
		assign(RenderPipelines.CELESTIAL, p -> ShaderKey.SKY_TEXTURED);
		assign(RenderPipelines.OPAQUE_PARTICLE, p -> ShaderKey.PARTICLES);
		assign(RenderPipelines.TRANSLUCENT_PARTICLE, p -> ShaderKey.PARTICLES_TRANS);
		assign(RenderPipelines.WATER_MASK, p -> ShaderKey.BASIC);
		assign(RenderPipelines.GLINT, p -> ShaderKey.GLINT);
		assign(RenderPipelines.ARMOR_CUTOUT_NO_CULL, IrisPipelines::cutout);
		assign(RenderPipelines.EYES, p -> ShaderKey.ENTITIES_EYES);
		assign(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE, p -> ShaderKey.ENTITIES_EYES_TRANS);
		assign(RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL, IrisPipelines::cutout);
		assign(RenderPipelines.ARMOR_TRANSLUCENT, IrisPipelines::translucent);
		assign(RenderPipelines.BREEZE_WIND, IrisPipelines::translucent);
		assign(RenderPipelines.ENTITY_SOLID, IrisPipelines::solid);
		assign(RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD, IrisPipelines::solid);
		assign(RenderPipelines.END_GATEWAY, p -> ShaderKey.BLOCK_ENTITY);
		assign(RenderPipelines.ENERGY_SWIRL, p -> ShaderKey.ENTITIES_CUTOUT);
		assign(RenderPipelines.END_CRYSTAL_BEAM, p -> ShaderKey.ENTITIES_CUTOUT);
		assign(RenderPipelines.ENTITY_CUTOUT_Z_OFFSET, p -> ShaderKey.ENTITIES_CUTOUT);
		assign(RenderPipelines.LIGHTNING, p -> ShaderKey.LIGHTNING);
		assign(RenderPipelines.DRAGON_RAYS, p -> ShaderKey.LIGHTNING);
		assign(RenderPipelines.DRAGON_RAYS_DEPTH, p -> ShaderKey.LIGHTNING);
		assign(RenderPipelines.BEACON_BEAM_OPAQUE, p -> ShaderKey.BEACON);
		assign(RenderPipelines.BEACON_BEAM_TRANSLUCENT, p -> ShaderKey.BEACON);
		assign(RenderPipelines.END_PORTAL, p -> ShaderKey.BLOCK_ENTITY);
		assign(RenderPipelines.END_SKY, p -> ShaderKey.SKY_TEXTURED);
		assign(RenderPipelines.WEATHER_DEPTH_WRITE, p -> ShaderKey.WEATHER);
		assign(RenderPipelines.WEATHER_NO_DEPTH_WRITE, p -> ShaderKey.WEATHER);
		assign(RenderPipelines.TEXT, IrisPipelines::text);
		assign(RenderPipelines.TEXT_POLYGON_OFFSET, IrisPipelines::text);
		assign(RenderPipelines.TEXT_SEE_THROUGH, IrisPipelines::text);
		assign(RenderPipelines.TEXT_INTENSITY_SEE_THROUGH, IrisPipelines::textIntensity);
		assign(RenderPipelines.TEXT_BACKGROUND, p -> ShaderKey.TEXT_BG);
		assign(RenderPipelines.TEXT_BACKGROUND_SEE_THROUGH, p -> ShaderKey.TEXT_BG);
		assign(RenderPipelines.TEXT_INTENSITY, IrisPipelines::textIntensity);
		assign(RenderPipelines.CRUMBLING, p -> ShaderKey.CRUMBLING);
		assign(RenderPipelines.LEASH, p -> ShaderKey.LEASH);
		assign(HorizonRenderer.PIPELINE, p -> ShaderKey.SKY_BASIC);
		assign(RenderPipelines.CLOUDS, p -> ShaderKey.CLOUDS);
		assign(RenderPipelines.FLAT_CLOUDS, p -> ShaderKey.CLOUDS);
		assign(RenderPipelines.BANNER_PATTERN, IrisPipelines::translucent);

		// NeoForge's unlit item models use their own vertex shader, so no vanilla pipeline matches them.
		assign(NeoForgeRenderPipelines.ITEM_CUTOUT_UNLIT, IrisPipelines::cutout);
		assign(NeoForgeRenderPipelines.ITEM_TRANSLUCENT_UNLIT, IrisPipelines::translucent);

		for (RenderPipeline pipeline : new RenderPipeline[]{RenderPipelines.SOLID_TERRAIN, RenderPipelines.CUTOUT_TERRAIN,
			RenderPipelines.SOLID_BLOCK, RenderPipelines.CUTOUT_BLOCK}) {
			assignShadow(pipeline, p -> ShaderKey.SHADOW_TERRAIN_CUTOUT);
		}

		assignShadow(RenderPipelines.TRANSLUCENT_TERRAIN, p -> ShaderKey.SHADOW_TRANSLUCENT);
		assignShadow(RenderPipelines.TRANSLUCENT_BLOCK, p -> ShaderKey.SHADOW_TRANSLUCENT);

		for (RenderPipeline pipeline : new RenderPipeline[]{RenderPipelines.ENTITY_CUTOUT, RenderPipelines.ENTITY_CUTOUT_CULL,
			RenderPipelines.ENTITY_CUTOUT_Z_OFFSET, RenderPipelines.ENTITY_CUTOUT_DISSOLVE, RenderPipelines.ENTITY_SOLID,
			RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD, RenderPipelines.ENTITY_TRANSLUCENT, RenderPipelines.ENTITY_TRANSLUCENT_CULL,
			RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE, RenderPipelines.ARMOR_CUTOUT_NO_CULL, RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL,
			RenderPipelines.ARMOR_TRANSLUCENT, RenderPipelines.ITEM_CUTOUT, RenderPipelines.ITEM_TRANSLUCENT, RenderPipelines.END_CRYSTAL_BEAM,
			RenderPipelines.BREEZE_WIND, RenderPipelines.EYES, RenderPipelines.ENERGY_SWIRL, RenderPipelines.BANNER_PATTERN,
			RenderPipelines.GLINT, NeoForgeRenderPipelines.ITEM_CUTOUT_UNLIT, NeoForgeRenderPipelines.ITEM_TRANSLUCENT_UNLIT}) {
			assignShadow(pipeline, p -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
		}

		assignShadow(RenderPipelines.CRUMBLING, p -> ShaderKey.SHADOW_TEX);
		assignShadow(RenderPipelines.WATER_MASK, p -> ShaderKey.SHADOW_BASIC);
		assignShadow(RenderPipelines.OPAQUE_PARTICLE, p -> ShaderKey.SHADOW_PARTICLES);
		assignShadow(RenderPipelines.TRANSLUCENT_PARTICLE, p -> ShaderKey.SHADOW_PARTICLES);
		assignShadow(RenderPipelines.WEATHER_DEPTH_WRITE, p -> ShaderKey.SHADOW_PARTICLES);
		assignShadow(RenderPipelines.WEATHER_NO_DEPTH_WRITE, p -> ShaderKey.SHADOW_PARTICLES);
		assignShadow(RenderPipelines.LINES, p -> ShaderKey.SHADOW_LINES);
		assignShadow(RenderPipelines.SECONDARY_BLOCK_OUTLINE, p -> ShaderKey.SHADOW_LINES);
		assignShadow(RenderPipelines.LEASH, p -> ShaderKey.SHADOW_LEASH);
		assignShadow(RenderPipelines.TEXT, p -> ShaderKey.SHADOW_TEXT);
		assignShadow(RenderPipelines.TEXT_POLYGON_OFFSET, p -> ShaderKey.SHADOW_TEXT);
		assignShadow(RenderPipelines.TEXT_SEE_THROUGH, p -> ShaderKey.SHADOW_TEXT);
		assignShadow(RenderPipelines.TEXT_INTENSITY, p -> ShaderKey.SHADOW_TEXT_INTENSITY);
		assignShadow(RenderPipelines.TEXT_INTENSITY_SEE_THROUGH, p -> ShaderKey.SHADOW_TEXT_INTENSITY);
		assignShadow(RenderPipelines.TEXT_BACKGROUND, p -> ShaderKey.SHADOW_TEXT_BG);
		assignShadow(RenderPipelines.TEXT_BACKGROUND_SEE_THROUGH, p -> ShaderKey.SHADOW_TEXT_BG);
		assignShadow(RenderPipelines.BEACON_BEAM_OPAQUE, p -> ShaderKey.SHADOW_BEACON_BEAM);
		assignShadow(RenderPipelines.BEACON_BEAM_TRANSLUCENT, p -> ShaderKey.SHADOW_BEACON_BEAM);
		assignShadow(RenderPipelines.END_PORTAL, p -> ShaderKey.SHADOW_BLOCK);
		assignShadow(RenderPipelines.END_GATEWAY, p -> ShaderKey.SHADOW_BLOCK);
		assignShadow(RenderPipelines.LIGHTNING, p -> ShaderKey.SHADOW_LIGHTNING);
		assignShadow(RenderPipelines.DRAGON_RAYS, p -> ShaderKey.SHADOW_LIGHTNING);
		assignShadow(RenderPipelines.DRAGON_RAYS_DEPTH, p -> ShaderKey.SHADOW_LIGHTNING);
	}

	private IrisPipelines() {
	}

	private static Set<RenderPipeline> findVanillaPipelines() {
		Set<RenderPipeline> pipelines = new ReferenceOpenHashSet<>();

		for (Field field : RenderPipelines.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == RenderPipeline.class) {
				try {
					pipelines.add((RenderPipeline) field.get(null));
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
		}

		return pipelines;
	}

	private static void assign(RenderPipeline pipeline, Function<IrisRenderingPipeline, ShaderKey> key) {
		MAIN.put(pipeline, key);
	}

	private static void assignShadow(RenderPipeline pipeline, Function<IrisRenderingPipeline, ShaderKey> key) {
		SHADOW.put(pipeline, key);
	}

	/**
	 * Returns the key of the program the pipeline draws with, or null if it draws with its own.
	 */
	@Nullable
	public static ShaderKey getKey(IrisRenderingPipeline pipeline, RenderPipeline renderPipeline) {
		RenderPipeline equivalent = EQUIVALENTS.computeIfAbsent(renderPipeline, IrisPipelines::findEquivalent);
		var keys = ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? SHADOW : MAIN;

		return keys.getOrDefault(equivalent, NONE).apply(pipeline);
	}

	private static RenderPipeline findEquivalent(RenderPipeline renderPipeline) {
		// Mods often name their pipelines in the minecraft namespace, so vanilla's are told apart by identity.
		if (MAIN.containsKey(renderPipeline) || SHADOW.containsKey(renderPipeline) || VANILLA_PIPELINES.contains(renderPipeline)) {
			return renderPipeline;
		}

		RenderPipeline equivalent = renderPipeline;
		int bestLevel = NO_MATCH;

		// Where vanilla pipelines match equally well, the first assigned stands for all of them.
		for (RenderPipeline vanilla : MAIN.keySet()) {
			int level = matchLevel(renderPipeline, vanilla);

			if (level < bestLevel) {
				equivalent = vanilla;
				bestLevel = level;

				if (level == 0) {
					break;
				}
			}
		}

		return equivalent;
	}

	/**
	 * How closely a pipeline matches a vanilla one, lower being closer, or {@link #NO_MATCH} if they draw differently.
	 */
	private static int matchLevel(RenderPipeline pipeline, RenderPipeline vanilla) {
		if (!pipeline.getVertexShader().equals(vanilla.getVertexShader())
			|| !pipeline.getFragmentShader().equals(vanilla.getFragmentShader())
			|| !pipeline.getVertexFormat().equals(vanilla.getVertexFormat())
			|| pipeline.getVertexFormatMode() != vanilla.getVertexFormatMode()) {
			return NO_MATCH;
		}

		int level = 0;

		if (!pipeline.getShaderDefines().equals(vanilla.getShaderDefines())) {
			level |= 1;
		}

		// Blend ranks below defines, since programs were chosen by shader alone before 26.1.
		if (!pipeline.getColorTargetState().equals(vanilla.getColorTargetState())) {
			level |= 2;
		}

		return level;
	}

	private static ShaderKey cutout(IrisRenderingPipeline pipeline) {
		if (HandRenderer.INSTANCE.isActive()) {
			return HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE;
		}

		return isBlockEntities(pipeline) ? ShaderKey.BLOCK_ENTITY_DIFFUSE : ShaderKey.ENTITIES_CUTOUT_DIFFUSE;
	}

	private static ShaderKey solid(IrisRenderingPipeline pipeline) {
		if (HandRenderer.INSTANCE.isActive()) {
			return HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT : ShaderKey.HAND_TRANSLUCENT;
		}

		return isBlockEntities(pipeline) ? ShaderKey.BLOCK_ENTITY : ShaderKey.ENTITIES_SOLID;
	}

	private static ShaderKey translucent(IrisRenderingPipeline pipeline) {
		if (HandRenderer.INSTANCE.isActive()) {
			return HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE;
		}

		return isBlockEntities(pipeline) ? ShaderKey.BE_TRANSLUCENT : ShaderKey.ENTITIES_TRANSLUCENT;
	}

	private static ShaderKey text(IrisRenderingPipeline pipeline) {
		if (HandRenderer.INSTANCE.isActive()) {
			return HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_TEXT : ShaderKey.HAND_TEXT_TRANSLUCENT;
		}

		return isBlockEntities(pipeline) ? ShaderKey.TEXT_BE : ShaderKey.TEXT;
	}

	private static ShaderKey textIntensity(IrisRenderingPipeline pipeline) {
		if (HandRenderer.INSTANCE.isActive()) {
			return ShaderKey.HAND_TEXT_INTENSITY;
		}

		return isBlockEntities(pipeline) ? ShaderKey.TEXT_INTENSITY_BE : ShaderKey.TEXT_INTENSITY;
	}

	private static boolean isBlockEntities(IrisRenderingPipeline pipeline) {
		return pipeline.getPhase() == WorldRenderingPhase.BLOCK_ENTITIES;
	}
}
