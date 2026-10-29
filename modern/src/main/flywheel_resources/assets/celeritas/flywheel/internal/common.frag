#include "celeritas:internal/packed_material.glsl"
#include "celeritas:internal/fog_distance.glsl"

#ifdef CLRWL_OIT
uniform sampler2D _flw_depthRange;
uniform sampler2D _flw_blueNoise;

float _clrwl_tented_blue_noise(float normalizedDepth)
{
    float tentIn = abs(normalizedDepth * 2. - 1);
    float tentIn2 = tentIn * tentIn;
    float tentIn4 = tentIn2 * tentIn2;
    float tent = 1 - (tentIn2 * tentIn4);

    float b = texture(_flw_blueNoise, gl_FragCoord.xy / vec2(64)).r;

    return b * tent;
}

float _clrwl_linear_depth()
{
    return _clrwl_linearize_depth(gl_FragCoord.z, _flw_cullData.znear, _flw_cullData.zfar);
}

#ifdef CLRWL_EVALUATE
float _clrwl_frontmost_transmittance_from_depth(float linear, vec2 range)
{
    return linear <= -range.x + 2e-5 ? 1.0 : 0.0;
}
#endif
#endif

void _clrwl_post_shader();

void _clrwl_main()
{
    #ifndef _CLRWL_IS_FALLBACK
        vec3 screenPos = vec3(gl_FragCoord.xy / flw_viewportSize, gl_FragCoord.z);
        vec3 ndc = screenPos * 2.0 - 1.0;
        vec4 viewPos = flw_projectionInverse * vec4(ndc, 1.0);
        viewPos /= viewPos.w;
        vec4 flwPos = flw_viewInverse * viewPos;

        flw_distance = _clrwl_fogDistance(flwPos.xyz, flw_cameraPos, flw_fogShape);
    #endif

    _clrwl_shader_main();
    _clrwl_post_shader();
}
