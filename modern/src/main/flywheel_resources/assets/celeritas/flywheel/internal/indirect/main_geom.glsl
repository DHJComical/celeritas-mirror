#include "celeritas:internal/common_geom.glsl"
#include "celeritas:internal/packed_material.glsl"

void main()
{
    #ifndef _CLRWL_IS_FALLBACK
    _flw_unpackMaterialProperties(clrwl_in[0]._clrwl_packedMaterial, flw_material);
    #else
    clrwl_out._clrwl_packedMaterial = clrwl_in[0]._clrwl_packedMaterial;
    clrwl_out._clrwl_entityId = clrwl_in[0]._clrwl_entityId;
    clrwl_out._clrwl_blockEntityId = clrwl_in[0]._clrwl_blockEntityId;
    #endif

    _clrwl_entityId = clrwl_in[0]._clrwl_entityId;
    _clrwl_blockEntityId = clrwl_in[0]._clrwl_blockEntityId;

    _clrwl_main();
}
