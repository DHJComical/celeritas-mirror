package org.embeddedt.embeddium.impl.mixin.features.shader.uniform;

//? if >=1.17 {
import com.mojang.blaze3d.shaders.Uniform;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

//? if <1.21.2
import net.minecraft.client.renderer.ShaderInstance;
//? if >=1.21.2
/*import net.minecraft.client.renderer.CompiledShaderProgram;*/

/**
 * On the NVIDIA drivers (and maybe some others), the OpenGL submission thread requires expensive state synchronization
 * to happen when glGetUniformLocation and glGetInteger are called. In our case, this is rather unnecessary, since
 * these uniform locations can be trivially cached.
 */
//? if <1.21.2
@Mixin(ShaderInstance.class)
//? if >=1.21.2
/*@Mixin(CompiledShaderProgram.class)*/
public class ShaderProgramMixin {
    @Shadow
    @Final
    private List<Uniform> uniforms;

    //? if <1.21.2 {
    @Shadow
    @Final
    private List<String> samplerNames;

    @Shadow
    @Final
    private int programId;


    @Unique
    private Object2IntMap<String> uniformCache;

    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;glGetUniformLocation(ILjava/lang/CharSequence;)I"))
    private int redirectGetUniformLocation(int program, CharSequence name) {
        if(this.uniformCache == null) {
            this.uniformCache = new Object2IntOpenHashMap<>();
            this.uniformCache.defaultReturnValue(-1);

            for (var samplerName : this.samplerNames) {
                var location = Uniform.glGetUniformLocation(this.programId, samplerName);

                if(location != -1)
                    this.uniformCache.put(samplerName, location);
            }
        }
        var location = this.uniformCache.getInt(name);

        if (location == -1) {
            throw new IllegalStateException("Failed to find uniform '%s' during shader bind".formatted(name));
        }

        return location;
    }

    @Redirect(method = "apply", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ShaderInstance;uniforms:Ljava/util/List;", ordinal = 0))
    private List<Uniform> uploadUniforms(ShaderInstance instance) {
    //?}

    //? if >=1.21.2 {
    /*@Redirect(method = "apply", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/CompiledShaderProgram;uniforms:Ljava/util/List;", ordinal = 0))
    private List<Uniform> uploadUniforms(CompiledShaderProgram instance) {
    *///?}
        List<Uniform> uniforms = this.uniforms;
        //noinspection ForLoopReplaceableByForEach
        for(int i = 0; i < uniforms.size(); i++) {
            uniforms.get(i).upload();
        }
        return Collections.emptyList();
    }
}
//?}