#include "celeritas:internal/indirect/buffer_bindings.glsl"
#include "celeritas:internal/indirect/model_descriptor.glsl"
#include "celeritas:internal/indirect/matrices.glsl"
#include "celeritas:internal/uniforms.glsl"
#include "flywheel:util/matrix.glsl"
#include "celeritas:internal/indirect/cull/shadow_common.glsl"

layout(local_size_x = 32) in;

layout(std430, binding = _FLW_DRAW_INSTANCE_INDEX_BUFFER_BINDING) restrict writeonly buffer TargetBuffer {
    uint _flw_instanceIndices[];
};

layout(std430, binding = _FLW_PAGE_FRAME_DESCRIPTOR_BUFFER_BINDING) restrict readonly buffer PageFrameDescriptorBuffer {
    uint _flw_pageFrameDescriptors[];
};

layout(std430, binding = _FLW_INSTANCE_VISIBILITY_BUFFER_BINDING) restrict buffer VisibiliyBuffer {
    uint _flw_visibility[];
};

layout(std430, binding = _FLW_BOUNDING_SPHERE_BUFFER_BINDING) restrict readonly buffer BoundingSphereBuffer {
    FlwBoundingSphere _flw_boundingSpheres[];
};

layout(std430, binding = _FLW_MODEL_BUFFER_BINDING) restrict buffer ModelBuffer {
    FlwModelDescriptor _flw_models[];
};

#ifdef _CLRWL_OCCLUSION_CULLING
layout(binding = 0) uniform sampler2D _flw_depthPyramid;
#endif

bool _clrwl_isVisible(uint instanceIndex, uint modelIndex)
{
    vec3 center;
    float radius;
    _flw_unpackBoundingSphere(_flw_boundingSpheres[instanceIndex], center, radius);

    bool isVisible = true;

    if (_clrwl_testSphereDistance(center, radius, isVisible))
    {
        return isVisible;
    }
    #ifdef _CLRWL_FRUSTUM_CULLING
    if (isVisible)
    {
        bool inFrustum = _clrwl_testSphereOn4Planes(center, radius, clrwl_shadowFrustumPlanes.groups[0])
                      && _clrwl_testSphereOn4Planes(center, radius, clrwl_shadowFrustumPlanes.groups[1])
                      && _clrwl_testSphereOn3Planes(center, radius, clrwl_shadowFrustumPlanes.groups[2]);

        isVisible = isVisible && inFrustum;
    }
    #endif

    #ifdef _CLRWL_OCCLUSION_CULLING
    if (isVisible)
    {
        transformBoundingSphere(flw_view, center, radius);
        isVisible = isVisible && _clrwl_testOcclusionCull(center, radius, _flw_depthPyramid);
    }
    #endif

    return isVisible;
}

#if !_CLRWL_FORCE_DISABLE_SUBGROUP_BALLOT && (defined GL_KHR_shader_subgroup_basic && defined GL_KHR_shader_subgroup_ballot)
    #define _CLRWL_USE_SUBGROUP_BALLOT
#endif

void main()
{
#ifndef _CLRWL_USE_SUBGROUP_BALLOT
    if (gl_LocalInvocationID.x == 0)
    {
        _flw_visibility[gl_WorkGroupID.x] = 0;
    }
#endif

    uint pageIndex = gl_WorkGroupID.x << 1u;

    if (pageIndex >= _flw_pageFrameDescriptors.length())
    {
        return;
    }

    uint modelIndex = _flw_pageFrameDescriptors[pageIndex];
    uint pageValidity = _flw_pageFrameDescriptors[pageIndex + 1];
    uint localInvocationMask = 1u << gl_LocalInvocationID.x;

    if ((localInvocationMask & pageValidity) == 0)
    {
        return;
    }

    uint instanceIndex = gl_GlobalInvocationID.x;
    bool isVisible = _clrwl_isVisible(instanceIndex, modelIndex);
    bool wasVisibleLastFrame = (_flw_visibility[gl_WorkGroupID.x] & localInvocationMask) != 0;

    if (isVisible && !wasVisibleLastFrame)
    {
        uint localIndex = atomicAdd(_flw_models[modelIndex].instanceCount, 1);
        uint targetIndex = _flw_models[modelIndex].baseInstance + localIndex;
        _flw_instanceIndices[targetIndex] = instanceIndex;
    }

#ifdef _CLRWL_USE_SUBGROUP_BALLOT
    uvec4 visibility = subgroupBallot(isVisible);

    if (subgroupElect())
    {
        _flw_visibility[gl_WorkGroupID.x] = visibility.x;
    }
#else
    if (isVisible)
    {
        atomicOr(_flw_visibility[gl_WorkGroupID.x], localInvocationMask);
    }
#endif
}
