#include "celeritas:internal/uniforms.glsl"
#include "flywheel:util/matrix.glsl"

bool _clrwl_isSphereInCube(vec3 center, float radius, vec3 origin, float halfLength)
{
    vec3 v = abs(center - origin);
    float maxD = max(v.x, max(v.y, v.z));
    return maxD <= halfLength - radius;
}

bool _clrwl_testSphereDistance(vec3 center, float radius, out bool isVisible)
{
    float reversedCullDist = clrwl_shadowFrustumPlanes.groups[2].X.w;
    float cullDist = clrwl_shadowFrustumPlanes.groups[2].Y.w;

    if (_clrwl_isSphereInCube(center, radius, flw_cameraPos, reversedCullDist))
    {
        isVisible = true;
        return true;
    }

    if (!_clrwl_isSphereInCube(center, radius, flw_cameraPos, cullDist))
    {
        isVisible = false;
        return true;
    }

    isVisible = true;
    return false;
}

#ifdef _CLRWL_FRUSTUM_CULLING
bool _clrwl_testSphereOn4Planes(vec3 center, float radius, _ClrwlShadowFrustumPlanesGroup planes)
{
    return all(greaterThanEqual(fma(planes.X, center.xxxx, fma(planes.Y, center.yyyy, fma(planes.Z, center.zzzz, planes.W))), -radius.xxxx));
}

bool _clrwl_testSphereOn3Planes(vec3 center, float radius, _ClrwlShadowFrustumPlanesGroup planes)
{
    return all(greaterThanEqual(fma(planes.X.xyz, center.xxx, fma(planes.Y.xyz, center.yyy, fma(planes.Z.xyz, center.zzz, planes.W.xyz))), -radius.xxx));
}
#endif

#ifdef _CLRWL_OCCLUSION_CULLING
bool _clrwl_projectSphere(vec3 c, float r, float znear, float P00, float P11, out vec4 aabb)
{
    // Closest point on the sphere is between the camera and the near plane, don't even attempt to cull.
    if (c.z + r > -znear)
    {
        return false;
    }

    aabb = vec4(c.x - r,  c.y - r, c.x + r, c.y + r) * vec4(P00, P11, P00, P11);

    return true;
}

vec2 _clrwl_clipToUV(vec2 p)
{
    return p * vec2(0.5f) + vec2(0.5f); // clip space -> uv space
}

vec4 _clrwl_clipToUV(vec4 bounds)
{
    return bounds * vec4(0.5f) + vec4(0.5f); // clip space -> uv space
}

float _clrwl_computeSphereMinDepth(vec3 c, float r, mat4 p)
{
    float z = c.z + r;
    float clip_z = fma(z, p[2][2], p[3][2]);
    float clip_w = fma(z, p[2][3], p[3][3]);

    clip_z = clrwl_distortShadowClipZ(clip_z);

    return (clip_z / clip_w) / 2.0 + 0.5;
}

bool _clrwl_testOcclusionCull(vec3 center, float radius, sampler2D _flw_depthPyramid)
{
    vec4 aabb;
    if (!_clrwl_projectSphere(center, radius, _flw_cullData.znear, _flw_cullData.P00, _flw_cullData.P11, aabb))
    {
        return true;
    }

    vec2 p01 = _clrwl_clipToUV(clrwl_distortShadowClipXY(aabb.xw));
    vec2 p11 = _clrwl_clipToUV(clrwl_distortShadowClipXY(aabb.zw));
    vec2 p10 = _clrwl_clipToUV(clrwl_distortShadowClipXY(aabb.zy));
    vec2 p00 = _clrwl_clipToUV(clrwl_distortShadowClipXY(aabb.xy));

    vec2 pMin = min(min(p00, p01), min(p10, p11));
    vec2 pMax = max(max(p00, p01), max(p10, p11));

    float width  = (pMax.x - pMin.x) * _flw_cullData.pyramidWidth;
    float height = (pMax.y - pMin.y) * _flw_cullData.pyramidHeight;

    int level = clamp(int(floor(log2(max(width, height)))), 0, _flw_cullData.pyramidLevels);

    ivec2 levelSize = textureSize(_flw_depthPyramid, level);

    ivec2 ip00 = ivec2(pMin * levelSize);
    ivec2 ip11 = ivec2(pMax * levelSize);
    ivec2 ip01 = ivec2(ip11.x, ip00.y);
    ivec2 ip10 = ivec2(ip00.x, ip11.y);

    // Clamp to the texture bounds.
    // Since we're not going through a sampler out of bounds texel fetches will return 0.
    ip01 = clamp(ip01, ivec2(0), levelSize - ivec2(1));
    ip11 = clamp(ip11, ivec2(0), levelSize - ivec2(1));
    ip10 = clamp(ip10, ivec2(0), levelSize - ivec2(1));
    ip00 = clamp(ip00, ivec2(0), levelSize - ivec2(1));

    float depth01 = texelFetch(_flw_depthPyramid, ip01, level).r;
    float depth11 = texelFetch(_flw_depthPyramid, ip11, level).r;
    float depth10 = texelFetch(_flw_depthPyramid, ip10, level).r;
    float depth00 = texelFetch(_flw_depthPyramid, ip00, level).r;

    float depth = max(max(depth00, depth01), max(depth10, depth11));
    float depthSphere = _clrwl_computeSphereMinDepth(center, radius, flw_projection);

    return depthSphere <= depth;
}
#endif
