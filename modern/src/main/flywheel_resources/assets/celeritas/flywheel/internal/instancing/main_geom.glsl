#include "celeritas:internal/common_geom.glsl"
#include "celeritas:internal/packed_material.glsl"

uniform uint _clrwl_packedMaterialUniform;
uniform int _clrwl_entityIdUniform;
uniform int _clrwl_blockEntityIdUniform;

void main()
{
    #ifndef _CLRWL_IS_FALLBACK
    _flw_unpackMaterialProperties(_clrwl_packedMaterialUniform, flw_material);
    #endif

    _clrwl_entityId = _clrwl_entityIdUniform;
    _clrwl_blockEntityId = _clrwl_blockEntityIdUniform;
    _clrwl_main();
}
