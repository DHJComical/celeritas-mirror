package net.irisshaders.iris.compat.flywheel.compile;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.compile.component.SsboInstanceComponent;
import dev.engine_room.flywheel.backend.compile.component.BufferTextureInstanceComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

public class ClrwlPipelines
{
    public static final ResourceLocation INSTANCING_MAIN_VERT = Colorwheel.rl("internal/instancing/main.vert");
    public static final ResourceLocation INSTANCING_MAIN_GEOM = Colorwheel.rl("internal/instancing/main_geom.glsl");
    public static final ResourceLocation INSTANCING_MAIN_FRAG = Colorwheel.rl("internal/instancing/main.frag");

    public static final ResourceLocation INDIRECT_MAIN_VERT = Colorwheel.rl("internal/indirect/main.vert");
    public static final ResourceLocation INDIRECT_MAIN_GEOM = Colorwheel.rl("internal/indirect/main_geom.glsl");
    public static final ResourceLocation INDIRECT_MAIN_FRAG = Colorwheel.rl("internal/indirect/main.frag");

    private static final PipelineBuilder INSTANCING_BUILDER = PipelineBuilder.builder()
            .id("instancing")
            .extensions(ClrwlInstancedPrograms.EXTENSIONS)
            .assembler(BufferTextureInstanceComponent::new)
            .vertex(INSTANCING_MAIN_VERT)
            .geometry(INSTANCING_MAIN_GEOM)
            .fragment(INSTANCING_MAIN_FRAG);

    private static final PipelineBuilder INDIRECT_BUILDER = PipelineBuilder.builder()
            .id("indirect")
            .extensions(ClrwlIndirectPrograms.EXTENSIONS)
            .assembler(SsboInstanceComponent::new)
            .vertex(INDIRECT_MAIN_VERT)
            .geometry(INDIRECT_MAIN_GEOM)
            .fragment(INDIRECT_MAIN_FRAG);

    public static final ClrwlPrograms.Pipeline INSTANCING = INSTANCING_BUILDER.build(false);
    public static final ClrwlPrograms.Pipeline INSTANCING_FALLBACK = INSTANCING_BUILDER.build(true);

    public static final ClrwlPrograms.Pipeline INDIRECT = INDIRECT_BUILDER.build(false);
    public static final ClrwlPrograms.Pipeline INDIRECT_FALLBACK = INDIRECT_BUILDER.build(true);

    private static class PipelineBuilder
    {
        private String id;
        private Collection<String> extensions;
        private ClrwlPrograms.Pipeline.InstanceAssembler assembler;
        private ResourceLocation vertex;
        private ResourceLocation geometry;
        private ResourceLocation fragment;

        static PipelineBuilder builder()
        {
            return new PipelineBuilder();
        }

        public PipelineBuilder id(String id)
        {
            this.id = id;
            return this;
        }

        public PipelineBuilder extensions(Collection<String> extensions)
        {
            this.extensions = extensions;
            return this;
        }

        public PipelineBuilder assembler(ClrwlPrograms.Pipeline.InstanceAssembler assembler)
        {
            this.assembler = assembler;
            return this;
        }

        public PipelineBuilder vertex(ResourceLocation vertex)
        {
            this.vertex = vertex;
            return this;
        }

        public PipelineBuilder geometry(ResourceLocation geometry)
        {
            this.geometry = geometry;
            return this;
        }

        public PipelineBuilder fragment(ResourceLocation fragment)
        {
            this.fragment = fragment;
            return this;
        }

        public ClrwlPrograms.Pipeline build(boolean fallback)
        {
            return new ClrwlPrograms.Pipeline(id, fallback, extensions, assembler, vertex, geometry, fragment);
        }
    }
}
