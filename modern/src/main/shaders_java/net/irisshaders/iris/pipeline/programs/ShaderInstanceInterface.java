package net.irisshaders.iris.pipeline.programs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;

public interface ShaderInstanceInterface {
	void iris$createExtraShaders(ResourceProvider factory, ResourceLocation name) throws IOException;
}
