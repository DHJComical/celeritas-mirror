struct FlwBoundingSphere
{
    float x;
    float y;
    float z;
    float radius;
};

struct FlwModelDescriptor
{
    uint instanceCount;
    uint baseInstance;
    uint matrixIndex;
    FlwBoundingSphere boundingSphere;
    int entityId;
    int blockEntityId;
    uint clrwlData; // unused: 26 bits | material: 2 bits | light emission: 4 bits
};

void _flw_unpackBoundingSphere(in FlwBoundingSphere sphere, out vec3 center, out float radius)
{
    center = vec3(sphere.x, sphere.y, sphere.z);
    radius = sphere.radius;
}

FlwBoundingSphere _flw_packBoundingSphere(vec3 center, float radius)
{
    return FlwBoundingSphere(center.x, center.y, center.z, radius);
}

void _clrwl_unpackData(in FlwModelDescriptor desc, out int entityId, out int blockEntityId, out int lightEmission)
{
    entityId      = desc.entityId;
    blockEntityId = desc.blockEntityId;
    lightEmission = int((desc.clrwlData & 0x00000008u) >> 0);
}

uint _clrwl_unpackMaterialBitset(in FlwModelDescriptor desc)
{
    return (desc.clrwlData & 0x00000030u) >> 4;
}
