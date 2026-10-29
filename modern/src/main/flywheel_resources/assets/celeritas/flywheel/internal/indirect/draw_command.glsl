struct ClrwlMeshCenter
{
    float x;
    float y;
    float z;
};

struct FlwMeshDrawCommand
{
    uint indexCount;
    uint instanceCount;
    uint firstIndex;
    uint vertexOffset;
    uint baseInstance;

    uint modelIndex;
    uint matrixIndex;

    uint packedFogAndCutout;
    uint packedMaterialProperties;

    ClrwlMeshCenter meshCenter;
};
