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
