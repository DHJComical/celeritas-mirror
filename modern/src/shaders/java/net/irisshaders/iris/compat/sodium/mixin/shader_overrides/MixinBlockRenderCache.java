package net.irisshaders.iris.compat.sodium.mixin.shader_overrides;

import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;

@Mixin(BlockRenderCache.class)
public class MixinBlockRenderCache {
    /**
     * @author embeddedt
     * @reason provide render type overrides
     */
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/impl/modern/render/chunk/compile/pipeline/BlockRenderer;<init>(Lorg/embeddedt/embeddium/impl/model/color/ColorProviderRegistry;Lorg/embeddedt/embeddium/impl/model/light/LightPipelineProvider;Ljava/util/Map;)V"), index = 2)
    private Map<Block, RenderType> shaders$getRenderType(Map<Block, RenderType> original) {
        return WorldRenderingSettings.INSTANCE.getBlockTypeIds();
    }
}
