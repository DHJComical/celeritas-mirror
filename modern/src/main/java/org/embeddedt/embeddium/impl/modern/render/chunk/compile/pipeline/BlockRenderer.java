package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

//? if shaders
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
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
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.modern.render.chunk.ContextAwareChunkVertexEncoder;
import org.embeddedt.embeddium.impl.modern.render.chunk.MojangVertexConsumer;
import org.embeddedt.embeddium.impl.modern.util.ModernQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BakedQuadGroupAnalyzer;
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
//? if <1.21.11 {
import org.embeddedt.embeddium.api.BlockRendererRegistry;
import org.embeddedt.embeddium.api.model.EmbeddiumBakedModelExtension;
//?}
import org.embeddedt.embeddium.impl.render.chunk.ChunkColorWriter;
//? if ffapi {
import org.embeddedt.embeddium.impl.render.frapi.FRAPIModelUtils;
import org.embeddedt.embeddium.impl.render.frapi.FRAPIRenderHandler;
//?}
//? if ffapi && >=1.20
import org.embeddedt.embeddium.impl.render.frapi.IndigoBlockRenderContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BakedQuadGroupAnalyzer.*;

/**
 * The Embeddium equivalent to vanilla's ModelBlockRenderer. This is the primary component of the chunk meshing logic;
 * it is responsible for accepting {@link BlockRenderContext} and generating the appropriate geometry.
 * <p>
 * This class does not need to be thread-safe, as a separate instance is allocated per meshing thread.
 */
public class BlockRenderer {
    private final BlockRendererConfig config;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData quadLightData = new QuadLightData();

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private final boolean useAmbientOcclusion;

    private final int[] quadColors = new int[4];

    //? if <1.21.11 {
    /**
     * The list of registered custom block renderers. These may augment or fully bypass the model system for the
     * block.
     */
    private final List<BlockRendererRegistry.Renderer> customRenderers = new it.unimi.dsi.fastutil.objects.ObjectArrayList<>();
    //?}

    //? if ffapi
    private final FRAPIRenderHandler fabricModelRenderingHandler;

    private final ChunkColorWriter colorEncoder;

    private final boolean isRenderPassOptEnabled;
    private final MojangVertexConsumer vertexConsumer = new MojangVertexConsumer();
    private final BakedQuadGroupAnalyzer analyzer = new BakedQuadGroupAnalyzer();

    private final ArrayList<ContextAwareChunkVertexEncoder> usedContextEncoders = new ArrayList<>(4);

    private Material defaultMaterial;

    public record BlockRendererConfig(ColorProviderRegistry colorProviderRegistry, LightPipelineProvider lighters
                                      //? if shaders
                                      , @Nullable Map<net.minecraft.world.level.block.Block, net.minecraft.client.renderer.RenderType> renderTypeOverrides
    ) {}

    public BlockRenderer(BlockRendererConfig config) {
        this.config = config;

        this.occlusionCache = new BlockOcclusionCache();
        //? if <26.1 {
        this.useAmbientOcclusion = Minecraft.useAmbientOcclusion();
        //?} else
        /*this.useAmbientOcclusion = Minecraft.getInstance().options.ambientOcclusion().get();*/
        //? if ffapi && >=1.20 {
        this.fabricModelRenderingHandler = FRAPIRenderHandler.INDIGO_PRESENT ? new IndigoBlockRenderContext(this.occlusionCache, config.lighters.getLightData()) : null;
        //?} else if ffapi {
        /*this.fabricModelRenderingHandler = null;
        *///?}
        this.isRenderPassOptEnabled = Celeritas.options().performance.useRenderPassOptimization;

        //? if shaders {
        this.colorEncoder = WorldRenderingSettings.INSTANCE.shouldUseSeparateAo() ? ChunkColorWriter.SEPARATE_AO : ChunkColorWriter.EMBEDDIUM;
        //?} else {
        /*this.colorEncoder = ChunkColorWriter.EMBEDDIUM;
        *///?}
    }

    /**
     * Renders all geometry for a block into the given chunk build buffers.
     * @param ctx the context for the current block being rendered
     * @param buffers the buffer to output geometry to
     */
    public void renderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers) {
        int defaultQuadRenderingFlags = USE_ALL_THINGS;

        if (!isRenderPassOptEnabled) {
            defaultQuadRenderingFlags &= ~USE_RENDER_PASS_OPTIMIZATION;
        }

        var config = this.config;

        //? if <26.1 {
        var blockRenderType = ctx.renderLayer();
        //? if shaders {
        if (config.renderTypeOverrides() != null) {
            var type = config.renderTypeOverrides.get(ctx.state().getBlock());
            if (type != null) {
                blockRenderType = type;
                defaultQuadRenderingFlags &= ~USE_RENDER_PASS_OPTIMIZATION;
            }
        }
        //?}
        this.defaultMaterial = buffers.getRenderPassConfiguration().getMaterialForRenderType(blockRenderType);
        //?}

        this.analyzer.setDefaultRenderingFlags(defaultQuadRenderingFlags);

        ColorProvider<BlockState> colorizer = config.colorProviderRegistry.getColorProvider(ctx.state().getBlock());

        LightMode mode = this.getLightingMode(ctx);
        LightPipeline lighter = config.lighters.getLighter(mode);
        Vec3 renderOffset;

        //? if >=1.21.11 {
        /*renderOffset = ctx.state().getOffset(ctx.pos());
        *///?} else if >=1.20 <1.21.11 {
        if (ctx.state().hasOffsetFunction()) {
            renderOffset = ctx.state().getOffset(ctx.localSlice(), ctx.pos());
        } else {
            renderOffset = Vec3.ZERO;
        }
        //?} else
        /*renderOffset = ctx.state().getOffset(ctx.localSlice(), ctx.pos());*/

        //? if <1.21.11 {
        // Process custom renderers
        customRenderers.clear();
        BlockRendererRegistry.instance().fillCustomRenderers(customRenderers, ctx);

        if(!customRenderers.isEmpty()) {
            var material = this.defaultMaterial;
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
        //?}

        //? if ffapi {
        // Delegate FRAPI models to their pipeline
        if (this.fabricModelRenderingHandler != null && FRAPIModelUtils.isFRAPIModel(ctx.model())) {
            this.fabricModelRenderingHandler.reset();
            this.fabricModelRenderingHandler.renderEmbeddium(ctx, buffers, ctx.stack(), ctx.random());
            return;
        }
        //?}

        for (Direction face : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> quads = this.getGeometry(ctx, face);

            if (!quads.isEmpty() && this.isFaceVisible(ctx, face)) {
                int flags = this.analyzer.getFlagsForRendering(ModernQuadFacing.fromDirection(face), BakedQuadView.ofList(quads));
                this.renderQuadList(ctx, lighter, colorizer, renderOffset, buffers, quads, face, flags);
            }
        }

        List<BakedQuad> all = this.getGeometry(ctx, null);

        if (!all.isEmpty()) {
            int flags = this.analyzer.getFlagsForRendering(ModelQuadFacing.UNASSIGNED, BakedQuadView.ofList(all));
            this.renderQuadList(ctx, lighter, colorizer, renderOffset, buffers, all, null, flags);
        }

        usedContextEncoders.forEach(ContextAwareChunkVertexEncoder::finishRenderingBlock);
        usedContextEncoders.clear();
    }

    private List<BakedQuad> getGeometry(BlockRenderContext ctx, Direction face) {
        //? if <1.21.11 {
        var random = ctx.random();
        random.setSeed(ctx.seed());

        return ctx.model().getQuads(ctx.state(), face, random/*? if forgelike && >=1.19 {*/, ctx.modelData(), ctx.renderLayer()/*?}*/ /*? if forgelike && <1.19 {*//*, ctx.modelData()*//*?}*/);
        //?} else
        /*return ctx.model().getQuads(face);*/
    }

    private boolean isFaceVisible(BlockRenderContext ctx, Direction face) {
        return this.occlusionCache.shouldDrawSide(ctx.state(), ctx.localSlice(), ctx.pos(), face);
    }

    private void renderQuadList(BlockRenderContext ctx, LightPipeline lighter, ColorProvider<BlockState> colorizer, Vec3 offset,
                                ChunkBuildBuffers buffers, List<BakedQuad> quads, Direction cullFace, int flags) {
        var renderPassConfig = buffers.getRenderPassConfiguration();

        boolean reorient = (flags & USE_REORIENTING) != 0;

        var material = this.defaultMaterial;
        var usedContextEncoders = this.usedContextEncoders;

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuadView quad = BakedQuadView.of(quads.get(i));

            final var lightData = this.getVertexLight(ctx, quad.hasAmbientOcclusion() ? lighter : config.lighters.getLighter(LightMode.FLAT), cullFace, quad);
            final var vertexColors = this.getVertexColors(ctx, colorizer, quad);

            //? if <26.1 {
            var quadMaterial = BakedQuadGroupAnalyzer.chooseOptimalMaterial(flags, material, renderPassConfig, quad);
            //?} else {
            /*var quadMaterial = switch (quad.getTransparencyLevel()) {
                case OPAQUE -> renderPassConfig.defaultSolidMaterial();
                case TRANSPARENT -> renderPassConfig.defaultCutoutMippedMaterial();
                case TRANSLUCENT -> renderPassConfig.defaultTranslucentMaterial();
            };
            *///?}
            ChunkModelBuilder builder = buffers.get(quadMaterial);

            if (builder.getEncoder() instanceof ContextAwareChunkVertexEncoder encoder) {
                if (!usedContextEncoders.contains(encoder)) {
                    usedContextEncoders.add(encoder);
                }
                encoder.prepareToRenderBlockFace(ctx, cullFace);
            }

            this.writeGeometry(ctx, builder, offset, quadMaterial, quad, vertexColors, lightData, reorient);

            TextureAtlasSprite sprite = (TextureAtlasSprite)quad.celeritas$getSprite();

            if (SpriteUtil.hasAnimation(sprite) && builder.getSectionContextBundle() instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
                //noinspection unchecked
                ((Collection<TextureAtlasSprite>)mcData.animatedSprites).add(sprite);
            }
        }

        if (colorizer != null) {
            colorizer.reset();
        }
    }

    private QuadLightData getVertexLight(BlockRenderContext ctx, LightPipeline lighter, Direction cullFace, BakedQuadView quad) {
        QuadLightData light = this.quadLightData;
        var pos = ctx.pos();
        lighter.calculate(quad, pos.getX(), pos.getY(), pos.getZ(), light, ModernQuadFacing.fromDirectionOrUnassigned(cullFace), quad.getLightFace(), quad.hasShade(), false);

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
                               QuadLightData light,
                               boolean reorient)
    {
        ModelQuadOrientation orientation = reorient ? ModelQuadOrientation.orientByBrightness(light.br, light.lm) : ModelQuadOrientation.NORMAL;
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
                && (
                        ((EmbeddiumBakedModelExtension)model).useAmbientOcclusionWithLightEmission(state, ctx.renderLayer())
                   || ctx.lightEmission() == 0)) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
    //?} else {
    /*private LightMode getLightingMode(BlockRenderContext ctx) {
        var model = ctx.model();
        //? if <1.21.11 {
        var state = ctx.state();
        var aoTristate = model.useAmbientOcclusion(state, ctx.modelData(), ctx.renderLayer());
        //?} else
        /^var aoTristate = model.ambientOcclusion();^/
        boolean canBeSmooth = this.useAmbientOcclusion && switch(aoTristate) {
            case TRUE -> true;
            case DEFAULT -> ctx.lightEmission() == 0;
            case FALSE -> false;
        };
        return canBeSmooth ? LightMode.SMOOTH : LightMode.FLAT;
    }
    *///?}
}
