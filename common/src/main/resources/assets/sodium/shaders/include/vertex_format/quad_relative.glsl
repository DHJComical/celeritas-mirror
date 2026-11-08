// The layout is defined by QuadRelativeChunkVertex

#ifndef VERT_CAN_FETCH_CORNERS
#error "QuadRelativeChunkVertex can only be pulled"
#endif

// The quad's origin, in 1/4 blocks relative to the section
ivec3 _vert_quad_origin() {
    return ivec3((uvec3(a_QuadData.x) >> uvec3(0u, 7u, 14u)) & uvec3(0x7Fu)) - 32;
}

vec3 _vert_decode_position(uvec3 position, uint low_bits) {
    uvec3 offset = (position << 5u) | ((uvec3(low_bits) >> uvec3(0u, 5u, 10u)) & uvec3(0x1Fu));
    ivec3 origin = _vert_quad_origin() << 18;
    int shift = int((a_QuadData.x >> 21u) & 0x7u);
    // Summed as integers so the only rounding is the conversion to float
    return vec3(origin + (ivec3(offset) << shift)) * (1.0 / 1048576.0);
}

vec2 _vert_decode_tex_coord(uvec2 tex_coord, uint low_bits) {
    uvec2 coord = (tex_coord << 4u) | ((uvec2(low_bits) >> uvec2(15u, 19u)) & uvec2(0xFu));
    return vec2(coord) * (1.0 / 1048576.0);
}

vec4 _vert_decode_color(vec4 color, uint low_bits) {
    float ao = float((low_bits >> 23u) & 0xFFu) / 255.0;
    return vec4(color.rgb * ao, color.a);
}

ivec2 _vert_decode_light_coord(uint light_coord) {
    return ivec2(uvec2(light_coord, light_coord >> 8u) & uvec2(0xFFu));
}

void _vert_init() {
    _vert_fetch();

    _vert_position = _vert_decode_position(a_Position, a_LowBits);
    _vert_tex_diffuse_coord = _vert_decode_tex_coord(a_TexCoord, a_LowBits);
    _vert_color = _vert_decode_color(a_Color, a_LowBits);
    _vert_rdh_factor = _get_rdh_factor();

    _material_params = a_QuadData.y & 0xFu;
    _draw_id = a_QuadData.x >> 24u;
    _vert_tex_light_coord = _vert_decode_light_coord(a_LightCoord);
}

vec3 _vert_corner_position(int corner) {
    _VertInputs v = _vert_fetch_corner(corner);
    return _vert_decode_position(v.a_Position, v.a_LowBits);
}

vec2 _vert_corner_tex_coord(int corner) {
    _VertInputs v = _vert_fetch_corner(corner);
    return _vert_decode_tex_coord(v.a_TexCoord, v.a_LowBits);
}

vec4 _vert_corner_color(int corner) {
    _VertInputs v = _vert_fetch_corner(corner);
    return _vert_decode_color(v.a_Color, v.a_LowBits);
}

ivec2 _vert_corner_light_coord(int corner) {
    return _vert_decode_light_coord(_vert_fetch_corner(corner).a_LightCoord);
}
