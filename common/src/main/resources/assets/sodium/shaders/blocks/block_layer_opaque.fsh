#version 330 core

#import <sodium:include/fog.glsl>

in VS_OUT
{
    vec4 v_Color; // The interpolated vertex color
    vec2 v_TexCoord; // The interpolated block texture coordinates

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

#ifdef USE_VANILLA_COLOR_FORMAT
    // Apply per-vertex color. AO shade is applied ahead of time on the CPU.
    diffuseColor *= fs_in.v_Color;
#else
    // Apply per-vertex color
    diffuseColor.rgb *= fs_in.v_Color.rgb;

    // Apply ambient occlusion "shade"
    diffuseColor.rgb *= fs_in.v_Color.a;
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