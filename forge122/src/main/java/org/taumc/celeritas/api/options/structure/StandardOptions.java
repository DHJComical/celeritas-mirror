package org.taumc.celeritas.api.options.structure;

import net.minecraft.util.ResourceLocation;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.impl.util.ResourceLocationUtil;

public final class StandardOptions {
    public static class Group {
        public static final ResourceLocation RENDERING = ResourceLocationUtil.make("minecraft", "rendering");
        public static final ResourceLocation WINDOW = ResourceLocationUtil.make("minecraft", "window");
        public static final ResourceLocation INDICATORS = ResourceLocationUtil.make("minecraft", "indicators");
        public static final ResourceLocation GRAPHICS = ResourceLocationUtil.make("minecraft", "graphics");
        public static final ResourceLocation MIPMAPS = ResourceLocationUtil.make("minecraft", "mipmaps");
        public static final ResourceLocation DETAILS = ResourceLocationUtil.make("minecraft", "details");
        public static final ResourceLocation CHUNK_UPDATES = ResourceLocationUtil.make("celeritas", "chunk_updates");
        public static final ResourceLocation RENDERING_CULLING = ResourceLocationUtil.make("celeritas", "rendering_culling");
        public static final ResourceLocation CPU_SAVING = ResourceLocationUtil.make("celeritas", "cpu_saving");
        public static final ResourceLocation SORTING = ResourceLocationUtil.make("celeritas", "sorting");
        public static final ResourceLocation LIGHTING = ResourceLocationUtil.make("celeritas", "lighting");
    }

    public static class Pages {
        public static final OptionIdentifier<Void> GENERAL = OptionIdentifier.create("celeritas", "general");
        public static final OptionIdentifier<Void> QUALITY = OptionIdentifier.create("celeritas", "quality");
        public static final OptionIdentifier<Void> PERFORMANCE = OptionIdentifier.create("celeritas", "performance");
        public static final OptionIdentifier<Void> ADVANCED = OptionIdentifier.create("celeritas", "advanced");
        public static final OptionIdentifier<Void> SHADERS = OptionIdentifier.create("celeritas", "shaders");
    }

    public static class Option {
        public static final ResourceLocation RENDER_DISTANCE = ResourceLocationUtil.make("minecraft", "render_distance");
        public static final ResourceLocation SIMULATION_DISTANCE = ResourceLocationUtil.make("minecraft", "simulation_distance");
        public static final ResourceLocation BRIGHTNESS = ResourceLocationUtil.make("minecraft", "brightness");
        public static final ResourceLocation GUI_SCALE = ResourceLocationUtil.make("minecraft", "gui_scale");
        public static final ResourceLocation FULLSCREEN = ResourceLocationUtil.make("minecraft", "fullscreen");
        public static final ResourceLocation FULLSCREEN_RESOLUTION = ResourceLocationUtil.make("minecraft", "fullscreen_resolution");
        public static final ResourceLocation VSYNC = ResourceLocationUtil.make("minecraft", "vsync");
        public static final ResourceLocation MAX_FRAMERATE = ResourceLocationUtil.make("minecraft", "max_frame_rate");
        public static final ResourceLocation VIEW_BOBBING = ResourceLocationUtil.make("minecraft", "view_bobbing");
        public static final ResourceLocation INACTIVITY_FPS_LIMIT = ResourceLocationUtil.make("minecraft", "inactivity_fps_limit");
        public static final ResourceLocation ATTACK_INDICATOR = ResourceLocationUtil.make("minecraft", "attack_indicator");
        public static final ResourceLocation AUTOSAVE_INDICATOR = ResourceLocationUtil.make("minecraft", "autosave_indicator");
        public static final ResourceLocation GRAPHICS_MODE = ResourceLocationUtil.make("minecraft", "graphics_mode");
        public static final ResourceLocation CLOUDS = ResourceLocationUtil.make("minecraft", "clouds");
        public static final ResourceLocation WEATHER = ResourceLocationUtil.make("minecraft", "weather");
        public static final ResourceLocation LEAVES = ResourceLocationUtil.make("minecraft", "leaves");
        public static final ResourceLocation PARTICLES = ResourceLocationUtil.make("minecraft", "particles");
        public static final ResourceLocation SMOOTH_LIGHT = ResourceLocationUtil.make("minecraft", "smooth_lighting");
        public static final ResourceLocation BIOME_BLEND = ResourceLocationUtil.make("minecraft", "biome_blend");
        public static final ResourceLocation ENTITY_DISTANCE = ResourceLocationUtil.make("minecraft", "entity_distance");
        public static final ResourceLocation ENTITY_SHADOWS = ResourceLocationUtil.make("minecraft", "entity_shadows");
        public static final ResourceLocation VIGNETTE = ResourceLocationUtil.make("minecraft", "vignette");
        public static final ResourceLocation MIPMAP_LEVEL = ResourceLocationUtil.make("minecraft", "mipmap_levels");
        public static final ResourceLocation CHUNK_UPDATE_THREADS = ResourceLocationUtil.make("celeritas", "chunk_update_threads");
        public static final ResourceLocation DEFFER_CHUNK_UPDATES = ResourceLocationUtil.make("celeritas", "defer_chunk_updates");
        public static final ResourceLocation BLOCK_FACE_CULLING = ResourceLocationUtil.make("celeritas", "block_face_culling");
        public static final ResourceLocation COMPACT_VERTEX_FORMAT = ResourceLocationUtil.make("celeritas", "compact_vertex_format");
        public static final ResourceLocation FOG_OCCLUSION = ResourceLocationUtil.make("celeritas", "fog_occlusion");
        public static final ResourceLocation ENTITY_CULLING = ResourceLocationUtil.make("celeritas", "entity_culling");
        public static final ResourceLocation ANIMATE_VISIBLE_TEXTURES = ResourceLocationUtil.make("celeritas", "animate_only_visible_textures");
        public static final ResourceLocation NO_ERROR_CONTEXT = ResourceLocationUtil.make("celeritas", "no_error_context");
        public static final ResourceLocation PERSISTENT_MAPPING = ResourceLocationUtil.make("celeritas", "persistent_mapping");
        public static final ResourceLocation CPU_FRAMES_AHEAD = ResourceLocationUtil.make("celeritas", "cpu_render_ahead_limit");
        public static final ResourceLocation TRANSLUCENT_FACE_SORTING = ResourceLocationUtil.make("celeritas", "translucent_face_sorting");
        public static final ResourceLocation USE_QUAD_NORMALS_FOR_LIGHTING = ResourceLocationUtil.make("celeritas", "use_quad_normals_for_lighting");
        public static final ResourceLocation RENDER_PASS_OPTIMIZATION = ResourceLocationUtil.make("celeritas", "render_pass_optimization");
        public static final ResourceLocation RENDER_PASS_CONSOLIDATION = ResourceLocationUtil.make("celeritas", "render_pass_consolidation");
        public static final ResourceLocation USE_FASTER_CLOUDS = ResourceLocationUtil.make("celeritas", "use_faster_clouds");
        public static final ResourceLocation ASYNC_GRAPH_SEARCH = ResourceLocationUtil.make("celeritas", "async_graph_search");
    }
}
