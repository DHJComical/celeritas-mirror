package net.irisshaders.iris.mixin.entity_render_context;

//? if >=1.20 {

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class MixinHumanoidArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
	extends RenderLayer<T, M> {
	private int backupValue = 0;

	public MixinHumanoidArmorLayer(RenderLayerParent<T, M> pRenderLayer0) {
		super(pRenderLayer0);
	}

	@ModifyExpressionValue(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack changeId(ItemStack original) {
		if (WorldRenderingSettings.INSTANCE.getItemIds() == null) return original;

        if (original.getItem() instanceof ArmorItem lvArmorItem8) {
            ResourceLocation location = BuiltInRegistries.ITEM.getKey(lvArmorItem8);

            CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt(new NamespacedId(location.getNamespace(), location.getPath())));
        }

        return original;
    }

	@Inject(method = "renderTrim*", at = @At(value = "HEAD"))
	private void changeTrimTemp(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) ArmorTrim pArmorTrim4) {
		if (WorldRenderingSettings.INSTANCE.getItemIds() == null) return;

		backupValue = CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt(new NamespacedId("minecraft", "trim_" + pArmorTrim4.material().value().assetName())));
	}

	@Inject(method = "renderTrim*", at = @At(value = "TAIL"))
	private void changeTrimTemp2(CallbackInfo ci) {
		if (WorldRenderingSettings.INSTANCE.getItemIds() == null) return;
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(backupValue);
		backupValue = 0;
	}

	@Inject(method = "renderArmorPiece", at = @At(value = "TAIL"))
	private void changeId2(CallbackInfo ci) {
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
	}
}

//?}