package org.embeddedt.embeddium.api.options.structure;

import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.Celeritas;

public final class StandardOptions {
    public static class Group {
        public static final OptionIdentifier<Void> RENDERING = OptionIdentifier.create("minecraft", "rendering");
        public static final OptionIdentifier<Void> WINDOW = OptionIdentifier.create("minecraft", "window");
        public static final OptionIdentifier<Void> INDICATORS = OptionIdentifier.create("minecraft", "indicators");
        public static final OptionIdentifier<Void> GRAPHICS = OptionIdentifier.create("minecraft", "graphics");
        public static final OptionIdentifier<Void> MIPMAPS = OptionIdentifier.create("minecraft", "mipmaps");
        public static final OptionIdentifier<Void> DETAILS = OptionIdentifier.create("minecraft", "details");
        public static final OptionIdentifier<Void> CHUNK_UPDATES = OptionIdentifier.create(Celeritas.MODID, "chunk_updates");
        public static final OptionIdentifier<Void> RENDERING_CULLING = OptionIdentifier.create(Celeritas.MODID, "rendering_culling");
        public static final OptionIdentifier<Void> CPU_SAVING = OptionIdentifier.create(Celeritas.MODID, "cpu_saving");
        public static final OptionIdentifier<Void> SORTING = OptionIdentifier.create(Celeritas.MODID, "sorting");
        public static final OptionIdentifier<Void> LIGHTING = OptionIdentifier.create(Celeritas.MODID, "lighting");
    }

    public static class Pages {
        public static final OptionIdentifier<Void> GENERAL = OptionIdentifier.create(Celeritas.MODID, "general");
        public static final OptionIdentifier<Void> QUALITY = OptionIdentifier.create(Celeritas.MODID, "quality");
        public static final OptionIdentifier<Void> PERFORMANCE = OptionIdentifier.create(Celeritas.MODID, "performance");
        public static final OptionIdentifier<Void> ADVANCED = OptionIdentifier.create(Celeritas.MODID, "advanced");
        public static final OptionIdentifier<Void> SHADERS = OptionIdentifier.create(Celeritas.MODID, "shaders");
    }

    public static class Option {
        public static final OptionIdentifier<Void> RENDER_DISTANCE = OptionIdentifier.create("minecraft", "render_distance");
        public static final OptionIdentifier<Void> SIMULATION_DISTANCE = OptionIdentifier.create("minecraft", "simulation_distance");
        public static final OptionIdentifier<Void> BRIGHTNESS = OptionIdentifier.create("minecraft", "brightness");
        public static final OptionIdentifier<Void> GUI_SCALE = OptionIdentifier.create("minecraft", "gui_scale");
        public static final OptionIdentifier<Void> FULLSCREEN = OptionIdentifier.create("minecraft", "fullscreen");
        public static final OptionIdentifier<Void> FULLSCREEN_RESOLUTION = OptionIdentifier.create("minecraft", "fullscreen_resolution");
        public static final OptionIdentifier<Void> VSYNC = OptionIdentifier.create("minecraft", "vsync");
        public static final OptionIdentifier<Void> MAX_FRAMERATE = OptionIdentifier.create("minecraft", "max_frame_rate");
        public static final OptionIdentifier<Void> VIEW_BOBBING = OptionIdentifier.create("minecraft", "view_bobbing");
        public static final OptionIdentifier<Void> INACTIVITY_FPS_LIMIT = OptionIdentifier.create("minecraft", "inactivity_fps_limit");
        public static final OptionIdentifier<Void> ATTACK_INDICATOR = OptionIdentifier.create("minecraft", "attack_indicator");
        public static final OptionIdentifier<Void> AUTOSAVE_INDICATOR = OptionIdentifier.create("minecraft", "autosave_indicator");
        public static final OptionIdentifier<Void> GRAPHICS_MODE = OptionIdentifier.create("minecraft", "graphics_mode");
        public static final OptionIdentifier<Void> CLOUDS = OptionIdentifier.create("minecraft", "clouds");
        public static final OptionIdentifier<Void> WEATHER = OptionIdentifier.create("minecraft", "weather");
        public static final OptionIdentifier<Void> LEAVES = OptionIdentifier.create("minecraft", "leaves");
        public static final OptionIdentifier<Void> PARTICLES = OptionIdentifier.create("minecraft", "particles");
        public static final OptionIdentifier<Void> SMOOTH_LIGHT = OptionIdentifier.create("minecraft", "smooth_lighting");
        public static final OptionIdentifier<Void> BIOME_BLEND = OptionIdentifier.create("minecraft", "biome_blend");
        public static final OptionIdentifier<Void> ENTITY_DISTANCE = OptionIdentifier.create("minecraft", "entity_distance");
        public static final OptionIdentifier<Void> ENTITY_SHADOWS = OptionIdentifier.create("minecraft", "entity_shadows");
        public static final OptionIdentifier<Void> VIGNETTE = OptionIdentifier.create("minecraft", "vignette");
        public static final OptionIdentifier<Void> MIPMAP_LEVEL = OptionIdentifier.create("minecraft", "mipmap_levels");
        public static final OptionIdentifier<Void> CHUNK_UPDATE_THREADS = OptionIdentifier.create(Celeritas.MODID, "chunk_update_threads");
        public static final OptionIdentifier<Void> DEFFER_CHUNK_UPDATES = OptionIdentifier.create(Celeritas.MODID, "defer_chunk_updates");
        public static final OptionIdentifier<Void> BLOCK_FACE_CULLING = OptionIdentifier.create(Celeritas.MODID, "block_face_culling");
        public static final OptionIdentifier<Void> COMPACT_VERTEX_FORMAT = OptionIdentifier.create(Celeritas.MODID, "compact_vertex_format");
        public static final OptionIdentifier<Void> FOG_OCCLUSION = OptionIdentifier.create(Celeritas.MODID, "fog_occlusion");
        public static final OptionIdentifier<Void> ENTITY_CULLING = OptionIdentifier.create(Celeritas.MODID, "entity_culling");
        public static final OptionIdentifier<Void> ANIMATE_VISIBLE_TEXTURES = OptionIdentifier.create(Celeritas.MODID, "animate_only_visible_textures");
        public static final OptionIdentifier<Void> NO_ERROR_CONTEXT = OptionIdentifier.create(Celeritas.MODID, "no_error_context");
        public static final OptionIdentifier<Void> PERSISTENT_MAPPING = OptionIdentifier.create(Celeritas.MODID, "persistent_mapping");
        public static final OptionIdentifier<Void> CPU_FRAMES_AHEAD = OptionIdentifier.create(Celeritas.MODID, "cpu_render_ahead_limit");
        public static final OptionIdentifier<Void> TRANSLUCENT_FACE_SORTING = OptionIdentifier.create(Celeritas.MODID, "translucent_face_sorting");
        public static final OptionIdentifier<Void> USE_QUAD_NORMALS_FOR_LIGHTING = OptionIdentifier.create(Celeritas.MODID, "use_quad_normals_for_lighting");
        public static final OptionIdentifier<Void> RENDER_PASS_OPTIMIZATION = OptionIdentifier.create(Celeritas.MODID, "render_pass_optimization");
        public static final OptionIdentifier<Void> RENDER_PASS_CONSOLIDATION = OptionIdentifier.create(Celeritas.MODID, "render_pass_consolidation");
        public static final OptionIdentifier<Void> USE_FASTER_CLOUDS = OptionIdentifier.create(Celeritas.MODID, "use_faster_clouds");
        public static final OptionIdentifier<Void> ASYNC_GRAPH_SEARCH = OptionIdentifier.create(Celeritas.MODID, "async_graph_search");
        public static final OptionIdentifier<Void> CHUNK_FADE_IN_DURATION = OptionIdentifier.create(Celeritas.MODID, "chunk_fade_in_duration");
    }
}
