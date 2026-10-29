#include "celeritas:internal/packed_material.glsl"
#include "celeritas:internal/diffuse.glsl"

#ifdef _FLW_CRUMBLING
const int _CLRWL_DOWN = 0;
const int _CLRWL_UP = 1;
const int _CLRWL_NORTH = 2;
const int _CLRWL_SOUTH = 3;
const int _CLRWL_WEST = 4;
const int _CLRWL_EAST = 5;

// based on net.minecraftforge.client.ForgeHooksClient.getNearestStable
int _clrwl_getNearestFacing(vec3 normal)
{
    float maxAlignment = -2;
    int face = 2;

    // Calculate the alignment of the normal vector with each axis.
    // Note that `-dot(normal, axis) == dot(normal, -axis)`.
    vec3 alignment = vec3(
    dot(normal, vec3(1., 0., 0.)),
    dot(normal, vec3(0., 1., 0.)),
    dot(normal, vec3(0., 0., 1.))
    );

    if (-alignment.y > maxAlignment)
    {
        maxAlignment = -alignment.y;
        face = _CLRWL_DOWN;
    }
    if (alignment.y > maxAlignment)
    {
        maxAlignment = alignment.y;
        face = _CLRWL_UP;
    }
    if (-alignment.z > maxAlignment)
    {
        maxAlignment = -alignment.z;
        face = _CLRWL_NORTH;
    }
    if (alignment.z > maxAlignment)
    {
        maxAlignment = alignment.z;
        face = _CLRWL_SOUTH;
    }
    if (-alignment.x > maxAlignment)
    {
        maxAlignment = -alignment.x;
        face = _CLRWL_WEST;
    }
    if (alignment.x > maxAlignment)
    {
        maxAlignment = alignment.x;
        face = _CLRWL_EAST;
    }

    return face;
}

vec2 _clrwl_getCrumblingTexCoord()
{
    switch (_clrwl_getNearestFacing(flw_vertexNormal))
    {
        case _CLRWL_DOWN: return vec2(flw_vertexPos.x, -flw_vertexPos.z);
        case _CLRWL_UP: return vec2(flw_vertexPos.x, flw_vertexPos.z);
        case _CLRWL_NORTH: return vec2(-flw_vertexPos.x, -flw_vertexPos.y);
        case _CLRWL_SOUTH: return vec2(flw_vertexPos.x, -flw_vertexPos.y);
        case _CLRWL_WEST: return vec2(-flw_vertexPos.z, -flw_vertexPos.y);
        case _CLRWL_EAST: return vec2(flw_vertexPos.z, -flw_vertexPos.y);
    }

    // default to north
    return vec2(-flw_vertexPos.x, -flw_vertexPos.y);
}
#endif

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

#ifdef FLW_EMBEDDED
mat4 _flw_modelMatrix;
mat3 _flw_normalMatrix;
#endif

vec4 _clrwl_meshCenter;

void _clrwl_main(FlwInstance instance, uint stableInstanceID, uint baseVertex)
{
    flw_vertexId = gl_VertexID - baseVertex;

    _clrwl_layoutVertex();

    // --- Compute mesh center, vertex tangent and midtexcoord

    vec4 flw_vertexPos_bkp = flw_vertexPos;
    vec4 flw_vertexColor_bkp = flw_vertexColor;
    vec2 flw_vertexTexCoord_bkp = flw_vertexTexCoord;
    ivec2 flw_vertexOverlay_bkp = flw_vertexOverlay;
    vec2 flw_vertexLight_bkp = flw_vertexLight;
    vec3 flw_vertexNormal_bkp = flw_vertexNormal;

    vec3 midMesh = clrwl_vertexMidMesh.w == -1
    ? _clrwl_meshCenter.xyz
    : flw_vertexPos.xyz + clrwl_vertexMidMesh.xyz / 64.0;

    flw_vertexPos = vec4(midMesh.xyz, 1.0);
    flw_vertexNormal = clrwl_vertexTangent.xyz;
    flw_vertexTexCoord = clrwl_vertexMidTexCoord;

    flw_instanceVertex(instance);
    flw_materialVertex();

    vec4 transformedMeshCenter = flw_vertexPos;
    clrwl_vertexTangent.xyz = flw_vertexNormal;
    clrwl_vertexMidTexCoord = flw_vertexTexCoord;

    flw_vertexPos = flw_vertexPos_bkp;
    flw_vertexColor = flw_vertexColor_bkp;
    flw_vertexTexCoord = flw_vertexTexCoord_bkp;
    flw_vertexOverlay = flw_vertexOverlay_bkp;
    flw_vertexLight = flw_vertexLight_bkp;
    flw_vertexNormal = flw_vertexNormal_bkp;

    // ---

    flw_instanceVertex(instance);
    flw_materialVertex();

    #ifdef _FLW_CRUMBLING
        flw_vertexTexCoord = _clrwl_getCrumblingTexCoord();
    #endif

    #ifdef FLW_EMBEDDED
        flw_vertexPos = _flw_modelMatrix * flw_vertexPos;
        transformedMeshCenter = _flw_modelMatrix * transformedMeshCenter;
        flw_vertexNormal = _flw_normalMatrix * flw_vertexNormal;
        clrwl_vertexTangent.xyz = _flw_normalMatrix * clrwl_vertexTangent.xyz;
    #endif

    clrwl_vertexMidMesh = vec4((transformedMeshCenter.xyz - flw_vertexPos.xyz) * 64.0,
                               clrwl_vertexMidMesh.w == -1 ? _clrwl_meshCenter.w : clrwl_vertexMidMesh.w);

    flw_vertexNormal = normalize(flw_vertexNormal);

    #ifdef _CLRWL_IS_FALLBACK
        FlwLightAo light;
        if (flw_light(flw_vertexPos.xyz, flw_vertexNormal, light))
        {
            if (!flw_material.ambientOcclusion)
            {
                light.ao = 1.0;
            }

            #ifdef _CLRWL_SEPARATE_AO
            flw_vertexLight = max(flw_vertexLight, light.light);
            flw_vertexColor.a = light.ao;
            #else
            flw_vertexLight = max(flw_vertexLight, light.light);
            flw_vertexColor.rgb *= light.ao;
            #endif
        }

        #if CLRWL_OLD_LIGHTING
            flw_vertexColor.rgb *= _clrwl_diffuseFactor();
        #endif
    #endif

    #ifdef _FLW_DEBUG
        clrwl_debugIds = uvec2(stableInstanceID, baseVertex);

        if (_flw_debugMode == 6u) // midMesh
        {
            flw_vertexPos.xyz += (clrwl_vertexMidMesh.xyz / 64.0) * (sin(flw_renderSeconds * 3.14159) * 0.5 + 0.5);
        }
    #endif

    if (flw_material.useOverlay)
    {
        clrwl_overlayColor = texelFetch(flw_overlayTex, flw_vertexOverlay, 0);
        clrwl_overlayColor.a = 1.0 - clrwl_overlayColor.a;
    }
    else
    {
        clrwl_overlayColor = vec4(0.0);
    }

    _clrwl_shader_main();
}
