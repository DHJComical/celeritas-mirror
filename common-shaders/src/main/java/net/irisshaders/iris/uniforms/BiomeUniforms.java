package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;

import java.util.ServiceLoader;

public interface BiomeUniforms {
    BiomeUniforms BIOME_UNIFORMS = ServiceLoader.load(BiomeUniforms.class).findFirst().orElseThrow();

    void addBiomeUniforms(UniformHolder uniforms);
}
