// https://github.com/Engine-Room/Flywheel/blob/610b1683f3ed0fef5cd387a126bd2c530a9c2ead/common/src/backend/resources/assets/flywheel/flywheel/internal/uniforms/fog.glsl

layout(std140) uniform _ClrwlFogUniforms {
    vec4 flw_fogColor;
    vec2 flw_fogRange;
    int flw_fogShape;
};
