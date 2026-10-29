#include "celeritas:internal/uniforms.glsl"
#include "flywheel:util/matrix.glsl"

#ifdef _CLRWL_FRUSTUM_CULLING
// Disgustingly vectorized sphere frustum intersection taking advantage of ahead of time packing.
// Only uses 6 fmas and some boolean ops.
// See also:
// flywheel:uniform/flywheel.glsl
// dev.engine_room.flywheel.lib.math.MatrixMath.writePackedFrustumPlanes
// org.joml.FrustumIntersection.testSphere
bool _flw_testSphere(vec3 center, float radius)
{
    bvec4 xyInside = greaterThanEqual(fma(flw_frustumPlanes.xyX, center.xxxx, fma(flw_frustumPlanes.xyY, center.yyyy, fma(flw_frustumPlanes.xyZ, center.zzzz, flw_frustumPlanes.xyW))), -radius.xxxx);
    bvec2 zInside = greaterThanEqual(fma(flw_frustumPlanes.zX, center.xx, fma(flw_frustumPlanes.zY, center.yy, fma(flw_frustumPlanes.zZ, center.zz, flw_frustumPlanes.zW))), -radius.xx);

    return all(xyInside) && all(zInside);
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

    vec3 cr = c * r;
    float czr2 = c.z * c.z - r * r;

    float vx = sqrt(c.x * c.x + czr2);
    float minx = (vx * c.x - cr.z) / (vx * c.z + cr.x);
    float maxx = (vx * c.x + cr.z) / (vx * c.z - cr.x);

    float vy = sqrt(c.y * c.y + czr2);
    float miny = (vy * c.y + cr.z) / (vy * c.z - cr.y);
    float maxy = (vy * c.y - cr.z) / (vy * c.z + cr.y);

    aabb = -vec4(minx, miny, maxx, maxy) * vec4(P00, P11, P00, P11);

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

    return (clip_z / clip_w) / 2.0 + 0.5;
}

bool _clrwl_testOcclusionCull(vec3 center, float radius, sampler2D _flw_depthPyramid)
{
    vec4 aabb;
    if (!_clrwl_projectSphere(center, radius, _flw_cullData.znear, _flw_cullData.P00, _flw_cullData.P11, aabb))
    {
        return true;
    }

    aabb = _clrwl_clipToUV(aabb);

    float width = (aabb.z - aabb.x) * _flw_cullData.pyramidWidth;
    float height = (aabb.w - aabb.y) * _flw_cullData.pyramidHeight;

    int level = clamp(int(ceil(log2(max(width, height)))), 0, _flw_cullData.pyramidLevels);

    ivec2 levelSize = textureSize(_flw_depthPyramid, level);

    ivec4 levelSizePair = ivec4(levelSize, levelSize);

    ivec4 bounds = ivec4(aabb * vec4(levelSizePair));

    // Clamp to the texture bounds.
    // Since we're not going through a sampler out of bounds texel fetches will return 0.
    bounds = clamp(bounds, ivec4(0), levelSizePair - ivec4(1));

    float depth01 = texelFetch(_flw_depthPyramid, bounds.xw, level).r;
    float depth11 = texelFetch(_flw_depthPyramid, bounds.zw, level).r;
    float depth10 = texelFetch(_flw_depthPyramid, bounds.zy, level).r;
    float depth00 = texelFetch(_flw_depthPyramid, bounds.xy, level).r;

    float depth = max(max(depth00, depth01), max(depth10, depth11));
    float depthSphere = _clrwl_computeSphereMinDepth(center, radius, flw_projection);

    return depthSphere <= depth;
}
#endif
