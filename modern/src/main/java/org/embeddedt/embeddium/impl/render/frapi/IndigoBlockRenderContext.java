package org.embeddedt.embeddium.impl.render.frapi;

//? if ffapi && >=1.20 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.modern.render.chunk.MojangVertexConsumer;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockOcclusionCache;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
//? if forge
import net.minecraftforge.client.model.data.ModelData;
//? if neoforge
/*import net.neoforged.neoforge.client.model.data.ModelData;*/
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Adaptation of Indigo's {@link BlockRenderContext} that delegates back to the Sodium renderer.
 */
public class IndigoBlockRenderContext extends BlockRenderContext implements FRAPIRenderHandler {
    private org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext currentContext;
    private ChunkBuildBuffers currentBuffers;
    private final BlockOcclusionCache occlusionCache;
    private final LightDataAccess lightDataAccess;
    private final Object2ObjectOpenHashMap<Material, MojangVertexConsumer> mojangVertexConsumers;

    private int cullChecked, cullValue;

    public IndigoBlockRenderContext(BlockOcclusionCache occlusionCache, LightDataAccess lightDataAccess) {
        this.occlusionCache = occlusionCache;
        this.lightDataAccess = lightDataAccess;
        this.mojangVertexConsumers = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected AoCalculator createAoCalc(BlockRenderInfo blockInfo) {
        return new AoCalculator(blockInfo) {
            @Override
            public int light(BlockPos pos, BlockState state) {
                int data = lightDataAccess.get(pos);
                return LightDataAccess.getLightmap(data);
            }

            @Override
            public float ao(BlockPos pos, BlockState state) {
                return LightDataAccess.unpackAO(lightDataAccess.get(pos));
            }
        };
    }

    //? if <1.21.4-rc.3 {
    @Override
    public boolean isFaceCulled(@Nullable Direction face) {
        if (face == null) {
            return false;
        }

        int fM = (1 << face.ordinal());

        // Use a bitmask to cache the cull checks so we don't run them more than once per face
        if((cullChecked & fM) != 0) {
            return (cullValue & fM) != 0;
        } else {
            var ctx = this.currentContext;
            boolean flag = !this.occlusionCache.shouldDrawSide(ctx.state(), ctx.localSlice(), ctx.pos(), face);
            if(flag) {
                cullValue |= fM;
            }
            cullChecked |= fM;
            return flag;
        }
    }
    //?}

    @Override
    protected VertexConsumer getVertexConsumer(RenderType layer) {
        var material = currentBuffers.getRenderPassConfiguration().getMaterialForRenderType(layer);
        var consumer = mojangVertexConsumers.get(material);
        if (consumer == null) {
            consumer = new MojangVertexConsumer();
            mojangVertexConsumers.put(material, consumer);
        }
        consumer.initialize(currentBuffers.get(material), material, null);
        consumer.embeddium$setOffset(currentContext.origin());
        return consumer;
    }

    @Override
    protected void bufferQuad(MutableQuadViewImpl quad, VertexConsumer vertexConsumer) {
        super.bufferQuad(quad, vertexConsumer);
        if(vertexConsumer instanceof MojangVertexConsumer modelConsumer) {
            modelConsumer.close();
        }
    }

    public void reset() {
        cullChecked = 0;
        cullValue = 0;
    }

    private RuntimeException processException(Throwable e) {
        if(e instanceof RuntimeException) {
            return (RuntimeException)e;
        } else {
            return new IllegalStateException("Unexpected throwable", e);
        }
    }

    public void renderEmbeddium(org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext ctx,
                                ChunkBuildBuffers buffers,
                                PoseStack mStack,
                                RandomSource random) {
        this.currentContext = ctx;
        this.currentBuffers = buffers;
        // We unfortunately have no choice but to push a pose here since FRAPI now mutates the given stack
        mStack.pushPose();
        try {
            this.render(ctx.localSlice(), ctx.model(), ctx.state(), ctx.pos(), mStack, null, true, random, ctx.seed(), OverlayTexture.NO_OVERLAY /*? if forgelike {*/ , ctx.modelData(), ctx.renderLayer() /*?}*/);
        } catch(Throwable e) {
            throw processException(e);
        } finally {
            mStack.popPose();
            this.currentContext = null;
            this.currentBuffers = null;
        }
    }
}
// }
