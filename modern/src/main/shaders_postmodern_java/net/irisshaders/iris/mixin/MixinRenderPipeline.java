package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.pipeline.DerivedPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderPipeline.class)
public class MixinRenderPipeline implements DerivedPipeline {
	@Unique
	private RenderPipeline iris$parent;

	@Inject(method = "toBuilder", at = @At("RETURN"))
	private void iris$recordParent(CallbackInfoReturnable<RenderPipeline.Builder> cir) {
		((DerivedPipeline) cir.getReturnValue()).setParentPipeline((RenderPipeline) (Object) this);
	}

	@Override
	public RenderPipeline getParentPipeline() {
		return iris$parent;
	}

	@Override
	public void setParentPipeline(RenderPipeline parent) {
		iris$parent = parent;
	}
}
