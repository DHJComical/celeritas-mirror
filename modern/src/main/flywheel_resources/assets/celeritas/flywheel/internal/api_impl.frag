#include "celeritas:internal/material.glsl"
#include "flywheel:internal/api_impl.glsl"
#include "celeritas:internal/uniforms.glsl"

#ifndef _CLRWL_IS_FALLBACK
in ClrwlVertexData
{
    vec4 flw_vertexPos;
    vec4 flw_vertexColor;
    vec2 flw_vertexTexCoord;
    flat ivec2 flw_vertexOverlay;
    vec2 flw_vertexLight;
    vec3 flw_vertexNormal;
    vec4 clrwl_vertexTangent;

#ifdef CLRWL_IS_INDIRECT
    flat uint _clrwl_packedMaterial;
    flat int _clrwl_entityId;
    flat int _clrwl_blockEntityId;
#endif

#ifdef _FLW_DEBUG
    vec2 clrwl_vertexEntity;
    vec2 clrwl_vertexMidTexCoord;
    vec4 clrwl_vertexMidMesh;
    flat uvec2 clrwl_debugIds;
#endif
};

vec4 flw_sampleColor;
float flw_distance;

bool flw_fragDiffuse;
vec4 flw_fragColor;
ivec2 flw_fragOverlay;
vec2 flw_fragLight;

vec4 clrwl_overlayColor = vec4(0.0);

#else // _CLRWL_IS_FALLBACK

in ClrwlFallbackVertexData
{
    vec4 clrwl_overlayColor;

#ifdef CLRWL_IS_INDIRECT
    flat uint _clrwl_packedMaterial;
    flat int _clrwl_entityId;
    flat int _clrwl_blockEntityId;
#endif
};
#endif

#ifndef CLRWL_IS_INDIRECT
int _clrwl_entityId;
int _clrwl_blockEntityId;
#endif

FlwMaterial flw_material;

uniform sampler2D flw_diffuseTex;
uniform sampler2D flw_overlayTex;

#ifdef _FLW_CRUMBLING
uniform sampler2D _flw_crumblingTex;
#endif
