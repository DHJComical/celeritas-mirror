// The position of the vertex around the model origin
vec3 _vert_position;

// The block texture coordinate of the vertex
vec2 _vert_tex_diffuse_coord;

// The light texture coordinate of the vertex
ivec2 _vert_tex_light_coord;

// The color of the vertex
vec4 _vert_color;

// The "rdh factor" of the vertex, used when applying AO correction
vec4 _vert_rdh_factor;

// The index of the draw command which this vertex belongs to
uint _draw_id;

// The material bits for the primitive
uint _material_params;

#define REGION_SIZE 256

uvec3 _get_relative_chunk_coord(uint pos) {
    // Packing scheme is defined by LocalSectionIndex
    return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
}

vec3 _get_draw_translation(uint pos) {
    return _get_relative_chunk_coord(pos) * vec3(16.0);
}

#import <sodium:generated/vertex_fetch.glsl>

vec4 _get_rdh_factor() {
#ifdef VERT_HAS_a_RdhFactor
    return a_RdhFactor;
#else
    return vec4(0.0);
#endif
}

#import <sodium:generated/vertex_format.glsl>
