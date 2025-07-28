package org.embeddedt.embeddium.impl.mixin.features.textures.mipmaps;

//? if >=1.21.5 {

/*import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(RenderPipelines.class)
public class RenderPipelinesMixin {
    /^*
     * @author embeddedt
     * @reason Force cutout/cutout_mipped to use 0.1F alpha cutoff to match modern handling
     ^/
    @ModifyConstant(method = "<clinit>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=pipeline/cutout_mipped")), constant = @Constant(floatValue = 0.5f))
    private static float adjustAlphaCutoff(float oldCutoff) {
        return 0.1F;
    }
}
*///?}