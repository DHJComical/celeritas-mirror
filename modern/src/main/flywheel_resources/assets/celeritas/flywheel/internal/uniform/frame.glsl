// https://github.com/Engine-Room/Flywheel/blob/610b1683f3ed0fef5cd387a126bd2c530a9c2ead/common/src/backend/resources/assets/flywheel/flywheel/internal/uniforms/frame.glsl

layout(std140) uniform _ClrwlFrameUniforms
{
    ivec4 _flw_renderOrigin;

    vec4 _flw_cameraPos;
    vec4 _flw_cameraPosPrev;
    vec4 _flw_cameraLook;
    vec4 _flw_cameraLookPrev;
    vec2 flw_cameraRot;
    vec2 flw_cameraRotPrev;

/** 0 means no fluid. Use FLW_CAMERA_IN_FLUID_* defines to detect fluid type. */
    uint flw_cameraInFluid;
/** 0 means no block. Use FLW_CAMERA_IN_BLOCK_* defines to detect block type. */
    uint flw_cameraInBlock;

    uint _flw_debugMode;

    float _flw_oitNoise;
};

#define flw_renderOrigin (_flw_renderOrigin.xyz)
#define flw_cameraPos (_flw_cameraPos.xyz)
#define flw_cameraLook (_flw_cameraLook.xyz)
#define flw_cameraPosPrev (_flw_cameraPosPrev.xyz)
#define flw_cameraLookPrev (_flw_cameraLookPrev.xyz)

#define FLW_CAMERA_IN_FLUID_WATER 1
#define FLW_CAMERA_IN_FLUID_LAVA 2
#define FLW_CAMERA_IN_FLUID_UNKNOWN 0xFFFFFFFFu

#define FLW_CAMERA_IN_BLOCK_POWDER_SNOW 1
#define FLW_CAMERA_IN_BLOCK_UNKNOWN 0xFFFFFFFFu
