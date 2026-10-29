package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.shaders.Program;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.blending.DepthColorStorage;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.lwjgl.opengl.KHRDebug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

/**
 * The apply and clear injections only see shaders Iris doesn't replace, since {@link
 * net.irisshaders.iris.pipeline.programs.IrisShaderInstance} overrides both without calling them.
 */
@Mixin(ShaderInstance.class)
public abstract class MixinShaderInstance {
    private static boolean shouldOverrideShaders() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline) pipeline).shouldOverrideShaders();
        } else {
            return false;
        }
    }


    @Shadow
    @Final
    private int programId;

    @Shadow
    public abstract String getName();

    @Shadow
    @Final
    private Program vertexProgram;

    @Shadow
    @Final
    private Program fragmentProgram;


    @Unique
    private static final MethodHandle NONE = MethodHandles.constant(Integer.class, 2);
    @Unique
    private static final MethodHandle ALWAYS = MethodHandles.constant(Integer.class, 1);
    @Unique
    private MethodHandle shouldSkip;
    private static Map<Class<?>, MethodHandle> shouldSkipList = new Object2ObjectOpenHashMap<>();

    @Inject(method = "/<init>/", at = @At("TAIL"), require = 0)
    private void iriss$storeSkip(CallbackInfo ci) {
        shouldSkip = shouldSkipList.computeIfAbsent(getClass(), x -> {
            try {
                MethodHandle iris$skipDraw = MethodHandles.lookup().findVirtual(x, "iris$skipDraw", MethodType.methodType(boolean.class));
                CeleritasShaders.logger().warn("Class " + x.getName() + " has opted out of being rendered with shaders.");
                return iris$skipDraw;
            } catch (NoSuchMethodException | IllegalAccessException e) {
                return NONE;
            }
        });
    }

    public boolean iris$shouldSkipThis() {
        // Celeritas always allows unknown shaders
        if (!Iris.getIrisConfig().isBlockUnknownShaders()) {
            if (ShadowRenderer.ACTIVE) return true;
            if (!shouldOverrideShaders()) return false;
            if (shouldSkip == NONE) return false;
            if (shouldSkip == ALWAYS) return true;
            try {
                return (boolean) shouldSkip.invoke(((Object)this));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            return shouldOverrideShaders();
        }
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void iris$lockDepthColorState(CallbackInfo ci) {
        if (!iris$shouldSkipThis()) {
            if (shouldOverrideShaders()) {
                WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
                if (pipeline instanceof IrisRenderingPipeline) {
                    if (ShadowRenderer.ACTIVE) {
                        // Fallback shadow rendering is disabled by Iris rn
                        //((IrisRenderingPipeline) pipeline).bindDefaultShadow();
                    } else {
                        ((IrisRenderingPipeline) pipeline).bindDefault();
                    }
                }
            }

            return;
        }

        DepthColorStorage.disableDepthColor();
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void iris$unlockDepthColorState(CallbackInfo ci) {
        if (!iris$shouldSkipThis()) {
            if (shouldOverrideShaders()) {
                WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
                if (pipeline instanceof IrisRenderingPipeline) {
                    Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                }
            }

            return;
        }

        DepthColorStorage.unlockDepthColor();
    }

    @Inject(method = "/<init>/", at = @At("RETURN"))
    private void iris$injectDebug(CallbackInfo ci) {
        String name = this.getName();
        GLDebug.nameObject(KHRDebug.GL_PROGRAM, this.programId, name);
        GLDebug.nameObject(KHRDebug.GL_SHADER, this.vertexProgram.getId(), name);
        GLDebug.nameObject(KHRDebug.GL_SHADER, this.fragmentProgram.getId(), name);
    }
}
