package net.irisshaders.iris.vanilla;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

public class ShaderOverrideEngine {
    private static final Map<String, Supplier<ShaderInstance>> iris$overrides = new Object2ObjectOpenHashMap<>();
    private static final Set<String> missingOverrides = new ObjectOpenHashSet<>();

    private static @Nullable ShaderInstance iris$findOverride(ShaderKey key) {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline) pipeline).getShaderMap().getShader(key);
        } else {
            return null;
        }
    }

    public static @Nullable ShaderInstance getOverride(String name) {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

        if (!(pipeline instanceof ShaderRenderingPipeline)) {
            return null;
        }

        var overrideSupplier = iris$overrides.get(name);
        if (overrideSupplier != null) {
            return overrideSupplier.get();
        } else if (missingOverrides.add(name)) {
            IRIS_LOGGER.warn("Missing shader override for '{}'", name);
        }

        return null;
    }

    static {
        iris$overrides.put("position", () -> {
            if (isSky()) {
                return iris$findOverride(ShaderKey.SKY_BASIC);
            } else if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_BASIC);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.BASIC);
            } else {
                return null;
            }
        });
        iris$overrides.put("position_color", () -> {
            if (isSky()) {
                return iris$findOverride(ShaderKey.SKY_BASIC_COLOR);
            } else if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_BASIC_COLOR);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.BASIC_COLOR);
            } else {
                return null;
            }
        });
        iris$overrides.put("position_tex", () -> {
            if (isSky()) {
                return iris$findOverride(ShaderKey.SKY_TEXTURED);
            } else if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_TEX);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.TEXTURED);
            } else {
                return null;
            }
        });
        Supplier<ShaderInstance> positionTexColor = () -> {
            if (isSky()) {
                return iris$findOverride(ShaderKey.SKY_TEXTURED_COLOR);
            } else if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_TEX_COLOR);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.TEXTURED_COLOR);
            } else {
                return null;
            }
        };
        iris$overrides.put("position_tex_color", positionTexColor);
        iris$overrides.put("position_color_tex", positionTexColor);
        iris$overrides.put("particle", () -> {
            if (isPhase(WorldRenderingPhase.RAIN_SNOW)) {
                return iris$findOverride(ShaderKey.WEATHER);
            } else if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_PARTICLES);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.PARTICLES);
            } else {
                return null;
            }
        });
        Supplier<ShaderInstance> cloudsShader = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_CLOUDS);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.CLOUDS);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_clouds", cloudsShader);
        iris$overrides.put("position_tex_color_normal", cloudsShader);
        iris$overrides.put("rendertype_solid", () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_TERRAIN_CUTOUT);
            } else if (isBlockEntities() || isEntities()) {
                return iris$findOverride(ShaderKey.MOVING_BLOCK);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.TERRAIN_SOLID);
            } else {
                return null;
            }
        });
        Supplier<ShaderInstance> cutout = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_TERRAIN_CUTOUT);
            } else if (isBlockEntities() || isEntities()) {
                return iris$findOverride(ShaderKey.MOVING_BLOCK);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.TERRAIN_CUTOUT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_cutout", cutout);
        iris$overrides.put("rendertype_cutout_mipped", cutout);
        Supplier<ShaderInstance> translucentShader = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_TERRAIN_CUTOUT);
            } else if (isBlockEntities() || isEntities()) {
                return iris$findOverride(ShaderKey.MOVING_BLOCK);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.TERRAIN_TRANSLUCENT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_translucent", translucentShader);
        iris$overrides.put("rendertype_translucent_no_crumbling", translucentShader);
        iris$overrides.put("rendertype_translucent_moving_block", translucentShader);
        iris$overrides.put("rendertype_tripwire", translucentShader);
        Supplier<ShaderInstance> entityCutout = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE);
            } else if (isBlockEntities()) {
                return iris$findOverride(ShaderKey.BLOCK_ENTITY_DIFFUSE);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.ENTITIES_CUTOUT_DIFFUSE);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_entity_cutout", entityCutout);
        iris$overrides.put("rendertype_entity_cutout_no_cull", entityCutout);
        iris$overrides.put("rendertype_entity_cutout_no_cull_z_offset", entityCutout);
        iris$overrides.put("rendertype_entity_decal", entityCutout);
        iris$overrides.put("rendertype_entity_smooth_cutout", entityCutout);
        iris$overrides.put("rendertype_armor_cutout_no_cull", entityCutout);
        Supplier<ShaderInstance> entityTranslucent = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE);
            } else if (isBlockEntities()) {
                return iris$findOverride(ShaderKey.BE_TRANSLUCENT);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.ENTITIES_TRANSLUCENT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_entity_translucent", entityTranslucent);
        iris$overrides.put("rendertype_entity_translucent_cull", entityTranslucent);
        iris$overrides.put("rendertype_item_entity_translucent_cull", entityTranslucent);
        iris$overrides.put("rendertype_breeze_wind", entityTranslucent);
        iris$overrides.put("rendertype_entity_no_outline", entityTranslucent);
        Supplier<ShaderInstance> energySwirlAndShadow = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT : ShaderKey.HAND_TRANSLUCENT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ShaderKey.BLOCK_ENTITY);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.ENTITIES_CUTOUT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_energy_swirl", energySwirlAndShadow);
        iris$overrides.put("rendertype_entity_shadow", energySwirlAndShadow);
        Supplier<ShaderInstance> glint = () -> {
            if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.GLINT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_glint", glint);
        iris$overrides.put("rendertype_glint_direct", glint);
        iris$overrides.put("rendertype_glint_translucent", glint);
        iris$overrides.put("rendertype_armor_glint", glint);
        iris$overrides.put("rendertype_entity_glint_direct", glint);
        iris$overrides.put("rendertype_entity_glint", glint);
        iris$overrides.put("rendertype_armor_entity_glint", glint);
        Supplier<ShaderInstance> entitySolid = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE);
            } else if (isBlockEntities()) {
                return iris$findOverride(ShaderKey.BLOCK_ENTITY_DIFFUSE);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.ENTITIES_SOLID_DIFFUSE);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_entity_solid", entitySolid);
        Supplier<ShaderInstance> waterMask = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT : ShaderKey.HAND_TRANSLUCENT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ShaderKey.BLOCK_ENTITY);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.ENTITIES_SOLID);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_water_mask", waterMask);
        iris$overrides.put("rendertype_beacon_beam", () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_BEACON_BEAM);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.BEACON);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_entity_alpha", () -> {
            if (!ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.ENTITIES_ALPHA);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_eyes", () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ShaderKey.BLOCK_ENTITY);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.ENTITIES_EYES);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_entity_translucent_emissive", () -> {
            if (ShadowRenderer.ACTIVE) {
                // TODO: Wrong program
                return iris$findOverride(ShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ShaderKey.BLOCK_ENTITY);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.ENTITIES_EYES_TRANS);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_leash", () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_LEASH);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.LEASH);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_lightning", () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_LIGHTNING);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.LIGHTNING);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_crumbling", () -> {
            if (shouldOverrideShaders() && !ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.CRUMBLING);
            } else {
                return null;
            }
        });
        Supplier<ShaderInstance> textShader = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_TEXT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(ShaderKey.HAND_TEXT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ShaderKey.TEXT_BE);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.TEXT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_text", textShader);
        iris$overrides.put("rendertype_text_see_through", textShader);
        iris$overrides.put("position_color_tex_lightmap", textShader);
        Supplier<ShaderInstance> textBgShader = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_TEXT_BG);
            } else {
                return iris$findOverride(ShaderKey.TEXT_BG);
            }
        };
        iris$overrides.put("rendertype_text_background", textBgShader);
        iris$overrides.put("rendertype_text_background_see_through", textBgShader);
        Supplier<ShaderInstance> textIntensityShader = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_TEXT_INTENSITY);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(ShaderKey.HAND_TEXT_INTENSITY);
            } else if (isBlockEntities()) {
                return iris$findOverride(ShaderKey.TEXT_INTENSITY_BE);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.TEXT_INTENSITY);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_text_intensity", textIntensityShader);
        iris$overrides.put("rendertype_text_intensity_see_through", textIntensityShader);
        Supplier<ShaderInstance> linesShader = () -> {
            if (ShadowRenderer.ACTIVE) {
                return iris$findOverride(ShaderKey.SHADOW_LINES);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ShaderKey.LINES);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_lines", linesShader);
    }


    private static boolean isBlockEntities() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

        return pipeline != null && pipeline.getPhase() == WorldRenderingPhase.BLOCK_ENTITIES;
    }

    private static boolean isEntities() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

        return pipeline != null && pipeline.getPhase() == WorldRenderingPhase.ENTITIES;
    }

    private static boolean isSky() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

        if (pipeline != null) {
            return switch (pipeline.getPhase()) {
                case CUSTOM_SKY, SKY, SUNSET, SUN, STARS, VOID, MOON -> true;
                default -> false;
            };
        } else {
            return false;
        }
    }


    // TODO: getPositionColorLightmapShader

    // TODO: getPositionTexLightmapColorShader

    // NOTE: getRenderTypeOutlineShader should not be overriden.

    // ignored: getRendertypeEndGatewayShader (we replace the end portal rendering for shaders)
    // ignored: getRendertypeEndPortalShader (we replace the end portal rendering for shaders)

    private static boolean isPhase(WorldRenderingPhase phase) {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

        if (pipeline != null) {
            return pipeline.getPhase() == phase;
        } else {
            return false;
        }
    }

    private static boolean shouldOverrideShaders() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline) pipeline).shouldOverrideShaders();
        } else {
            return false;
        }
    }
}
