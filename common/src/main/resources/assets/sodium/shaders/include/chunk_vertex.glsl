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

#ifdef USE_VERTEX_COMPRESSION
#if !defined(VERT_POS_SCALE)
#error "VERT_POS_SCALE not defined"
#elif !defined(VERT_POS_OFFSET)
#error "VERT_POS_OFFSET not defined"
#elif !defined(VERT_TEX_SCALE)
#error "VERT_TEX_SCALE not defined"
#endif

vec3 _vert_decode_position(uvec4 pos_id) {
    uvec3 position = (pos_id.xyz << 5u) | ((uvec3(pos_id.w) >> uvec3(0u, 5u, 10u)) & uvec3(0x1Fu));
    return vec3(position) * VERT_POS_SCALE + VERT_POS_OFFSET;
}

// The low bits of each texture coordinate are stored with the draw parameters
vec2 _vert_decode_tex_coord(uvec2 tex_coord, uint light_coord) {
    uvec2 coord = (tex_coord << 2u) | ((uvec2(light_coord & 0xFFFFu) >> uvec2(4u, 6u)) & uvec2(0x3u));
    return vec2(coord) * VERT_TEX_SCALE;
}

void _vert_init() {
    _vert_fetch();

    uint packed_draw_params = (a_LightCoord & 0xFFFFu);

    _vert_position = _vert_decode_position(a_PosId);
    _vert_tex_diffuse_coord = _vert_decode_tex_coord(a_TexCoord, a_LightCoord);
    _vert_color = a_Color;
    _vert_rdh_factor = _get_rdh_factor();

    _material_params = (packed_draw_params) & 0xFu;
    _draw_id = (packed_draw_params >> 8) & 0xFFu;
    _vert_tex_light_coord = ivec2((uvec2((a_LightCoord >> 16) & 0xFFFFu) >> uvec2(0, 8)) & uvec2(0xFFu));
}

#else

vec3 _vert_decode_position(vec3 pos_id) {
    return pos_id;
}

vec2 _vert_decode_tex_coord(vec2 tex_coord, uint light_coord) {
    return tex_coord;
}

void _vert_init() {
    _vert_fetch();

    _vert_position = _vert_decode_position(a_PosId);
    _vert_tex_diffuse_coord = _vert_decode_tex_coord(a_TexCoord, a_LightCoord);
    _vert_color = a_Color;
    _vert_rdh_factor = _get_rdh_factor();

    uint packed_draw_params = (a_LightCoord & 0xFFFFu);
    // Vertex Material
    _material_params = (packed_draw_params) & 0xFFu;

    // Vertex Mesh ID
    _draw_id  = (packed_draw_params >> 8) & 0xFFu;

    // Vertex Light
    _vert_tex_light_coord = ivec2((uvec2((a_LightCoord >> 16) & 0xFFFFu) >> uvec2(0, 8)) & uvec2(0xFFu));
}
#endif

#ifdef VERT_CAN_FETCH_CORNERS
// Decoded values of another corner of this vertex's primitive, which pulled vertex data makes available
vec3 _vert_corner_position(int corner) {
    return _vert_decode_position(_vert_fetch_corner(corner).a_PosId);
}

vec2 _vert_corner_tex_coord(int corner) {
    _VertInputs v = _vert_fetch_corner(corner);
    return _vert_decode_tex_coord(v.a_TexCoord, v.a_LightCoord);
}
#endif
