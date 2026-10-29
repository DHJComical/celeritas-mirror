#include "celeritas:internal/indirect/buffer_bindings.glsl"
#include "celeritas:internal/indirect/model_descriptor.glsl"
#include "celeritas:internal/indirect/matrices.glsl"
#include "celeritas:internal/uniforms.glsl"
#include "flywheel:util/matrix.glsl"
#include "celeritas:internal/indirect/cull/gbuffers_common.glsl"

layout(local_size_x = 32) in;

layout(std430, binding = _FLW_DRAW_INSTANCE_INDEX_BUFFER_BINDING) restrict writeonly buffer TargetBuffer {
    uint _flw_instanceIndices[];
};

layout(std430, binding = _FLW_PAGE_FRAME_DESCRIPTOR_BUFFER_BINDING) restrict readonly buffer PageFrameDescriptorBuffer {
    uint _flw_pageFrameDescriptors[];
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

bool _flw_isVisible(uint instanceIndex, uint modelIndex)
{
    vec3 center;
    float radius;
    _flw_unpackBoundingSphere(_flw_boundingSpheres[instanceIndex], center, radius);

    bool isVisible = true;

#ifdef _CLRWL_FRUSTUM_CULLING
    isVisible = isVisible && _flw_testSphere(center, radius);
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

uniform uint clrwl_materialFilter;

void main()
{
    uint pageIndex = gl_WorkGroupID.x << 1u;

    if (pageIndex >= _flw_pageFrameDescriptors.length())
    {
        return;
    }

    uint modelIndex = _flw_pageFrameDescriptors[pageIndex];
    uint materialBitset = _clrwl_unpackMaterialBitset(_flw_models[modelIndex]);

    if ((materialBitset & clrwl_materialFilter) == 0)
    {
        return;
    }

    uint pageValidity = _flw_pageFrameDescriptors[pageIndex + 1];
    uint localInvocationMask = 1u << gl_LocalInvocationID.x;

    if ((localInvocationMask & pageValidity) == 0)
    {
        return;
    }

    uint instanceIndex = gl_GlobalInvocationID.x;

    if (_flw_isVisible(instanceIndex, modelIndex))
    {
        uint localIndex = atomicAdd(_flw_models[modelIndex].instanceCount, 1);
        uint targetIndex = _flw_models[modelIndex].baseInstance + localIndex;
        _flw_instanceIndices[targetIndex] = instanceIndex;
    }
}
