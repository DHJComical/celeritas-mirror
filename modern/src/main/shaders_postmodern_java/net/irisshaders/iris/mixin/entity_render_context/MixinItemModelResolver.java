package net.irisshaders.iris.mixin.entity_render_context;

import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.layer.ItemRenderStateExtension;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public class MixinItemModelResolver {
	@Inject(method = "appendItemLayers", at = @At("HEAD"))
	private void iris$recordItem(CallbackInfo ci, @Local(argsOnly = true) ItemStackRenderState output, @Local(argsOnly = true) ItemStack item) {
		((ItemRenderStateExtension) output).iris$setItem(item.getItem(), item.get(DataComponents.ITEM_MODEL));
	}
}
