package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.impl.Celeritas;
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
import org.embeddedt.embeddium.impl.modern.render.chunk.ContextAwareChunkVertexEncoder;
import org.embeddedt.embeddium.impl.modern.render.chunk.MojangVertexConsumer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.DirectionUtil;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.api.BlockRendererRegistry;
import org.embeddedt.embeddium.api.model.EmbeddiumBakedModelExtension;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.modern.render.chunk.sprite.SpriteTransparencyLevelHolder;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.render.chunk.ChunkColorWriter;
import org.embeddedt.embeddium.impl.render.frapi.FRAPIModelUtils;
import org.embeddedt.embeddium.impl.render.frapi.FRAPIRenderHandler;
//? if ffapi && >=1.20
import org.embeddedt.embeddium.impl.render.frapi.IndigoBlockRenderContext;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The Embeddium equivalent to vanilla's ModelBlockRenderer. This is the primary component of the chunk meshing logic;
 * it is responsible for accepting {@link BlockRenderContext} and generating the appropriate geometry.
 * <p>
 * This class does not need to be thread-safe, as a separate instance is allocated per meshing thread.
 */
public class BlockRenderer {
    private final ColorProviderRegistry colorProviderRegistry;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData quadLightData = new QuadLightData();

    private final LightPipelineProvider lighters;

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private final boolean useAmbientOcclusion;

    private final int[] quadColors = new int[4];

    /**
     * The list of registered custom block renderers. These may augment or fully bypass the model system for the
     * block.
     */
    private final List<BlockRendererRegistry.Renderer> customRenderers = new ObjectArrayList<>();

    private final FRAPIRenderHandler fabricModelRenderingHandler;

    private final ChunkColorWriter colorEncoder = ChunkColorWriter.get();

    private final boolean isRenderPassOptEnabled;
    private final MojangVertexConsumer vertexConsumer = new MojangVertexConsumer();

    /**
     * Tracks whether the MC-138211 quad reorienting fix should be applied during emission of quad geometry.
     * This fix must be disabled with certain modded models that use superimposed quads, as it can alter the triangulation
     * of some layers but not others, resulting in Z-fighting.
     */
    private static final int USE_REORIENTING = 0x1;
    private static final int USE_RENDER_PASS_OPTIMIZATION = 0x2;
    private static final int USE_ALL_THINGS = 0xFFFFFFFF;

    private int quadRenderingFlags = 0;

    private final Map<Block, RenderType> renderTypeOverrides;

    public BlockRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters, @Nullable Map<Block, RenderType> renderTypeOverrides) {
        this.colorProviderRegistry = colorRegistry;
        this.lighters = lighters;
        this.renderTypeOverrides = renderTypeOverrides;

        this.occlusionCache = new BlockOcclusionCache();
        this.useAmbientOcclusion = Minecraft.useAmbientOcclusion();
        //? if ffapi && >=1.20 {
        this.fabricModelRenderingHandler = FRAPIRenderHandler.INDIGO_PRESENT ? new IndigoBlockRenderContext(this.occlusionCache, lighters.getLightData()) : null;
        //?} else {
        /*this.fabricModelRenderingHandler = null;
        *///?}
        this.isRenderPassOptEnabled = Celeritas.options().performance.useRenderPassOptimization;
    }

    /**
     * Renders all geometry for a block into the given chunk build buffers.
     * @param ctx the context for the current block being rendered
     * @param buffers the buffer to output geometry to
     */
    public void renderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers) {
        int defaultQuadRenderingFlags = USE_ALL_THINGS;
        RenderType blockRenderType = ctx.renderLayer();

        if (this.renderTypeOverrides != null) {
            RenderType type = this.renderTypeOverrides.get(ctx.state().getBlock());
            if (type != null) {
                blockRenderType = type;
                defaultQuadRenderingFlags &= ~USE_RENDER_PASS_OPTIMIZATION;
            }
        }

        var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(blockRenderType);
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
                try(var consumer = vertexConsumer.initialize(buffers.get(material), material, ctx)) {
                    consumer.embeddium$setOffset(ctx.origin());
                    BlockRendererRegistry.RenderResult result = customRenderer.renderBlock(ctx, ctx.random(), consumer);
                    if (result == BlockRendererRegistry.RenderResult.OVERRIDE) {
                        return;
                    }
                }
            }
        }

        // Delegate FRAPI models to their pipeline
        if (this.fabricModelRenderingHandler != null && FRAPIModelUtils.isFRAPIModel(ctx.model())) {
            this.fabricModelRenderingHandler.reset();
            this.fabricModelRenderingHandler.renderEmbeddium(ctx, buffers, ctx.stack(), ctx.random());
            return;
        }

        int nullCullFaceFlags = defaultQuadRenderingFlags;

        var isMaterialSolid = material == buffers.getRenderPassConfiguration().defaultSolidMaterial();

        var encoder = buffers.get(material).getEncoder();

        for (Direction face : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> quads = this.getGeometry(ctx, face);

            if (!quads.isEmpty() && this.isFaceVisible(ctx, face)) {
                if (encoder instanceof ContextAwareChunkVertexEncoder contextAwareEncoder) {
                    contextAwareEncoder.prepareToRenderBlockFace(ctx, face);
                }
                this.quadRenderingFlags = defaultQuadRenderingFlags;
                this.renderQuadList(ctx, material, lighter, colorizer, renderOffset, buffers, meshBuilder, quads, face);
                // Make sure any flags that are turned off are also turned off for the null cullface
                nullCullFaceFlags &= this.quadRenderingFlags;
            }
        }

        List<BakedQuad> all = this.getGeometry(ctx, null);

        if (!all.isEmpty()) {
            if (encoder instanceof ContextAwareChunkVertexEncoder contextAwareEncoder) {
                contextAwareEncoder.prepareToRenderBlockFace(ctx, null);
            }
            this.quadRenderingFlags = nullCullFaceFlags;
            this.renderQuadList(ctx, material, lighter, colorizer, renderOffset, buffers, meshBuilder, all, null);
        }

        if (encoder instanceof ContextAwareChunkVertexEncoder contextAwareEncoder) {
            contextAwareEncoder.finishRenderingBlock();
        }
    }

    private List<BakedQuad> getGeometry(BlockRenderContext ctx, Direction face) {
        var random = ctx.random();
        random.setSeed(ctx.seed());

        return ctx.model().getQuads(ctx.state(), face, random/*? if forgelike && >=1.19 {*/, ctx.modelData(), ctx.renderLayer()/*?}*/ /*? if forgelike && <1.19 {*//*, ctx.modelData()*//*?}*/);
    }

    private boolean isFaceVisible(BlockRenderContext ctx, Direction face) {
        return this.occlusionCache.shouldDrawSide(ctx.state(), ctx.localSlice(), ctx.pos(), face);
    }

    private static int computeLightFlagMask(BakedQuadView quad) {
        int flag = 0;

        //? if forgelike && >=1.19 {
        if (quad.hasAmbientOcclusion()) {
            flag |= 1;
        }
        //?}

        //? if >=1.16 {
        if (quad.hasShade()) {
            flag |= 2;
        }
        //?}

        return flag;
    }

    private SpriteTransparencyLevel getQuadTransparencyLevel(BakedQuadView quad) {
        if ((quad.getFlags() & ModelQuadFlags.IS_PASS_OPTIMIZABLE) == 0 || quad.getSprite() == null) {
            return SpriteTransparencyLevel.TRANSLUCENT;
        }

        return SpriteTransparencyLevelHolder.getTransparencyLevel(quad.getSprite());
    }

    private void scanQuadsAndConfigureForRendering(List<BakedQuad> quads) {
        int quadsSize = quads.size();

        // By definition, singleton or empty lists of quads have a common config. Only check larger lists
        if (quadsSize >= 2) {
            // Disable reorienting if quads use different light configurations, as otherwise layered quads
            // may be triangulated differently from others in the stack, and that will cause z-fighting.
            int flagMask = -1;

            SpriteTransparencyLevel highestSeenLevel = SpriteTransparencyLevel.OPAQUE;

            // noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < quadsSize; i++) {
                var quad = BakedQuadView.of(quads.get(i));

                int newFlag = computeLightFlagMask(quad);
                if (flagMask == -1) {
                    flagMask = newFlag;
                } else if (newFlag != flagMask) {
                    // Disable reorienting
                    this.quadRenderingFlags &= ~USE_REORIENTING;
                }

                SpriteTransparencyLevel level = getQuadTransparencyLevel(quad);

                if (level.ordinal() < highestSeenLevel.ordinal()) {
                    // Downgrading will result in the quads being rendered in the wrong order, disable
                    this.quadRenderingFlags &= ~USE_RENDER_PASS_OPTIMIZATION;
                } else {
                    highestSeenLevel = level;
                }
            }
        }

        if (!this.isRenderPassOptEnabled) {
            this.quadRenderingFlags &= ~USE_RENDER_PASS_OPTIMIZATION;
        }
    }

    private Material chooseOptimalMaterial(Material defaultMaterial, RenderPassConfiguration<?> renderPassConfiguration, BakedQuadView quad) {
        if (defaultMaterial == renderPassConfiguration.defaultSolidMaterial() || (this.quadRenderingFlags & USE_RENDER_PASS_OPTIMIZATION) == 0 || (quad.getFlags() & ModelQuadFlags.IS_PASS_OPTIMIZABLE) == 0 || quad.getSprite() == null) {
            // No improvement possible
            return defaultMaterial;
        }

        SpriteTransparencyLevel level = SpriteTransparencyLevelHolder.getTransparencyLevel(quad.getSprite());

        if (level == SpriteTransparencyLevel.OPAQUE) {
            // Can use solid with no visual difference
            return renderPassConfiguration.defaultSolidMaterial();
        } else if (level == SpriteTransparencyLevel.TRANSPARENT && defaultMaterial == renderPassConfiguration.defaultTranslucentMaterial()) {
            // Can use cutout_mipped with no visual difference
            return renderPassConfiguration.defaultCutoutMippedMaterial();
        } else {
            // Have to use default
            return defaultMaterial;
        }
    }

    private void renderQuadList(BlockRenderContext ctx, Material material, LightPipeline lighter, ColorProvider<BlockState> colorizer, Vec3 offset,
                                ChunkBuildBuffers buffers, ChunkModelBuilder defaultBuilder, List<BakedQuad> quads, Direction cullFace) {

        scanQuadsAndConfigureForRendering(quads);

        var renderPassConfig = buffers.getRenderPassConfiguration();

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuadView quad = BakedQuadView.of(quads.get(i));

            final var lightData = this.getVertexLight(ctx, quad.hasAmbientOcclusion() ? lighter : this.lighters.getLighter(LightMode.FLAT), cullFace, quad);
            final var vertexColors = this.getVertexColors(ctx, colorizer, quad);

            var quadMaterial = this.chooseOptimalMaterial(material, renderPassConfig, quad);
            ChunkModelBuilder builder = (quadMaterial == material) ? defaultBuilder : buffers.get(quadMaterial);

            this.writeGeometry(ctx, builder, offset, quadMaterial, quad, vertexColors, lightData);

            TextureAtlasSprite sprite = quad.getSprite();

            if (SpriteUtil.hasAnimation(sprite) && builder.getSectionContextBundle() instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
                //noinspection unchecked
                ((Collection<TextureAtlasSprite>)mcData.animatedSprites).add(sprite);
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
        ModelQuadOrientation orientation = (this.quadRenderingFlags & USE_REORIENTING) != 0 ? ModelQuadOrientation.orientByBrightness(light.br, light.lm) : ModelQuadOrientation.NORMAL;
        var vertices = this.vertices;

        ModelQuadFacing normalFace = quad.getNormalFace();

        int vanillaNormal = DirectionUtil.PACKED_NORMALS[quad.getLightFace().ordinal()];
        int trueNormal = quad.getComputedFaceNormal();

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

            out.vanillaNormal = vanillaNormal;
            out.trueNormal = trueNormal;
        }

        var vertexBuffer = builder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material);
    }

    //? if forge || fabric {
    private boolean modelUsesAO(BlockRenderContext ctx) {
        //? if forge && >=1.19 {
        return ctx.model().useAmbientOcclusion(ctx.state(), ctx.renderLayer());
        //?} else if forge && >=1.18 {
        /*return ctx.model().useAmbientOcclusion(ctx.state());
        *///?} else if forge {
        /*return ctx.model().isAmbientOcclusion(ctx.state());
        *///?} else {
        /*return ctx.model().useAmbientOcclusion();
        *///?}
    }

    private LightMode getLightingMode(BlockRenderContext ctx) {
        var model = ctx.model();
        var state = ctx.state();
        if (this.useAmbientOcclusion && modelUsesAO(ctx)
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
