package net.irisshaders.iris.mixin.entity_render_context;

import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.shaderpack.materialmap.ModernWorldRenderingSettings;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemRenderer.class, priority = 1010)
public abstract class MixinItemRenderer {
	@Unique
	private int previousBeValue;

	@Inject(method = "render", at = @At(value = "HEAD"))
	private void changeId(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) ItemStack pItemRenderer0) {
		iris$setupId(pItemRenderer0);
	}

	@Unique
	private void iris$setupId(ItemStack pItemRenderer0) {
		if (WorldRenderingSettings.INSTANCE.getItemIds() == null) return;

		if (pItemRenderer0.getItem() instanceof BlockItem blockItem && !(pItemRenderer0.getItem() instanceof SolidBucketItem)) {
			if (ModernWorldRenderingSettings.INSTANCE.getBlockStateIds() == null) return;

			previousBeValue = CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
			CapturedRenderingState.INSTANCE.setCurrentBlockEntity(1);

			CapturedRenderingState.INSTANCE.setCurrentRenderedItem(ModernWorldRenderingSettings.INSTANCE.getBlockStateIds().getOrDefault(blockItem.getBlock().defaultBlockState(), 0));
		} else {
            //? if >=1.19.3 {
			ResourceLocation location = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(pItemRenderer0.getItem());
            //?} else
            /*ResourceLocation location = net.minecraft.core.Registry.ITEM.getKey(pItemRenderer0.getItem());*/

			CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt(new NamespacedId(location.getNamespace(), location.getPath())));
		}
	}

	@Inject(method = "render", at = @At(value = "RETURN"))
	private void changeId3(CallbackInfo ci) {
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
		CapturedRenderingState.INSTANCE.setCurrentBlockEntity(previousBeValue);
		previousBeValue = 0;
	}
}
