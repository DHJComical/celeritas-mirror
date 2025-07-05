package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

//? if fabric && ffapi && >=1.17 {

/*import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.impl.modern.render.chunk.MojangVertexConsumer;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;

import java.util.Collection;
import java.util.function.Function;

public class FabricFluidRenderer {
    private final Reference2BooleanOpenHashMap<Class<? extends FluidRenderHandler>> handlersUsingCustomRenderer = new Reference2BooleanOpenHashMap<>();

    private boolean renderingCustomFluid = false;

    private final MojangVertexConsumer mojangConsumer = new MojangVertexConsumer();

    public boolean renderCustomFluid(BlockRenderContext ctx, FluidRenderHandler handler, FluidState fluidState, ChunkBuildBuffers buffers, Material material) {
        // Re-entrancy check - if fluid rendering calls super
        if (renderingCustomFluid) {
            return false;
        }

        // TODO move to a separate class
        boolean overridesRenderFluid = handlersUsingCustomRenderer.computeIfAbsent(handler.getClass(), (Function<? super Class<? extends FluidRenderHandler>, Boolean>) handlerClass -> {
            try {
                var method = handlerClass.getMethod("renderFluid", BlockPos.class, BlockAndTintGetter.class, VertexConsumer.class, /^? if >=1.18 {^/ BlockState.class, /^?}^/ FluidState.class);
                return method.getDeclaringClass() != FluidRenderHandler.class;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Unable to find renderFluid method. Possibly a mismatched Fabric API version?", e);
            }
        });

        if (!overridesRenderFluid) {
            // Just use our renderer for higher performance, skip going through FAPI and the re-entrance path
            return false;
        }

        var modelBuffer = buffers.get(material);

        // Set a flag so that we can re-enter the main render() method as the default renderer
        renderingCustomFluid = true;
        try {
            // Call vanilla fluid renderer and capture the results
            try(var consumer = mojangConsumer.initialize(modelBuffer, material, ctx)) {
                Minecraft.getInstance().getBlockRenderer().renderLiquid(ctx.pos(), ctx.localSlice(), consumer, /^? if >=1.18 {^/ ctx.state(), /^?}^/ fluidState);
            }
        } finally {
            renderingCustomFluid = false;
        }

        // Mark fluid sprites as being used in rendering
        TextureAtlasSprite[] sprites = handler.getFluidSprites(ctx.localSlice(), ctx.pos(), fluidState);
        if (modelBuffer.getSectionContextBundle() instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
            for(TextureAtlasSprite sprite : sprites) {
                //noinspection PointlessNullCheck
                if (sprite != null && SpriteUtil.hasAnimation(sprite)) {
                    //noinspection unchecked
                    ((Collection<TextureAtlasSprite>)mcData.animatedSprites).add(sprite);
                }
            }
        }

        return true;
    }
}
*///?}
