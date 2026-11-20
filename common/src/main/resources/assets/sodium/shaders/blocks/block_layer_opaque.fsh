#version 330 core

#ifdef LEGACY
#extension GL_EXT_gpu_shader4 : require
#endif

#import <sodium:include/fog.glsl>
#import <sodium:include/terrain_sampling.glsl>

in vec4 v_Color;
in vec4 v_RdhFactor;
in vec2 v_QuadCoord;
in vec2 v_TexCoord;

#if defined(USE_FOG) && defined(CHUNK_FADE_IN_DURATION_MS) && CHUNK_FADE_IN_DURATION_MS > 0
in float v_ChunkAgeMs;
#endif

in float v_MaterialMipBias;
#ifdef USE_FRAGMENT_DISCARD
in float v_MaterialAlphaCutoff;
#endif

#if defined(USE_FOG_POSTMODERN)
in float v_SphericalFragDistance;
in float v_CylindricalFragDistance;
#elif defined(USE_FOG)
in float v_FragDistance;
#endif

uniform sampler2D u_BlockTex; // The block texture

#ifndef LEGACY
out vec4 fragColor; // The output fragment for the color framebuffer
#else
#define fragColor gl_FragColor
#endif

void main() {
    vec4 diffuseColor = _sample_terrain(u_BlockTex, v_TexCoord, v_MaterialMipBias);

#ifdef USE_FRAGMENT_DISCARD
    if (diffuseColor.a < v_MaterialAlphaCutoff) {
        discard;
    }
#endif

    vec4 m_color = v_Color;
    // Apply correction factor to make the resulting color very close to what true bilinear interpolation would obtain
    // min(x, y) * (1 - max(x, y)) == min(x, y) - (x * y)
    float correctionWeight = min(v_QuadCoord.x, v_QuadCoord.y)
            - (v_QuadCoord.x * v_QuadCoord.y);
    m_color += v_RdhFactor * correctionWeight;

    // Apply per-vertex color. AO shade is applied ahead of time on the CPU.
    diffuseColor *= m_color;

#ifdef USE_FOG

#if defined(CHUNK_FADE_IN_DURATION_MS) && CHUNK_FADE_IN_DURATION_MS > 0
    // Make chunk fade in over a short duration
    if (v_ChunkAgeMs < CHUNK_FADE_IN_DURATION_MS) {
        diffuseColor = vec4(mix(_fogBackground(u_FogColor), diffuseColor.rgb, (clamp(v_ChunkAgeMs, 0, CHUNK_FADE_IN_DURATION_MS) / CHUNK_FADE_IN_DURATION_MS)), diffuseColor.a);
    }
#endif

#if defined(USE_FOG_POSTMODERN)
    fragColor = _blendFog(diffuseColor,
                          _linearFogValue(v_SphericalFragDistance, u_EnvFogStart, u_EnvFogEnd),
                          _linearFogValue(v_CylindricalFragDistance, u_RenderDistFogStart, u_RenderDistFogEnd),
                          u_FogColor);
#else // Legacy fog
    float fragDistance;
    if (u_FogShape == FOG_SHAPE_PLANAR) {
        fragDistance = gl_FragCoord.z / gl_FragCoord.w;
    } else {
        fragDistance = v_FragDistance;
    }
#if defined(USE_FOG_EXP2)
    fragColor = _exp2Fog(diffuseColor, fragDistance, u_FogColor, u_FogDensity);
#elif defined(USE_FOG_EXP)
    fragColor = _expFog(diffuseColor, fragDistance, u_FogColor, u_FogDensity);
#elif defined(USE_FOG_SMOOTH)
    fragColor = _linearFog(diffuseColor, fragDistance, u_FogColor, u_FogStart, u_FogEnd);
#endif
#endif

#else // No fog
    fragColor = diffuseColor;
#endif
}
