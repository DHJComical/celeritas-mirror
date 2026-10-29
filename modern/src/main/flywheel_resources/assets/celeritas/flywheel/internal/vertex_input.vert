in vec3 _flw_aPos;
in vec4 _flw_aColor;
in vec2 _flw_aTexCoord;
in vec2 _flw_aLight;
in vec4 _flw_aNormal;
in vec2 _clrwl_aEntity;
in vec2 _clrwl_aMidTexCoord;
in vec4 _clrwl_aTangent;
in vec4 _clrwl_aMidBlock;
in vec2 _flw_aOverlay;

void _clrwl_layoutVertex() {
    flw_vertexPos = vec4(_flw_aPos, 1.0);
    flw_vertexColor = _flw_aColor;
    flw_vertexTexCoord = _flw_aTexCoord;
    // Integer vertex attributes explode on some drivers for some draw calls, so get the driver
    // to cast the int to a float so we can cast it back to an int and reliably get a sane value.
    flw_vertexOverlay = ivec2(_flw_aOverlay);
    flw_vertexLight = _flw_aLight / 256.0;
    flw_vertexNormal = _flw_aNormal.xyz; // w is garbage
    clrwl_vertexEntity = _clrwl_aEntity;
    clrwl_vertexMidTexCoord = _clrwl_aMidTexCoord;
    clrwl_vertexTangent = _clrwl_aTangent;
    clrwl_vertexMidMesh = _clrwl_aMidBlock; // at_midBlock.w does not exists on 1.20.1
}
