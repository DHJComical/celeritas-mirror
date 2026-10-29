// https://github.com/Engine-Room/Flywheel/blob/dc5bc8e64976c69b38abb6965d5cd9033e5a8808/common/src/backend/resources/assets/flywheel/flywheel/internal/depth.glsl

float _clrwl_linearize_depth(float d, float zNear, float zFar)
{
    float z_n = 2.0 * d - 1.0;
    return 2.0 * zNear * zFar / (zFar + zNear - z_n * (zFar - zNear));
}

float _clrwl_delinearize_depth(float linearDepth, float zNear, float zFar)
{
    float z_n = (2.0 * zNear * zFar / linearDepth) - (zFar + zNear);
    return 0.5 * (z_n / (zNear - zFar) + 1.0);
}
