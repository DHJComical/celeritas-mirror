package net.irisshaders.iris.mixin;

import net.irisshaders.iris.layer.BlockEntityRenderType;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection {
	@ModifyVariable(method = {"submitModel", "submitModelPart", "submitCustomGeometry"}, at = @At("HEAD"), argsOnly = true)
	private RenderType iris$wrapBlockEntityRenderType(RenderType renderType) {
		return BlockEntityRenderType.wrapIfSubmitting(renderType);
	}
}
