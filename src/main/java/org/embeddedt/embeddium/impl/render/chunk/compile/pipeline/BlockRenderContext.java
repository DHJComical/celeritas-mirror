package org.embeddedt.embeddium.impl.render.chunk.compile.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.api.world.EmbeddiumBlockAndTintGetter;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
//? if forge && >=1.19
import net.minecraftforge.client.model.data.ModelData;
//? if forge && <1.19
/*import net.minecraftforge.client.model.data.IModelData;*/
//? if neoforge
/*import net.neoforged.neoforge.client.model.data.ModelData;*/
import org.embeddedt.embeddium.impl.render.matrix_stack.CachingPoseStack;
import org.embeddedt.embeddium.impl.render.world.WorldSliceLocalGenerator;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Holds the context for the current block being rendered in a chunk section. This container is reused rather than
 * being freshly constructed for each block to avoid allocations.
 */
public class BlockRenderContext {
    private final EmbeddiumBlockAndTintGetter localSlice;

    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private final Vector3f origin = new Vector3f();

    private final PoseStack stack = new PoseStack();

    private BlockState state;
    private BakedModel model;

    private long seed;

    //? if forgelike && >=1.19.1
    private ModelData modelData;
    //? if forgelike && <1.19.1
    /*private IModelData modelData;*/
    private RenderType renderLayer;

    private int lightValue = -1;

    public BlockRenderContext(WorldSlice world) {
        this.localSlice = WorldSliceLocalGenerator.generate(world);
        ((CachingPoseStack)this.stack).embeddium$setCachingEnabled(true);
    }

    public void update(BlockPos pos, BlockPos origin, BlockState state, BakedModel model, long seed, /*? if forgelike && >=1.19 {*/ ModelData modelData, /*?}*//*? if forgelike && <1.19 {*/ /*IModelData modelData, *//*?}*/ RenderType renderLayer) {
        this.pos.set(pos);
        this.origin.set(origin.getX(), origin.getY(), origin.getZ());

        this.state = state;
        this.model = model;

        this.seed = seed;

        this.lightValue = -1;

        //? if forgelike
        this.modelData = modelData;
        this.renderLayer = renderLayer;
    }

    /**
     * @return The position (in world space) of the block being rendered
     */
    public BlockPos pos() {
        return this.pos;
    }

    /**
     * @return The world which the block is being rendered from. Guaranteed to be a new object for each subchunk.
     */
    public EmbeddiumBlockAndTintGetter localSlice() {
        return this.localSlice;
    }

    /**
     * @return The state of the block being rendered
     */
    public BlockState state() {
        return this.state;
    }

    /**
     * @return A PoseStack for custom renderers
     */
    public PoseStack stack() {
        return this.stack;
    }

    /**
     * @return The model used for this block
     */
    public BakedModel model() {
        return this.model;
    }

    /**
     * @return The origin of the block within the model
     */
    public Vector3fc origin() {
        return this.origin;
    }

    /**
     * @return The PRNG seed for rendering this block
     */
    public long seed() {
        return this.seed;
    }

    //? if forgelike && >=1.19 {
    /**
     * @return The additional data for model instance
     */
    public ModelData modelData() {
        return this.modelData;
    }
    //?}

    //? if forgelike && <1.19 {
    /*/^*
     * @return The additional data for model instance
     ^/
    public IModelData modelData() {
        return this.modelData;
    }
    *///?}

    /**
     * @return The render layer for model rendering
     */
    public RenderType renderLayer() {
        return this.renderLayer;
    }

    /**
     * @return The light emission of the current block
     */
    public int lightEmission() {
        if (this.lightValue == -1) {
            this.lightValue = WorldUtil.getLightEmission(this.state, this.localSlice, this.pos);
        }
        return this.lightValue;
    }
}
