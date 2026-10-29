#include "celeritas:internal/common.vert"
#include "celeritas:internal/packed_material.glsl"
#include "celeritas:internal/instancing/light.glsl"

uniform uint _clrwl_packedMaterialUniform;
uniform int _flw_baseInstance = 0;

#ifdef FLW_EMBEDDED
uniform mat4 _flw_modelMatrixUniform;
uniform mat3 _flw_normalMatrixUniform;
#endif

uniform uint _flw_baseVertex;

uniform int _clrwl_entityIdUniform;
uniform int _clrwl_blockEntityIdUniform;
uniform vec4 _clrwl_meshCenterUniform;

void main()
{
    _flw_unpackMaterialProperties(_clrwl_packedMaterialUniform, flw_material);

    FlwInstance instance = _flw_unpackInstance(_flw_baseInstance + gl_InstanceID);

    #ifdef FLW_EMBEDDED
        _flw_modelMatrix = _flw_modelMatrixUniform;
        _flw_normalMatrix = _flw_normalMatrixUniform;
    #endif

    _clrwl_entityId = _clrwl_entityIdUniform;
    _clrwl_blockEntityId = _clrwl_blockEntityIdUniform;
    _clrwl_meshCenter = _clrwl_meshCenterUniform;

    _clrwl_main(instance, gl_InstanceID, _flw_baseVertex);
}
