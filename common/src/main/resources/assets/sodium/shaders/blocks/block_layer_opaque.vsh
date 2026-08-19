#version 330 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_material.glsl>

out vec4 v_Color;
out vec4 v_RdhFactor;
out vec2 v_QuadCoord;
out vec2 v_TexCoord;

#if defined(USE_FOG) && defined(CHUNK_FADE_IN_DURATION_MS) && CHUNK_FADE_IN_DURATION_MS > 0
out float v_ChunkAgeMs;
#endif

out float v_MaterialMipBias;
#ifdef USE_FRAGMENT_DISCARD
out float v_MaterialAlphaCutoff;
#endif

#if defined(USE_FOG_POSTMODERN)
out float v_SphericalFragDistance;
out float v_CylindricalFragDistance;
#elif defined(USE_FOG)
out float v_FragDistance;
#endif

uniform int u_FogShape;

uniform vec3 u_RegionOffset;

#ifndef CELERITAS_NO_LIGHTMAP
uniform sampler2D u_LightTex; // The light map texture sampler

vec4 _sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}
#endif

#if defined(USE_FOG) && defined(CHUNK_FADE_IN_DURATION_MS) && CHUNK_FADE_IN_DURATION_MS > 0
uniform float celeritas_ChunkAges[REGION_SIZE];
#endif

void main() {
    _vert_init();

    // Transform the chunk-local vertex position into world model space
    vec3 translation = u_RegionOffset + _get_draw_translation(_draw_id);
    vec3 position = _vert_position + translation;

#if defined(USE_FOG_POSTMODERN)
    v_SphericalFragDistance = getFragDistance(FOG_SHAPE_SPHERICAL, position);
    v_CylindricalFragDistance = getFragDistance(FOG_SHAPE_CYLINDRICAL, position);
#elif defined(USE_FOG)
    v_FragDistance = getFragDistance(u_FogShape, position);
#endif

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

    // Add the light color to the vertex color, and pass the texture coordinates to the fragment shader
    v_RdhFactor = _vert_rdh_factor * 2.0;
#ifdef CELERITAS_NO_LIGHTMAP
    v_Color = _vert_color;
#else
    vec4 lightColor = _sample_lightmap(u_LightTex, _vert_tex_light_coord);
    v_Color = _vert_color * lightColor;
    v_RdhFactor.rgb *= lightColor.rgb;
#endif
    
    int corner = gl_VertexID & 3;
    v_QuadCoord = vec2(corner >= 2 ? 1.0 : 0.0,
            corner == 1 || corner == 2 ? 1.0 : 0.0);
    v_TexCoord = _vert_tex_diffuse_coord;

    v_MaterialMipBias = _material_mip_bias(_material_params);
#ifdef USE_FRAGMENT_DISCARD
    v_MaterialAlphaCutoff = _material_alpha_cutoff(_material_params);
#endif
#if defined(USE_FOG) && defined(CHUNK_FADE_IN_DURATION_MS) && CHUNK_FADE_IN_DURATION_MS > 0
    v_ChunkAgeMs = celeritas_ChunkAges[_draw_id];
#endif
}
