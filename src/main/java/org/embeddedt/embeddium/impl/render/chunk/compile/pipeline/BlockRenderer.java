package org.embeddedt.embeddium.impl.render.chunk.compile.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.embeddedt.embeddium.impl.SodiumClientMod;
import org.embeddedt.embeddium.impl.model.color.ColorProvider;
import org.embeddedt.embeddium.impl.model.color.ColorProviderRegistry;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.DirectionUtil;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
//$ rng_import
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.18
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.api.BlockRendererRegistry;
import org.embeddedt.embeddium.api.model.EmbeddiumBakedModelExtension;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevelHolder;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.render.chunk.ChunkColorWriter;
import org.embeddedt.embeddium.impl.render.frapi.FRAPIModelUtils;
import org.embeddedt.embeddium.impl.render.frapi.FRAPIRenderHandler;
//? if ffapi && >=1.20
import org.embeddedt.embeddium.impl.render.frapi.IndigoBlockRenderContext;
import org.embeddedt.embeddium.impl.util.rand.XoRoShiRoRandom;

import java.util.Arrays;
import java.util.List;

/**
 * The Embeddium equivalent to vanilla's ModelBlockRenderer. This is the primary component of the chunk meshing logic;
 * it is responsible for accepting {@link BlockRenderContext} and generating the appropriate geometry.
 * <p>
 * This class does not need to be thread-safe, as a separate instance is allocated per meshing thread.
 */
public class BlockRenderer {
    private static final PoseStack EMPTY_STACK = new PoseStack();
    //? if >=1.19 {
    private final RandomSource random = new SingleThreadedRandomSource(42L);
    //?} else
    /*private final Random random = new XoRoShiRoRandom(42L);*/

    private final ColorProviderRegistry colorProviderRegistry;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData quadLightData = new QuadLightData();

    private final LightPipelineProvider lighters;

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private final boolean useAmbientOcclusion;

    private final int[] quadColors = new int[4];

    /**
     * Tracks whether the MC-138211 quad reorienting fix should be applied during emission of quad geometry.
     * This fix must be disabled with certain modded models that use superimposed quads, as it can alter the triangulation
     * of some layers but not others, resulting in Z-fighting.
     */
    private boolean useReorienting;

    /**
     * The list of registered custom block renderers. These may augment or fully bypass the model system for the
     * block.
     */
    private final List<BlockRendererRegistry.Renderer> customRenderers = new ObjectArrayList<>();

    private final FRAPIRenderHandler fabricModelRenderingHandler;

    private final ChunkColorWriter colorEncoder = ChunkColorWriter.get();

    private final boolean useRenderPassOptimization;

    public BlockRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters) {
        this.colorProviderRegistry = colorRegistry;
        this.lighters = lighters;

        this.occlusionCache = new BlockOcclusionCache();
        this.useAmbientOcclusion = Minecraft.useAmbientOcclusion();
        //? if ffapi && >=1.20 {
        this.fabricModelRenderingHandler = FRAPIRenderHandler.INDIGO_PRESENT ? new IndigoBlockRenderContext(this.occlusionCache, lighters.getLightData()) : null;
        //?} else {
        /*this.fabricModelRenderingHandler = null;
        *///?}
        this.useRenderPassOptimization = SodiumClientMod.options().performance.useRenderPassOptimization && !ShaderModBridge.areShadersEnabled();
    }

    /**
     * Renders all geometry for a block into the given chunk build buffers.
     * @param ctx the context for the current block being rendered
     * @param buffers the buffer to output geometry to
     */
    public void renderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers) {
        var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(ctx.renderLayer());
        var meshBuilder = buffers.get(material);

        ColorProvider<BlockState> colorizer = this.colorProviderRegistry.getColorProvider(ctx.state().getBlock());

        LightMode mode = this.getLightingMode(ctx);
        LightPipeline lighter = this.lighters.getLighter(mode);
        Vec3 renderOffset;

        //? if >=1.20 {
        if (ctx.state().hasOffsetFunction()) {
            renderOffset = ctx.state().getOffset(/*? if <1.21.2 {*/ctx.localSlice(),/*?}*/ ctx.pos());
        } else {
            renderOffset = Vec3.ZERO;
        }
        //?} else
        /*renderOffset = ctx.state().getOffset(ctx.localSlice(), ctx.pos());*/

        // Process custom renderers
        customRenderers.clear();
        BlockRendererRegistry.instance().fillCustomRenderers(customRenderers, ctx);

        if(!customRenderers.isEmpty()) {
            for (BlockRendererRegistry.Renderer customRenderer : customRenderers) {
                try(var consumer = meshBuilder.asVertexConsumer(material, ctx)) {
                    consumer.embeddium$setOffset(ctx.origin());
                    BlockRendererRegistry.RenderResult result = customRenderer.renderBlock(ctx, random, consumer);
                    if (result == BlockRendererRegistry.RenderResult.OVERRIDE) {
                        return;
                    }
                }
            }
        }

        // Delegate FRAPI models to their pipeline
        if (FRAPIModelUtils.isFRAPIModel(ctx.model())) {
            this.fabricModelRenderingHandler.reset();
            this.fabricModelRenderingHandler.renderEmbeddium(ctx, buffers, ctx.stack(), random);
            return;
        }

        boolean canReorientNullCullface = true;

        for (Direction face : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> quads = this.getGeometry(ctx, face);

            if (!quads.isEmpty() && this.isFaceVisible(ctx, face)) {
                this.useReorienting = true;
                this.renderQuadList(ctx, material, lighter, colorizer, renderOffset, buffers, meshBuilder, quads, face);
                if (!this.useReorienting) {
                    // Reorienting was disabled on this side, make sure it's disabled for the null cullface too, in case
                    // a mod layers textures in different lists
                    canReorientNullCullface = false;
                }
            }
        }

        List<BakedQuad> all = this.getGeometry(ctx, null);

        if (!all.isEmpty()) {
            this.useReorienting = canReorientNullCullface;
            this.renderQuadList(ctx, material, lighter, colorizer, renderOffset, buffers, meshBuilder, all, null);
        }
    }

    private List<BakedQuad> getGeometry(BlockRenderContext ctx, Direction face) {
        var random = this.random;
        random.setSeed(ctx.seed());

        return ctx.model().getQuads(ctx.state(), face, random/*? if forgelike && >=1.19 {*/, ctx.modelData(), ctx.renderLayer()/*?}*/ /*? if forgelike && <1.19 {*//*, ctx.modelData()*//*?}*/);
    }

    private boolean isFaceVisible(BlockRenderContext ctx, Direction face) {
        return this.occlusionCache.shouldDrawSide(ctx.state(), ctx.localSlice(), ctx.pos(), face);
    }

    private static int computeLightFlagMask(BakedQuad quad) {
        int flag = 0;

        //? if forgelike && >=1.19 {
        if (quad.hasAmbientOcclusion()) {
            flag |= 1;
        }
        //?}

        //? if >=1.16 {
        if (quad.isShade()) {
            flag |= 2;
        }
        //?}

        return flag;
    }

    /**
     * {@return true if all quads in the given list use similar enough lighting configuration that reorientation is
     * unlikely to lead to z-fighting}
     */
    private static boolean checkQuadsHaveSameLightingConfig(List<BakedQuad> quads) {
        int quadsSize = quads.size();

        // By definition, singleton or empty lists of quads have a common lighting config. Only check larger lists
        if (quadsSize >= 2) {
            int flagMask = -1;
            // noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < quadsSize; i++) {
                int newFlag = computeLightFlagMask(quads.get(i));
                if (flagMask == -1) {
                    flagMask = newFlag;
                } else if(newFlag != flagMask) {
                    return false;
                }
            }
        }

        return true;
    }

    private ChunkModelBuilder chooseOptimalBuilder(Material defaultMaterial, ChunkBuildBuffers buffers, ChunkModelBuilder defaultBuilder, BakedQuadView quad) {
        var renderPassConfiguration = buffers.getRenderPassConfiguration();
        if (defaultMaterial == renderPassConfiguration.defaultSolidMaterial() || !this.useRenderPassOptimization || (quad.getFlags() & ModelQuadFlags.IS_PASS_OPTIMIZABLE) == 0 || quad.getSprite() == null) {
            // No improvement possible
            return defaultBuilder;
        }

        SpriteTransparencyLevel level = SpriteTransparencyLevelHolder.getTransparencyLevel(quad.getSprite());

        if (level == SpriteTransparencyLevel.OPAQUE) {
            // Can use solid with no visual difference
            return buffers.get(renderPassConfiguration.defaultSolidMaterial());
        } else if (level == SpriteTransparencyLevel.TRANSPARENT && defaultMaterial == renderPassConfiguration.defaultTranslucentMaterial()) {
            // Can use cutout_mipped with no visual difference
            return buffers.get(renderPassConfiguration.defaultCutoutMippedMaterial());
        } else {
            // Have to use default
            return defaultBuilder;
        }
    }

    private void renderQuadList(BlockRenderContext ctx, Material material, LightPipeline lighter, ColorProvider<BlockState> colorizer, Vec3 offset,
                                ChunkBuildBuffers buffers, ChunkModelBuilder defaultBuilder, List<BakedQuad> quads, Direction cullFace) {

        if(!checkQuadsHaveSameLightingConfig(quads)) {
            // Disable reorienting if quads use different light configurations, as otherwise layered quads
            // may be triangulated differently from others in the stack, and that will cause z-fighting.
            this.useReorienting = false;
        }

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuadView quad = (BakedQuadView) quads.get(i);

            final var lightData = this.getVertexLight(ctx, quad.hasAmbientOcclusion() ? lighter : this.lighters.getLighter(LightMode.FLAT), cullFace, quad);
            final var vertexColors = this.getVertexColors(ctx, colorizer, quad);

            ChunkModelBuilder builder = this.chooseOptimalBuilder(material, buffers, defaultBuilder, quad);

            this.writeGeometry(ctx, builder, offset, material, quad, vertexColors, lightData);

            TextureAtlasSprite sprite = quad.getSprite();

            if (sprite != null) {
                builder.addSprite(sprite);
            }
        }
    }

    private QuadLightData getVertexLight(BlockRenderContext ctx, LightPipeline lighter, Direction cullFace, BakedQuadView quad) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, ctx.pos(), light, cullFace, quad.getLightFace(), quad.hasShade());

        return light;
    }

    private int[] getVertexColors(BlockRenderContext ctx, ColorProvider<BlockState> colorProvider, BakedQuadView quad) {
        final int[] vertexColors = this.quadColors;

        if (colorProvider != null && quad.hasColor()) {
            colorProvider.getColors(ctx.localSlice(), ctx.pos(), ctx.state(), quad, vertexColors);
            // Force full alpha on all colors
            for(int i = 0; i < vertexColors.length; i++) {
                vertexColors[i] |= 0xFF000000;
            }
        } else {
            Arrays.fill(vertexColors, 0xFFFFFFFF);
        }

        return vertexColors;
    }

    private void writeGeometry(BlockRenderContext ctx,
                               ChunkModelBuilder builder,
                               Vec3 offset,
                               Material material,
                               BakedQuadView quad,
                               int[] colors,
                               QuadLightData light)
    {
        ModelQuadOrientation orientation = this.useReorienting ? ModelQuadOrientation.orientByBrightness(light.br, light.lm) : ModelQuadOrientation.NORMAL;
        var vertices = this.vertices;

        ModelQuadFacing normalFace = quad.getNormalFace();

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            var out = vertices[dstIndex];
            out.x = ctx.origin().x() + quad.getX(srcIndex) + (float) offset.x();
            out.y = ctx.origin().y() + quad.getY(srcIndex) + (float) offset.y();
            out.z = ctx.origin().z() + quad.getZ(srcIndex) + (float) offset.z();

            out.color = colorEncoder.writeColor(ModelQuadUtil.mixARGBColors(colors[srcIndex], quad.getColor(srcIndex)), light.br[srcIndex]);

            out.u = quad.getTexU(srcIndex);
            out.v = quad.getTexV(srcIndex);

            out.light = ModelQuadUtil.mergeBakedLight(quad.getLight(srcIndex), quad.getVanillaLightEmission(), light.lm[srcIndex]);
        }

        var vertexBuffer = builder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material, ctx);
    }

    //? if forge || fabric {
    private boolean modelUsesAO(BlockRenderContext ctx, BakedModel model) {
        //? if forge && >=1.19 {
        return model.useAmbientOcclusion(ctx.state(), ctx.renderLayer());
        //?} else if forge && >=1.18 {
        /*return model.useAmbientOcclusion(ctx.state());
        *///?} else if forge {
        /*return model.isAmbientOcclusion(ctx.state());
        *///?} else {
        /*return model.useAmbientOcclusion();
        *///?}
    }

    private LightMode getLightingMode(BlockRenderContext ctx) {
        var model = ctx.model();
        var state = ctx.state();
        if (this.useAmbientOcclusion && modelUsesAO(ctx, model)
                && (((EmbeddiumBakedModelExtension)model).useAmbientOcclusionWithLightEmission(state, ctx.renderLayer()) || ctx.lightEmission() == 0)) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
    //?} else {
    /*private LightMode getLightingMode(BlockRenderContext ctx) {
        var model = ctx.model();
        var state = ctx.state();
        boolean canBeSmooth = this.useAmbientOcclusion && switch(model.useAmbientOcclusion(state, ctx.modelData(), ctx.renderLayer())) {
            case TRUE -> true;
            case DEFAULT -> ctx.lightEmission() == 0;
            case FALSE -> false;
        };
        return canBeSmooth ? LightMode.SMOOTH : LightMode.FLAT;
    }
    *///?}
}
