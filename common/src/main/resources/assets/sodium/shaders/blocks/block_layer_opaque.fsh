#version 330 core

#import <sodium:include/fog.glsl>

in VS_OUT
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
} fs_in;

uniform sampler2D u_BlockTex; // The block texture

uniform vec4 u_FogColor; // The color of the shader fog

#ifdef USE_FOG_SMOOTH
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog
#endif

#ifdef USE_FOG_POSTMODERN
uniform float u_RenderDistFogStart;
uniform float u_RenderDistFogEnd;
uniform float u_EnvFogStart;
uniform float u_EnvFogEnd;
#endif

#ifdef USE_FOG_EXP2
uniform float u_FogDensity; // The density of the shader fog
#endif

out vec4 fragColor; // The output fragment for the color framebuffer

void main() {
    vec4 diffuseColor = texture(u_BlockTex, fs_in.v_TexCoord, fs_in.v_MaterialMipBias);

#ifdef USE_FRAGMENT_DISCARD
    if (diffuseColor.a < fs_in.v_MaterialAlphaCutoff) {
        discard;
    }
#endif

    vec4 m_color = fs_in.v_Color;

#ifdef USE_VANILLA_COLOR_FORMAT
    // Apply per-vertex color. AO shade is applied ahead of time on the CPU.
    diffuseColor *= m_color;
#else
    // Apply per-vertex color
    diffuseColor.rgb *= m_color.rgb;

    // Apply ambient occlusion "shade"
    diffuseColor.rgb *= m_color.a;
#endif

#ifdef USE_FOG
#ifdef USE_FOG_POSTMODERN
    float fogValue = max(_linearFogValue(fs_in.v_CylindricalFragDistance, u_RenderDistFogStart, u_RenderDistFogEnd),
                         _linearFogValue(fs_in.v_SphericalFragDistance, u_EnvFogStart, u_EnvFogEnd));

    fragColor = vec4(mix(diffuseColor.rgb, u_FogColor.rgb, fogValue * u_FogColor.a), diffuseColor.a);
#elif defined(USE_FOG_EXP2)
    fragColor = _exp2Fog(diffuseColor, fs_in.v_FragDistance, u_FogColor, u_FogDensity);
#elif defined(USE_FOG_SMOOTH)
    fragColor = _linearFog(diffuseColor, fs_in.v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
#endif
#else
    fragColor = diffuseColor;
#endif
}