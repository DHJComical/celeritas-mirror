#include "celeritas:internal/indirect/buffer_bindings.glsl"
#include "celeritas:internal/indirect/draw_command.glsl"
#include "celeritas:internal/indirect/model_descriptor.glsl"

layout(local_size_x = _FLW_SUBGROUP_SIZE) in;

layout(std430, binding = _FLW_MODEL_BUFFER_BINDING) restrict readonly buffer ModelBuffer {
    FlwModelDescriptor models[];
};

layout(std430, binding = _FLW_DRAW_BUFFER_BINDING) restrict buffer DrawBuffer {
    FlwMeshDrawCommand drawCommands[];
};

// Apply the results of culling to the draw commands.
void main() {
    uint drawIndex = gl_GlobalInvocationID.x;

    if (drawIndex >= drawCommands.length()) {
        return;
    }

    uint modelIndex = drawCommands[drawIndex].modelIndex;
    drawCommands[drawIndex].instanceCount = models[modelIndex].instanceCount;
}
