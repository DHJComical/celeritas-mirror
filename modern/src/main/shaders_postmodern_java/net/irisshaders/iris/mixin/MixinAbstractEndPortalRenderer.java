package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Draws end portals and gateways as a solid, slowly scrolling texture, since vanilla's portal effect is done in its own
 * shader, which shader packs don't replace.
 */
@Mixin(AbstractEndPortalRenderer.class)
public class MixinAbstractEndPortalRenderer {
	@Shadow
	@Final
	private static Map<Direction, List<Vector3fc>> FACES;

	@Unique
	private static final float RED = 0.075f;
	@Unique
	private static final float GREEN = 0.15f;
	@Unique
	private static final float BLUE = 0.2f;

	@ModifyArg(method = "submitCube", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"))
	private static RenderType iris$drawSolid(RenderType renderType) {
		return Iris.getCurrentPack().isPresent() ? RenderTypes.entitySolid(AbstractEndPortalRenderer.END_PORTAL_LOCATION) : renderType;
	}

	@Inject(method = "lambda$submitCube$0", at = @At("HEAD"), cancellable = true)
	private static void iris$drawFaces(Collection<Direction> facesToShow, PoseStack.Pose pose, VertexConsumer buffer, CallbackInfo ci) {
		if (Iris.getCurrentPack().isEmpty()) {
			return;
		}

		ci.cancel();

		// Scrolls with a period of 100 seconds. The texture wraps rather than clamping.
		float progress = (SystemTimeUniforms.TIMER.getFrameTimeCounter() * 0.01f) % 1f;

		for (Direction direction : facesToShow) {
			List<Vector3fc> vertices = FACES.get(direction);
			iris$vertex(buffer, pose, vertices.get(0), direction, progress, progress);
			iris$vertex(buffer, pose, vertices.get(1), direction, progress, 0.2F + progress);
			iris$vertex(buffer, pose, vertices.get(2), direction, 0.2F + progress, 0.2F + progress);
			iris$vertex(buffer, pose, vertices.get(3), direction, 0.2F + progress, progress);
		}
	}

	@Unique
	private static void iris$vertex(VertexConsumer buffer, PoseStack.Pose pose, Vector3fc position, Direction direction, float u, float v) {
		buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(RED, GREEN, BLUE, 1.0f)
			.setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
			.setNormal(pose, direction.getStepX(), direction.getStepY(), direction.getStepZ());
	}
}
