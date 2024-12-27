package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

//? if >=1.15
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.util.RandomSource;
import org.embeddedt.embeddium.api.world.EmbeddiumBlockAndTintGetter;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.world.WorldSlice;
//? if >=1.15
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
@Accessors(fluent = true)
public class BlockRenderContext {
    private final EmbeddiumBlockAndTintGetter localSlice;

    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private final Vector3f origin = new Vector3f();

    //? if >=1.15
    private PoseStack stack;

    private BlockState state;
    private BakedModel model;

    private long seed;

    //? if forgelike {
    @Setter
    @Accessors(fluent = false)
    //?}
    //? if forgelike && >=1.19.1
    private ModelData modelData;
    //? if forgelike && <1.19.1
    /*private IModelData modelData;*/

    //? if >=1.15 {
    @Setter
    @Accessors(fluent = false)
    private RenderType renderLayer;
    //?}

    private int lightValue = -1;

    @Getter
    //? if >=1.19 {
    private final RandomSource random = new net.minecraft.world.level.levelgen.SingleThreadedRandomSource(42L);
    //?} else
    /*private final Random random = new org.embeddedt.embeddium.impl.util.rand.XoRoShiRoRandom(42L);*/

    @Getter
    private GeometryCategory category = GeometryCategory.BLOCK;

    public BlockRenderContext(WorldSlice world) {
        this.localSlice = WorldSliceLocalGenerator.generate(world);
    }

    public void update(GeometryCategory category, BlockPos pos, BlockPos origin, BlockState state, BakedModel model, long seed) {
        this.category = category;
        this.pos.set(pos);
        this.origin.set(origin.getX(), origin.getY(), origin.getZ());

        this.state = state;
        this.model = model;

        this.seed = seed;

        this.lightValue = -1;
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

    //? if >=1.15 {
    /**
     * @return A PoseStack for custom renderers
     */
    public PoseStack stack() {
        if (this.stack == null) {
            this.stack = new PoseStack();
            ((CachingPoseStack)this.stack).embeddium$setCachingEnabled(true);
        }
        return this.stack;
    }
    //?}

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

    //? if >=1.15 {
    /**
     * @return The render layer for model rendering
     */
    public RenderType renderLayer() {
        return this.renderLayer;
    }
    //?}

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
