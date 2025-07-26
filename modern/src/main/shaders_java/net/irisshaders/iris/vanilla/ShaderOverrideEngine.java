package net.irisshaders.iris.vanilla;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ModernShaderKey;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.irisshaders.iris.shadows.ModernShadowRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.embeddedt.embeddium.compat.mc.MCShaderInstance;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

public class ShaderOverrideEngine {
    private static final Map<String, Supplier<MCShaderInstance>> iris$overrides = new Object2ObjectOpenHashMap<>();
    private static final Set<String> missingOverrides = new ObjectOpenHashSet<>();

    @SuppressWarnings("unused") // called from ShaderOverridePatcher injection
    public static @Nullable ShaderInstance wrapGameRendererReturn(@Nullable ShaderInstance shader) {
        if (shader != null && !(shader instanceof ExtendedShader)) {
            var override = getOverride(shader.getName());
            if (override != null) {
                return (ShaderInstance)(Object)override;
            }
        }
        return shader;
    }

    private static @Nullable MCShaderInstance iris$findOverride(ModernShaderKey key) {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline) pipeline).getShaderMap().getShader(key);
        } else {
            return null;
        }
    }

    public static @Nullable MCShaderInstance getOverride(String name) {
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
                return iris$findOverride(ModernShaderKey.SKY_BASIC);
            } else if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_BASIC);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.BASIC);
            } else {
                return null;
            }
        });
        iris$overrides.put("position_color", () -> {
            if (isSky()) {
                return iris$findOverride(ModernShaderKey.SKY_BASIC_COLOR);
            } else if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_BASIC_COLOR);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.BASIC_COLOR);
            } else {
                return null;
            }
        });
        iris$overrides.put("position_tex", () -> {
            if (isSky()) {
                return iris$findOverride(ModernShaderKey.SKY_TEXTURED);
            } else if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_TEX);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.TEXTURED);
            } else {
                return null;
            }
        });
        Supplier<MCShaderInstance> positionTexColor = () -> {
            if (isSky()) {
                return iris$findOverride(ModernShaderKey.SKY_TEXTURED_COLOR);
            } else if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_TEX_COLOR);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.TEXTURED_COLOR);
            } else {
                return null;
            }
        };
        iris$overrides.put("position_tex_color", positionTexColor);
        iris$overrides.put("position_color_tex", positionTexColor);
        iris$overrides.put("particle", () -> {
            if (isPhase(WorldRenderingPhase.RAIN_SNOW)) {
                return iris$findOverride(ModernShaderKey.WEATHER);
            } else if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_PARTICLES);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.PARTICLES);
            } else {
                return null;
            }
        });
        Supplier<MCShaderInstance> cloudsShader = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_CLOUDS);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.CLOUDS);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_clouds", cloudsShader);
        iris$overrides.put("position_tex_color_normal", cloudsShader);
        iris$overrides.put("rendertype_solid", () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_TERRAIN_CUTOUT);
            } else if (isBlockEntities() || isEntities()) {
                return iris$findOverride(ModernShaderKey.MOVING_BLOCK);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.TERRAIN_SOLID);
            } else {
                return null;
            }
        });
        Supplier<MCShaderInstance> cutout = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_TERRAIN_CUTOUT);
            } else if (isBlockEntities() || isEntities()) {
                return iris$findOverride(ModernShaderKey.MOVING_BLOCK);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.TERRAIN_CUTOUT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_cutout", cutout);
        iris$overrides.put("rendertype_cutout_mipped", cutout);
        Supplier<MCShaderInstance> translucentShader = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_TERRAIN_CUTOUT);
            } else if (isBlockEntities() || isEntities()) {
                return iris$findOverride(ModernShaderKey.MOVING_BLOCK);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.TERRAIN_TRANSLUCENT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_translucent", translucentShader);
        iris$overrides.put("rendertype_translucent_no_crumbling", translucentShader);
        iris$overrides.put("rendertype_translucent_moving_block", translucentShader);
        iris$overrides.put("rendertype_tripwire", translucentShader);
        Supplier<MCShaderInstance> entityCutout = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ModernShaderKey.HAND_CUTOUT_DIFFUSE : ModernShaderKey.HAND_WATER_DIFFUSE);
            } else if (isBlockEntities()) {
                return iris$findOverride(ModernShaderKey.BLOCK_ENTITY_DIFFUSE);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.ENTITIES_CUTOUT_DIFFUSE);
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
        Supplier<MCShaderInstance> entityTranslucent = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ModernShaderKey.HAND_CUTOUT_DIFFUSE : ModernShaderKey.HAND_WATER_DIFFUSE);
            } else if (isBlockEntities()) {
                return iris$findOverride(ModernShaderKey.BE_TRANSLUCENT);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.ENTITIES_TRANSLUCENT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_entity_translucent", entityTranslucent);
        iris$overrides.put("rendertype_entity_translucent_cull", entityTranslucent);
        iris$overrides.put("rendertype_item_entity_translucent_cull", entityTranslucent);
        iris$overrides.put("rendertype_breeze_wind", entityTranslucent);
        iris$overrides.put("rendertype_entity_no_outline", entityTranslucent);
        Supplier<MCShaderInstance> energySwirlAndShadow = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ModernShaderKey.HAND_CUTOUT : ModernShaderKey.HAND_TRANSLUCENT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ModernShaderKey.BLOCK_ENTITY);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.ENTITIES_CUTOUT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_energy_swirl", energySwirlAndShadow);
        iris$overrides.put("rendertype_entity_shadow", energySwirlAndShadow);
        Supplier<MCShaderInstance> glint = () -> {
            if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.GLINT);
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
        Supplier<MCShaderInstance> entitySolid = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ModernShaderKey.HAND_CUTOUT_DIFFUSE : ModernShaderKey.HAND_WATER_DIFFUSE);
            } else if (isBlockEntities()) {
                return iris$findOverride(ModernShaderKey.BLOCK_ENTITY_DIFFUSE);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.ENTITIES_SOLID_DIFFUSE);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_entity_solid", entitySolid);
        Supplier<MCShaderInstance> waterMask = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(HandRenderer.INSTANCE.isRenderingSolid() ? ModernShaderKey.HAND_CUTOUT : ModernShaderKey.HAND_TRANSLUCENT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ModernShaderKey.BLOCK_ENTITY);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.ENTITIES_SOLID);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_water_mask", waterMask);
        iris$overrides.put("rendertype_beacon_beam", () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_BEACON_BEAM);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.BEACON);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_entity_alpha", () -> {
            if (!ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.ENTITIES_ALPHA);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_eyes", () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ModernShaderKey.BLOCK_ENTITY);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.ENTITIES_EYES);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_entity_translucent_emissive", () -> {
            if (ModernShadowRenderer.ACTIVE) {
                // TODO: Wrong program
                return iris$findOverride(ModernShaderKey.SHADOW_ENTITIES_CUTOUT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ModernShaderKey.BLOCK_ENTITY);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.ENTITIES_EYES_TRANS);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_leash", () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_LEASH);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.LEASH);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_lightning", () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_LIGHTNING);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.LIGHTNING);
            } else {
                return null;
            }
        });
        iris$overrides.put("rendertype_crumbling", () -> {
            if (shouldOverrideShaders() && !ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.CRUMBLING);
            } else {
                return null;
            }
        });
        Supplier<MCShaderInstance> textShader = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_TEXT);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(ModernShaderKey.HAND_TEXT);
            } else if (isBlockEntities()) {
                return iris$findOverride(ModernShaderKey.TEXT_BE);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.TEXT);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_text", textShader);
        iris$overrides.put("rendertype_text_see_through", textShader);
        iris$overrides.put("position_color_tex_lightmap", textShader);
        Supplier<MCShaderInstance> textBgShader = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_TEXT_BG);
            } else {
                return iris$findOverride(ModernShaderKey.TEXT_BG);
            }
        };
        iris$overrides.put("rendertype_text_background", textBgShader);
        iris$overrides.put("rendertype_text_background_see_through", textBgShader);
        Supplier<MCShaderInstance> textIntensityShader = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_TEXT_INTENSITY);
            } else if (HandRenderer.INSTANCE.isActive()) {
                return iris$findOverride(ModernShaderKey.HAND_TEXT_INTENSITY);
            } else if (isBlockEntities()) {
                return iris$findOverride(ModernShaderKey.TEXT_INTENSITY_BE);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.TEXT_INTENSITY);
            } else {
                return null;
            }
        };
        iris$overrides.put("rendertype_text_intensity", textIntensityShader);
        iris$overrides.put("rendertype_text_intensity_see_through", textIntensityShader);
        Supplier<MCShaderInstance> linesShader = () -> {
            if (ModernShadowRenderer.ACTIVE) {
                return iris$findOverride(ModernShaderKey.SHADOW_LINES);
            } else if (shouldOverrideShaders()) {
                return iris$findOverride(ModernShaderKey.LINES);
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
