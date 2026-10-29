#include "celeritas:internal/depth.glsl"
#include "celeritas:internal/diffuse.glsl"
#include "celeritas:internal/oit/wavelet.glsl"
#include "celeritas:internal/colorizer.glsl"

void clrwl_computeDiscard(vec4 color)
{
    #ifdef _FLW_USE_DISCARD
    if (flw_discardPredicate(color))
    {
        discard;
    }
    #endif
}

#ifndef _CLRWL_IS_FALLBACK
float _clrwl_diffuseFactor()
{
    if (flw_material.cardinalLightingMode == 2u)
    {
        return clrwl_diffuseFromLightDirections(flw_vertexNormal);
    }
    else if (flw_material.cardinalLightingMode == 1u)
    {
        if (flw_constantAmbientLight == 1u)
        {
            return clrwl_diffuseNether(flw_vertexNormal);
        }
        else
        {
            return clrwl_diffuse(flw_vertexNormal);
        }
    }
    else
    {
        return 1.;
    }
}

void _clrwl_materialFragment_hook()
{
    flw_materialFragment();

    if (flw_material.useOverlay)
    {
        clrwl_overlayColor = texelFetch(flw_overlayTex, flw_fragOverlay, 0);
        clrwl_overlayColor.a = 1.0 - clrwl_overlayColor.a;
    }
}

void _clrwl_shaderLight_hook()
{
    flw_shaderLight();

    #ifdef CLRWL_OLD_LIGHTING
    flw_fragColor.rgb *= _clrwl_diffuseFactor();
    #endif
}

void clrwl_getDebugColor(inout vec4 color)
{
    #ifdef _FLW_DEBUG
    switch (_flw_debugMode)
    {
        case 1u:
            color = vec4(flw_vertexNormal * .5 + .5, 1.);
            break;
        case 2u:
            color = vec4(clrwl_vertexTangent.xyz * .5 + .5, 1.);
            break;
        case 3u:
            color = mix(vec4(1.0, 0.0, 0.0, 1.0), vec4(0.0, 0.0, 1.0, 0.0), clrwl_vertexTangent.w * .5 + .5);
            break;
        case 4u:
            color = _flw_id2Color(uint(abs(clrwl_vertexEntity.x)));
            break;
        case 5u:
            color = vec4(sign(flw_vertexTexCoord.x - clrwl_vertexMidTexCoord.x), sign(flw_vertexTexCoord.y - clrwl_vertexMidTexCoord.y), 0.0, 1.);
            break;
        case 6u:
            color = vec4(clrwl_vertexMidMesh.xyz / 64.0 * .5 + .5, 1.);
            break;
        case 7u:
            color = vec4(clrwl_vertexMidMesh.w / 5.0, 0.0, 0.0, 1.);
            break;
        case 8u:
            color = _flw_id2Color(clrwl_debugIds.x);
            break;
        case 9u:
            color = vec4(vec2((flw_fragLight * 15.0 + 0.5) / 16.), 0., 1.);
            break;
        case 10u:
            color = vec4(flw_fragOverlay / 16., 0., 1.);
            break;
        case 11u:
            color = vec4(vec3(_clrwl_diffuseFactor()), 1.);
            break;
        case 12u:
            color = _flw_id2Color(clrwl_debugIds.y);
            break;
    }
    #endif
}

void clrwl_computeFragment(vec4 sampleColor, out vec4 fragColor, out vec2 fragLight, out float ao, out vec4 fragOverlay)
{
    flw_sampleColor = sampleColor;
    flw_fragColor = flw_sampleColor * flw_vertexColor;
    flw_fragLight = flw_vertexLight;
    flw_fragOverlay = flw_vertexOverlay;

    _clrwl_materialFragment_hook();

    vec4 fragColorBfLight = flw_fragColor;

    _clrwl_shaderLight_hook();

    if (flw_material.ambientOcclusion)
    {
        vec3 fragColorLightRatio = flw_fragColor.rgb / fragColorBfLight.rgb;
        ao = clamp(max(max(fragColorLightRatio.r, fragColorLightRatio.g), fragColorLightRatio.b), 0.0, 1.0);
    }
    else
    {
        ao = 1.0;
    }

    clrwl_computeDiscard(flw_fragColor);
    clrwl_getDebugColor(flw_fragColor);

    fragColor = flw_fragColor;
    fragLight = flw_fragLight + 1.0 / 32.0;
    fragOverlay = clrwl_overlayColor;
}
#endif
