package net.irisshaders.iris.mixin;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.gl.program.IrisProgramTypes;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	@Shadow
	private boolean renderHand;

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

    @Inject(method = "*()Lnet/minecraft/client/renderer/ShaderInstance;", at = @At("RETURN"), cancellable = true)
    private static void iris$overrideShader(CallbackInfoReturnable<ShaderInstance> cir) {
        var shader = cir.getReturnValue();
        if (shader == null) {
            return;
        }
        if (!(shader instanceof ExtendedShader)) {
            var shaderKey = shader.getName();
            var overrideSupplier = iris$overrides.get(shaderKey);
            if (overrideSupplier != null) {
                var override = overrideSupplier.get();
                if (override != null) {
                    cir.setReturnValue(override);
                }
            } else if (missingOverrides.add(shaderKey)) {
                Iris.logger.warn("Unknown vanilla shader being used: {}", shaderKey);
            }
        }
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void iris$initOverrides(CallbackInfo ci) {
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
        iris$overrides.put("clouds", cloudsShader);
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

	// TODO: getPositionColorLightmapShader

	// TODO: getPositionTexLightmapColorShader

	// NOTE: getRenderTypeOutlineShader should not be overriden.

    @Inject(method = "render", at = @At("HEAD"))
    private void iris$startFrame(CallbackInfo ci,
                                 //? if <1.21 {
                                 @Local(ordinal = 0, argsOnly = true) float tickDelta
                                 //?} else
                                 /*@Local(ordinal = 0, argsOnly = true) net.minecraft.client.DeltaTracker deltaTracker*/
                                 ) {
        // This allows certain functions like float smoothing to function outside a world.
        CapturedRenderingState.INSTANCE.setRealTickDelta(
                //? if <1.21 {
                tickDelta
                //?} else
                /*deltaTracker.getGameTimeDeltaPartialTick(true)*/
        );
        SystemTimeUniforms.COUNTER.beginFrame();
        SystemTimeUniforms.TIMER.beginFrame(Util.getNanos());
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

	@Inject(method = "<init>", at = @At("TAIL"))
	private void iris$logSystem(Minecraft arg, ItemInHandRenderer arg2, ResourceManager arg3, RenderBuffers arg4, CallbackInfo ci) {
		Iris.logger.info("Hardware information:");
		Iris.logger.info("CPU: " + GlUtil.getCpuInfo());
		Iris.logger.info("GPU: " + GlUtil.getRenderer() + " (Supports OpenGL " + GlUtil.getOpenGLVersion() + ")");
		Iris.logger.info("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")");
	}

	@Redirect(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"))
	private void iris$disableVanillaHandRendering(ItemInHandRenderer itemInHandRenderer, float tickDelta, PoseStack poseStack, BufferSource bufferSource, LocalPlayer localPlayer, int light) {
		if (IrisApi.getInstance().isShaderPackInUse()) {
			return;
		}

		itemInHandRenderer.renderHandsWithItems(tickDelta, poseStack, bufferSource, localPlayer, light);
	}

	@Inject(method = "renderLevel", at = @At("TAIL"))
	private void iris$runColorSpace(CallbackInfo ci) {
		Iris.getPipelineManager().getPipeline().ifPresent(WorldRenderingPipeline::finalizeGameRendering);
	}

	@Redirect(method = "reloadShaders", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;"))
	private ArrayList<Program> iris$reloadGeometryShaders() {
		ArrayList<Program> programs = Lists.newArrayList();
		programs.addAll(IrisProgramTypes.GEOMETRY.getPrograms().values());
		programs.addAll(IrisProgramTypes.TESS_CONTROL.getPrograms().values());
		programs.addAll(IrisProgramTypes.TESS_EVAL.getPrograms().values());
		return programs;
	}
}
