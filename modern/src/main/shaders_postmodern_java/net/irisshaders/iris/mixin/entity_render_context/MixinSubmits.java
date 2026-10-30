package net.irisshaders.iris.mixin.entity_render_context;

import net.irisshaders.iris.layer.RenderContextIds;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SubmitNodeStorage.ModelSubmit.class, SubmitNodeStorage.ModelPartSubmit.class, SubmitNodeStorage.ItemSubmit.class,
	SubmitNodeStorage.TextSubmit.class, SubmitNodeStorage.CustomGeometrySubmit.class})
public class MixinSubmits implements RenderContextIds.Holder {
	@Unique
	private int iris$entityId;
	@Unique
	private int iris$blockEntityId;
	@Unique
	private int iris$itemId;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void iris$captureIds(CallbackInfo ci) {
		iris$entityId = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
		iris$blockEntityId = CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
		iris$itemId = CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
	}

	@Override
	public void iris$restoreIds() {
		CapturedRenderingState.INSTANCE.setCurrentEntity(iris$entityId);
		CapturedRenderingState.INSTANCE.setCurrentBlockEntity(iris$blockEntityId);
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(iris$itemId);
	}
}
