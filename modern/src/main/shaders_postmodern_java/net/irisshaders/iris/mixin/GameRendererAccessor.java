package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
	@Invoker
	void invokeBobView(CameraRenderState cameraState, PoseStack poseStack);

	@Invoker
	void invokeBobHurt(CameraRenderState cameraState, PoseStack poseStack);
}
