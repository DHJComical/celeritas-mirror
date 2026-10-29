#include "celeritas:internal/common.frag"
#include "celeritas:internal/indirect/buffer_bindings.glsl"
#include "celeritas:internal/indirect/light.glsl"

void main()
{
    _flw_unpackMaterialProperties(_clrwl_packedMaterial, flw_material);
    _clrwl_main();
}
