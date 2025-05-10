package net.irisshaders.iris.mixin;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
//? if <1.21.2
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Uniform;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.gl.blending.DepthColorStorage;
import net.irisshaders.iris.pipeline.ModernIrisRenderingPipeline;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.irisshaders.iris.pipeline.programs.FallbackShader;
import net.irisshaders.iris.pipeline.programs.ShaderInstanceInterface;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.Minecraft;
//? if >=1.21.2
/*import net.minecraft.client.renderer.CompiledShaderProgram;*/
//? if <1.21.2
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

//? if <1.21.2
@Mixin(ShaderInstance.class)
//? if >=1.21.2
/*@Mixin(CompiledShaderProgram.class)*/
public abstract class MixinShaderInstance implements ShaderInstanceInterface {
    @Unique
    private static final ImmutableSet<String> ATTRIBUTE_LIST = ImmutableSet.of("Position", "Color", "Normal", "UV0", "UV1", "UV2");

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

    //? if <1.21.2 {
    @Shadow
    public abstract int getId();

    @Shadow
    public abstract String getName();

    @Shadow
    @Final
    private Program vertexProgram;

    @Shadow
    @Final
    private Program fragmentProgram;

    @Redirect(method = "updateLocations",
            at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void iris$redirectLogSpam(Logger logger, String message, Object arg1, Object arg2) {
        if (((Object) this) instanceof ExtendedShader || ((Object) this) instanceof FallbackShader) {
            return;
        }

        logger.warn(message, arg1, arg2);
    }

    @Redirect(method = "/<init>/", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;glBindAttribLocation(IILjava/lang/CharSequence;)V"))
    public void iris$redirectBindAttributeLocation(int i, int j, CharSequence charSequence) {
        if (((Object) this) instanceof ExtendedShader && ATTRIBUTE_LIST.contains(charSequence)) {
            Uniform.glBindAttribLocation(i, j, "iris_" + charSequence);
        } else {
            Uniform.glBindAttribLocation(i, j, charSequence);
        }
    }
    //?}


    @Unique
    private static final MethodHandle NONE = MethodHandles.constant(Integer.class, 2);
    @Unique
    private static final MethodHandle ALWAYS = MethodHandles.constant(Integer.class, 1);
    @Unique
    private MethodHandle shouldSkip;
    private static Map<Class<?>, MethodHandle> shouldSkipList = new Object2ObjectOpenHashMap<>();

    static {
        shouldSkipList.put(ExtendedShader.class, NONE);
        shouldSkipList.put(FallbackShader.class, NONE);
    }

    @Inject(method = "/<init>/", at = @At("TAIL"), require = 0)
    private void iriss$storeSkip(CallbackInfo ci) {
        shouldSkip = shouldSkipList.computeIfAbsent(getClass(), x -> {
            try {
                MethodHandle iris$skipDraw = MethodHandles.lookup().findVirtual(x, "iris$skipDraw", MethodType.methodType(boolean.class));
                IRIS_LOGGER.warn("Class " + x.getName() + " has opted out of being rendered with shaders.");
                return iris$skipDraw;
            } catch (NoSuchMethodException | IllegalAccessException e) {
                return NONE;
            }
        });
    }

    public boolean iris$shouldSkipThis() {
        // Celeritas always allows unknown shaders
        if (!IrisCommon.getIrisConfig().isBlockUnknownShaders()) {
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
            return !(((Object) this) instanceof ExtendedShader || ((Object) this) instanceof FallbackShader || !shouldOverrideShaders());
        }
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void iris$lockDepthColorState(CallbackInfo ci) {
        if (!iris$shouldSkipThis()) {
            if (!isKnownShader() && shouldOverrideShaders()) {
                WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
                if (pipeline instanceof ModernIrisRenderingPipeline) {
                    if (ShadowRenderer.ACTIVE) {
                        // Fallback shadow rendering is disabled by Iris rn
                        //((IrisRenderingPipeline) pipeline).bindDefaultShadow();
                    } else {
                        ((ModernIrisRenderingPipeline) pipeline).bindDefault();
                    }
                }
            }

            return;
        }

        DepthColorStorage.disableDepthColor();
    }

    private boolean isKnownShader() {
        return ((Object) this) instanceof ExtendedShader || ((Object) this) instanceof FallbackShader;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void iris$unlockDepthColorState(CallbackInfo ci) {
        if (!iris$shouldSkipThis()) {
            if (!isKnownShader() && shouldOverrideShaders()) {
                WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
                if (pipeline instanceof ModernIrisRenderingPipeline) {
                    Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                }
            }

            return;
        }

        DepthColorStorage.unlockDepthColor();
    }

    //? if <1.21.2 {
    @Redirect(method = "/<init>/", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/GsonHelper;parse(Ljava/io/Reader;)Lcom/google/gson/JsonObject;"))
    public JsonObject iris$setupGeometryShader(Reader reader, @Local(ordinal = 0, argsOnly = true) ResourceProvider resourceProvider, @Local(ordinal = 0) ResourceLocation name) {
        this.iris$createExtraShaders(resourceProvider, name);
        return GsonHelper.parse(reader);
    }

    @Inject(method = "/<init>/", at = @At("RETURN"))
    private void iris$injectDebug(CallbackInfo ci) {
        String name = this.getName();
        GLDebug.nameObject(KHRDebug.GL_PROGRAM, this.programId, name);
        GLDebug.nameObject(KHRDebug.GL_SHADER, this.vertexProgram.getId(), name);
        GLDebug.nameObject(KHRDebug.GL_SHADER, this.fragmentProgram.getId(), name);
    }
    //?}

    @Override
    public void iris$createExtraShaders(ResourceProvider provider, ResourceLocation name) {
        //no-op, used for ExtendedShader to call before the super constructor
    }
}
