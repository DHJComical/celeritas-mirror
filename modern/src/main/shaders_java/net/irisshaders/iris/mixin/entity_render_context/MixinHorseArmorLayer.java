package net.irisshaders.iris.mixin.entity_render_context;

import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HorseArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseArmorLayer.class)
public class MixinHorseArmorLayer {
	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/horse/Horse;FFFFFF)V", at = @At(value = "HEAD"))
	private void changeId(PoseStack pHorseArmorLayer0, MultiBufferSource pMultiBufferSource1, int pInt2, Horse pHorse3, float pFloat4, float pFloat5, float pFloat6, float pFloat7, float pFloat8, float pFloat9, CallbackInfo ci) {
        if (WorldRenderingSettings.INSTANCE.getItemIds() == null) {
            return;
        }
        //? if <1.20.6 {
        var horseArmorItem = pHorse3.getArmor().getItem();
        //?} else
        /*var horseArmorItem = pHorse3.getBodyArmorItem().getItem();*/
		if (!(horseArmorItem instanceof
                //? if <1.20.6 {
                net.minecraft.world.item.HorseArmorItem
                //?} else
                /*net.minecraft.world.item.AnimalArmorItem*/
        ))
			return;


        //? if >=1.19.3 {
        ResourceLocation location = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(horseArmorItem);
         //?} else
        /*ResourceLocation location = net.minecraft.core.Registry.ITEM.getKey(horseArmorItem);*/

		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt(new NamespacedId(location.getNamespace(), location.getPath())));
	}

	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/horse/Horse;FFFFFF)V", at = @At(value = "TAIL"))
	private void changeId2(CallbackInfo ci) {
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
	}
}
