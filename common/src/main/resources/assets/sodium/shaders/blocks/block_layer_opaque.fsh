#version 330 core

#import <sodium:include/fog.glsl>

#ifdef USE_GEOMETRY_SHADER
in GS_OUT
#else
in VS_OUT
#endif
{
#ifdef USE_GEOMETRY_SHADER
    vec2 edge;
    flat vec4 v_Color[4];
#else
    vec4 v_Color;
#endif
    vec2 v_TexCoord;

    float v_MaterialMipBias;
#ifdef USE_FRAGMENT_DISCARD
    float v_MaterialAlphaCutoff;
#endif

#ifdef USE_FOG
    float v_FragDistance; // The fragment's distance from the camera
#endif
} fs_in;

uniform sampler2D u_BlockTex; // The block texture

uniform vec4 u_FogColor; // The color of the shader fog

#ifdef USE_FOG_SMOOTH
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog
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

#ifdef USE_GEOMETRY_SHADER
    vec4 c1 = mix(fs_in.v_Color[0], fs_in.v_Color[1], fs_in.edge.x);
    vec4 c2 = mix(fs_in.v_Color[2], fs_in.v_Color[3], fs_in.edge.x);

    vec4 m_color = mix(c1, c2, fs_in.edge.y);
#else
    vec4 m_color = fs_in.v_Color;
#endif

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
#ifdef USE_FOG_EXP2
    fragColor = _exp2Fog(diffuseColor, fs_in.v_FragDistance, u_FogColor, u_FogDensity);
#endif
#ifdef USE_FOG_SMOOTH
    fragColor = _linearFog(diffuseColor, fs_in.v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
#endif
#else
    fragColor = diffuseColor;
#endif
}