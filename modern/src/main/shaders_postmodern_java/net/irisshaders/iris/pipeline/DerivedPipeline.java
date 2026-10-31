package net.irisshaders.iris.pipeline;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jetbrains.annotations.Nullable;

/**
 * Mixed into {@link RenderPipeline} and its builder: the pipeline that {@link RenderPipeline#toBuilder()} was called on.
 */
public interface DerivedPipeline {
	@Nullable
	RenderPipeline getParentPipeline();

	void setParentPipeline(RenderPipeline parent);
}
