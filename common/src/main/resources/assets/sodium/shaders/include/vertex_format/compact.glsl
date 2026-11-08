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
