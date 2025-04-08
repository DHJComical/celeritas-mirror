package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.shaderpack.IdMap;

import java.util.ServiceLoader;

public interface IdMapUniforms {
    IdMapUniforms ID_MAP_UNIFORMS = ServiceLoader.load(IdMapUniforms.class).findFirst().orElseThrow();

    void addIdMapUniforms(FrameUpdateNotifier notifier, UniformHolder uniforms, IdMap idMap, boolean isOldHandLight);
}
