// Lighting and bilinear color correction for block_layer_opaque.vsh. Must be imported after chunk_vertex.glsl.

#ifndef CELERITAS_NO_LIGHTMAP
uniform sampler2D u_LightTex; // The light map texture sampler

vec4 _sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}
#endif

vec4 _light_color(ivec2 lightCoord) {
#ifdef CELERITAS_NO_LIGHTMAP
    return vec4(1.0);
#else
    return _sample_lightmap(u_LightTex, lightCoord);
#endif
}

// How far the lit colors of this vertex's quad are from varying bilinearly across it, which is split along its 0-2
// diagonal. Every vertex of the quad gets the same value.
vec4 _vert_lit_rdh_factor(vec4 lightColor) {
#if defined(VERT_HAS_a_RdhFactor)
    return _vert_rdh_factor * 2.0 * vec4(lightColor.rgb, 1.0);
#elif defined(VERT_CAN_FETCH_CORNERS)
    vec4 lit0 = _vert_corner_color(0) * _light_color(_vert_corner_light_coord(0));
    vec4 lit1 = _vert_corner_color(1) * _light_color(_vert_corner_light_coord(1));
    vec4 lit2 = _vert_corner_color(2) * _light_color(_vert_corner_light_coord(2));
    vec4 lit3 = _vert_corner_color(3) * _light_color(_vert_corner_light_coord(3));
    return (lit1 + lit3) - (lit0 + lit2);
#else
    return vec4(0.0);
#endif
}
