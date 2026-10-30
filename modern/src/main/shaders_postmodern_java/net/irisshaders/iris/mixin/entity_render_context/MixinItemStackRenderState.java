package net.irisshaders.iris.mixin.entity_render_context;

import net.irisshaders.iris.layer.ItemRenderStateExtension;
import net.irisshaders.iris.layer.RenderContextIds;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public class MixinItemStackRenderState implements ItemRenderStateExtension {
	@Unique
	@Nullable
	private Item iris$item;
	@Unique
	@Nullable
	private Identifier iris$modelId;
	@Unique
	private int iris$previousBlockEntityId;

	@Override
	public void iris$setItem(@Nullable Item item, @Nullable Identifier modelId) {
		iris$item = item;
		iris$modelId = modelId;
	}

	@Inject(method = "clear", at = @At("HEAD"))
	private void iris$clearItem(CallbackInfo ci) {
		iris$setItem(null, null);
	}

	@Inject(method = "submit", at = @At("HEAD"))
	private void iris$beginItem(CallbackInfo ci) {
		iris$previousBlockEntityId = CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
		RenderContextIds.setItem(iris$item, iris$modelId);
	}

	@Inject(method = "submit", at = @At("RETURN"))
	private void iris$endItem(CallbackInfo ci) {
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
		CapturedRenderingState.INSTANCE.setCurrentBlockEntity(iris$previousBlockEntityId);
	}
}
