#include "celeritas:internal/indirect/buffer_bindings.glsl"
#include "celeritas:internal/indirect/model_descriptor.glsl"
#include "celeritas:internal/indirect/matrices.glsl"
#include "celeritas:internal/uniforms.glsl"
#include "flywheel:util/matrix.glsl"

layout(local_size_x = 32) in;

layout(std430, binding = _FLW_PAGE_FRAME_DESCRIPTOR_BUFFER_BINDING) restrict readonly buffer PageFrameDescriptorBuffer {
    uint _flw_pageFrameDescriptors[];
};

layout(std430, binding = _FLW_BOUNDING_SPHERE_BUFFER_BINDING) restrict writeonly buffer BoundingSphereBuffer {
    FlwBoundingSphere _flw_boundingSpheres[];
};

layout(std430, binding = _FLW_MODEL_BUFFER_BINDING) restrict readonly buffer ModelBuffer {
    FlwModelDescriptor _flw_models[];
};

layout(std430, binding = _FLW_MATRIX_BUFFER_BINDING) restrict readonly buffer MatrixBuffer {
    FlwMatrices _flw_matrices[];
};

void main()
{
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
    uint matrixIndex = _flw_models[modelIndex].matrixIndex;

    vec3 center;
    float radius;
    _flw_unpackBoundingSphere(_flw_models[modelIndex].boundingSphere, center, radius);

    FlwInstance instance = _flw_unpackInstance(instanceIndex);
    flw_transformBoundingSphere(instance, center, radius);

    if (matrixIndex > 0)
    {
        transformBoundingSphere(_flw_matrices[matrixIndex].pose, center, radius);
    }

    _flw_boundingSpheres[instanceIndex] = _flw_packBoundingSphere(center, radius);
}
