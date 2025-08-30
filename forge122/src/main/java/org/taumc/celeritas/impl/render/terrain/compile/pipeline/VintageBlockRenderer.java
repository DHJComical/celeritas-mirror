package org.taumc.celeritas.impl.render.terrain.compile.pipeline;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.model.pipeline.VertexBufferConsumer;
import net.minecraftforge.registries.IRegistryDelegate;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadOrientation;
import org.embeddedt.embeddium.impl.render.chunk.ChunkColorWriter;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.taumc.celeritas.impl.render.terrain.compile.VintageChunkBuildContext;
import org.taumc.celeritas.impl.render.terrain.compile.light.LightDataCache;
import org.taumc.celeritas.impl.render.terrain.compile.light.VintageDiffuseProvider;
import org.taumc.celeritas.impl.world.cloned.CeleritasBlockAccess;
import org.taumc.celeritas.mixin.core.terrain.BlockColorsAccessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VintageBlockRenderer {
    private final BlockModelShapes shapes;
    private final VintageChunkBuildContext context;
    private final VertexBufferConsumer consumer;
    private final LightPipelineProvider lighters;
    private final Map<IRegistryDelegate<Block>, IBlockColor> blockColors;

    private final QuadLightData quadLightData = new QuadLightData();
    private final int[] quadColors = new int[4];
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private IBlockState currentState;
    private CeleritasBlockAccess currentBlockAccess;

    public VintageBlockRenderer(VintageChunkBuildContext context, LightDataCache cache) {
        this.shapes = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
        this.consumer = new VertexBufferConsumer();
        this.context = context;
        this.lighters = new LightPipelineProvider(cache, VintageDiffuseProvider.INSTANCE, true);
        this.blockColors = ((BlockColorsAccessor)Minecraft.getMinecraft().getBlockColors()).getBlockColorMap();
    }

    public void renderBlock(IBlockState state, BlockPos pos, CeleritasBlockAccess blockAccess, BlockRenderLayer layer) {
        if (blockAccess.getWorldType() != WorldType.DEBUG_ALL_BLOCK_STATES) {
            state = state.getActualState(blockAccess, pos);
        }
        var model = this.shapes.getModelForState(state);
        state = state.getBlock().getExtendedState(state, blockAccess, pos);
        this.currentState = state;
        this.currentBlockAccess = blockAccess;

        var buffers = this.context.buffers;
        var buffer = buffers.get(buffers.getRenderPassConfiguration().getMaterialForRenderType(layer));

        long rand = MathHelper.getPositionRandom(pos);

        IBlockColor colorProvider = this.blockColors.get(state.getBlock().delegate);

        var offset = this.currentState.getOffset(blockAccess, pos);

        var aoEnabled = Minecraft.isAmbientOcclusionEnabled() && state.getLightValue(blockAccess, pos) == 0 && model.isAmbientOcclusion(state);

        var lighter = this.lighters.getLighter(aoEnabled ? LightMode.SMOOTH : LightMode.FLAT);

        for (var dir : EnumFacing.VALUES) {
            var quads = model.getQuads(state, dir, rand);

            if (quads.isEmpty() || !state.shouldSideBeRendered(blockAccess, pos, dir)) {
                continue;
            }

            renderQuadList(buffer, buffers, layer, pos, dir, lighter, colorProvider, offset, quads);
        }

        var quads = model.getQuads(state, null, rand);

        if (!quads.isEmpty()) {
            renderQuadList(buffer, buffers, layer, pos, null, lighter, colorProvider, offset, quads);
        }

        this.currentBlockAccess = null;
    }

    private QuadLightData getVertexLight(LightPipeline lighter, BlockPos pos, EnumFacing cullFace, BakedQuadView quad) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, pos.getX(), pos.getY(), pos.getZ(), light, VintageDiffuseProvider.fromEnumFacingOrUnassigned(cullFace), quad.getLightFace(), quad.hasShade());

        return light;
    }

    private int[] getVertexColors(BlockPos pos, IBlockColor colorProvider, BakedQuadView quad) {
        final int[] vertexColors = this.quadColors;

        if (colorProvider != null && quad.hasColor()) {
            // Force full alpha on all colors
            Arrays.fill(vertexColors, 0xFF000000 | ColorARGB.toABGR(colorProvider.colorMultiplier(this.currentState, this.currentBlockAccess, pos, quad.getColorIndex())));
        } else {
            Arrays.fill(vertexColors, 0xFFFFFFFF);
        }

        return vertexColors;
    }

    private void renderQuadList(ChunkModelBuilder defaultBuffer, ChunkBuildBuffers buffers,
                                BlockRenderLayer layer, BlockPos pos, EnumFacing cullFace,
                                LightPipeline lighter, IBlockColor colorProvider, Vec3d offset,
                                List<BakedQuad> quads) {
        int localX = pos.getX() & 15, localY = pos.getY() & 15, localZ = pos.getZ() & 15;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            var quad = quads.get(i);
            var format = quad.getFormat();
            if (format != DefaultVertexFormats.BLOCK && format != DefaultVertexFormats.ITEM) {
                throw new IllegalStateException("Unexpected vertex format: " + format);
            }

            BakedQuadView quadView = (BakedQuadView)quad;

            // Run through our light pipeline
            var light = this.getVertexLight(lighter, pos, cullFace, quadView);
            var colors = this.getVertexColors(pos, colorProvider, quadView);
            this.writeGeometry(localX, localY, localZ, defaultBuffer, offset,
                    buffers.getRenderPassConfiguration().getMaterialForRenderType(layer),
                    quadView, colors, light);
        }
    }

    private void writeGeometry(int localX, int localY, int localZ, ChunkModelBuilder builder,
                               Vec3d offset,
                               Material material,
                               BakedQuadView quad,
                               int[] colors,
                               QuadLightData light)
    {
        ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(light.br, light.lm);
        var vertices = this.vertices;

        ModelQuadFacing normalFace = quad.getNormalFace();

        int vanillaNormal = normalFace.getPackedNormal();
        int trueNormal = quad.getComputedFaceNormal();

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            var out = vertices[dstIndex];
            out.x = localX + quad.getX(srcIndex) + (float) offset.x;
            out.y = localY + quad.getY(srcIndex) + (float) offset.y;
            out.z = localZ + quad.getZ(srcIndex) + (float) offset.z;

            out.color = ChunkColorWriter.EMBEDDIUM.writeColor(ModelQuadUtil.mixARGBColors(colors[srcIndex], quad.getColor(srcIndex)), light.br[srcIndex]);

            out.u = quad.getTexU(srcIndex);
            out.v = quad.getTexV(srcIndex);

            out.light = ModelQuadUtil.mergeBakedLight(quad.getLight(srcIndex), quad.getVanillaLightEmission(), light.lm[srcIndex]);

            out.vanillaNormal = vanillaNormal;
            out.trueNormal = trueNormal;
        }

        var vertexBuffer = builder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material);
    }
}
