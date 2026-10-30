package net.irisshaders.iris.mixin.entity_render_context;

import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.layer.RenderContextIds;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives armor, including horses' and wolves', elytras and armor trims their item IDs.
 */
@Mixin(EquipmentLayerRenderer.class)
public class MixinEquipmentLayerRenderer {
	@Unique
	private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";
	@Unique
	private static final NamespacedId ELYTRA_WITH_CAPE = new NamespacedId("minecraft", "elytra_with_cape");

	@Inject(method = RENDER_LAYERS, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;usePlayerTexture()Z"))
	private void iris$setLayerId(CallbackInfo ci, @Local(argsOnly = true) ItemStack itemStack,
								 @Local(argsOnly = true) @Nullable Identifier playerTextureOverride, @Local EquipmentClientInfo.Layer layer) {
		// Elytras worn with a cape draw the cape's texture.
		if (layer.usePlayerTexture() && playerTextureOverride != null) {
			RenderContextIds.setItem(ELYTRA_WITH_CAPE);
		} else {
			RenderContextIds.setItem(itemStack.getItem(), itemStack.get(DataComponents.ITEM_MODEL));
		}
	}

	@Inject(method = RENDER_LAYERS, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
	private void iris$setTrimId(CallbackInfo ci, @Local ArmorTrim trim) {
		RenderContextIds.setItem(new NamespacedId("minecraft", "trim_" + trim.material().value().assets().base().suffix()));
	}

	@Inject(method = RENDER_LAYERS, at = @At("RETURN"))
	private void iris$resetItemId(CallbackInfo ci) {
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
	}
}
