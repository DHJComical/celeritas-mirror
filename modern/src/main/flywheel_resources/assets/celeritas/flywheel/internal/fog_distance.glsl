// https://github.com/Engine-Room/Flywheel/blob/dc5bc8e64976c69b38abb6965d5cd9033e5a8808/common/src/backend/resources/assets/flywheel/flywheel/internal/fog_distance.glsl

float _clrwl_sphericalDistance(vec3 relativePos) {
    return length(relativePos);
}

float _clrwl_cylindricalDistance(vec3 relativePos) {
    float distXZ = length(relativePos.xz);
    float distY = abs(relativePos.y);
    return max(distXZ, distY);
}

float _clrwl_fogDistance(vec3 relativePos, int fogShape) {
    if (fogShape == 0) {
        return _clrwl_sphericalDistance(relativePos);
    } else {
        return _clrwl_cylindricalDistance(relativePos);
    }
}

float _clrwl_fogDistance(vec3 worldPos, vec3 cameraPos, int fogShape) {
    return _clrwl_fogDistance(worldPos - cameraPos, fogShape);
}
