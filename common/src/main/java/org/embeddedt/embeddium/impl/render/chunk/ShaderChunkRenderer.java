package org.embeddedt.embeddium.impl.render.chunk;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.shader.*;
import org.embeddedt.embeddium.impl.render.chunk.shader.*;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private static final Logger LOGGER = LogManager.getLogger(ShaderChunkRenderer.class);

    private final Map<ChunkShaderOptions, @Nullable GlProgram<ChunkShaderInterface>> programs = new Object2ObjectOpenHashMap<>();

    protected final RenderPassConfiguration<?> renderPassConfiguration;

    protected final RenderDevice device;

    protected GlProgram<ChunkShaderInterface> activeProgram;

    public ShaderChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
        this.device = device;
        this.renderPassConfiguration = renderPassConfiguration;
    }

    protected @Nullable GlProgram<ChunkShaderInterface> compileProgram(ChunkShaderOptions options) {
        GlProgram<ChunkShaderInterface> program = this.programs.get(options);

        if (program == null && !this.programs.containsKey(options)) {
            try {
                program = this.createShader("blocks/block_layer_opaque", options);
            } catch(Exception e) {
                LOGGER.error("There was an error creating a chunk program. Terrain will not render until this is fixed.", e);
            }
            this.programs.put(options, program);
        }

        return program;
    }

    protected GlProgram<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX,
                "sodium:" + path + ".vsh", constants);
        
        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT,
                "sodium:" + path + ".fsh", constants);

        try {
            var builder = GlProgram.builder("sodium:chunk_shader").attachShader(vertShader).attachShader(fragShader);
            int i = 0;
            for (var attr : options.vertexType().getVertexFormat().getAttributes()) {
                builder.bindAttribute(attr.getName(), i++);
            }
            builder.bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR);
            return builder.link((shader) -> new DefaultChunkShaderInterface(shader, options));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected void begin(TerrainRenderPass pass) {
        pass.startDrawing();

        ChunkShaderOptions options = new ChunkShaderOptions(ChunkShaderFogComponent.FOG_SERVICE.getFogMode(), pass, this.renderPassConfiguration.getVertexTypeForPass(pass));

        this.activeProgram = this.compileProgram(options);

        if (this.activeProgram != null) {
            this.activeProgram.bind();
            this.activeProgram.getInterface()
                    .setupState(pass);
        }
    }

    protected void end(TerrainRenderPass pass) {
        if (this.activeProgram != null) {
            this.activeProgram.getInterface().restoreState();
            this.activeProgram.unbind();
            this.activeProgram = null;
        }


        pass.endDrawing();
    }

    @Override
    public void delete(CommandList commandList) {
        this.programs.values().stream().filter(Objects::nonNull)
                .forEach(GlProgram::delete);
    }

    @Override
    public RenderPassConfiguration<?> getRenderPassConfiguration() {
        return this.renderPassConfiguration;
    }
}
