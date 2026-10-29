// https://github.com/Engine-Room/Flywheel/blob/dc5bc8e64976c69b38abb6965d5cd9033e5a8808/common/src/backend/resources/assets/flywheel/flywheel/internal/diffuse.glsl

float clrwl_diffuse(vec3 normal) {
    vec3 n2 = normal * normal * vec3(.6, .25, .8);
    return min(n2.x + n2.y * (3. + normal.y) + n2.z, 1.);
}

float clrwl_diffuseNether(vec3 normal) {
    vec3 n2 = normal * normal * vec3(.6, .9, .8);
    return min(n2.x + n2.y + n2.z, 1.);
}

float clrwl_diffuseFromLightDirections(vec3 normal) {
    // We assume the directions are normalized before upload.
    float light0 = max(0.0, dot(flw_light0Direction, normal));
    float light1 = max(0.0, dot(flw_light1Direction, normal));
    return min(1.0, (light0 + light1) * 0.6 + 0.4);
}
