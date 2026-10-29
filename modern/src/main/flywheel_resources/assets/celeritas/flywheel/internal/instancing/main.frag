#include "celeritas:internal/common.frag"
#include "flywheel:internal/instancing/light.glsl"

uniform uint _clrwl_packedMaterialUniform;
uniform int _clrwl_entityIdUniform;
uniform int _clrwl_blockEntityIdUniform;

void main()
{
    _flw_unpackMaterialProperties(_clrwl_packedMaterialUniform, flw_material);
    _clrwl_entityId = _clrwl_entityIdUniform;
    _clrwl_blockEntityId = _clrwl_blockEntityIdUniform;
    _clrwl_main();
}
