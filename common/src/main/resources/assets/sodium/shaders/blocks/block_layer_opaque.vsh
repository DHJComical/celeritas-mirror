#version 330 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_material.glsl>

out VS_OUT
{
    vec4 v_Color;
    vec2 v_TexCoord;

    float v_MaterialMipBias;
#ifdef USE_FRAGMENT_DISCARD
    float v_MaterialAlphaCutoff;
#endif

#if defined(USE_FOG_POSTMODERN)
    float v_SphericalFragDistance;
    float v_CylindricalFragDistance;
#elif defined(USE_FOG)
    float v_FragDistance;
#endif
} vs_out;

uniform int u_FogShape;

uniform vec3 u_RegionOffset;

#ifndef CELERITAS_NO_LIGHTMAP
uniform sampler2D u_LightTex; // The light map texture sampler

vec4 _sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}
#endif

void main() {
    _vert_init();

    // Transform the chunk-local vertex position into world model space
    vec3 translation = u_RegionOffset + _get_draw_translation(_draw_id);
    vec3 position = _vert_position + translation;

#if defined(USE_FOG_POSTMODERN)
    vs_out.v_SphericalFragDistance = getFragDistance(FOG_SHAPE_SPHERICAL, position);
    vs_out.v_CylindricalFragDistance = getFragDistance(FOG_SHAPE_CYLINDRICAL, position);
#elif defined(USE_FOG)
    vs_out.v_FragDistance = getFragDistance(u_FogShape, position);
#endif

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

    // Add the light color to the vertex color, and pass the texture coordinates to the fragment shader
#ifdef CELERITAS_NO_LIGHTMAP
    vs_out.v_Color = _vert_color;
#else
    vs_out.v_Color = _vert_color * _sample_lightmap(u_LightTex, _vert_tex_light_coord);
#endif
    vs_out.v_TexCoord = _vert_tex_diffuse_coord;

    vs_out.v_MaterialMipBias = _material_mip_bias(_material_params);
#ifdef USE_FRAGMENT_DISCARD
    vs_out.v_MaterialAlphaCutoff = _material_alpha_cutoff(_material_params);
#endif
}
