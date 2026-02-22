//? if forgelike {
package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.pathways.HandRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
//? if neoforge
/*import net.neoforged.neoforge.client.ClientHooks;*/
//? if forge
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if neoforge {
/*@Mixin(ClientHooks.class)
*///?} else {
@Mixin(ForgeHooksClient.class)
//?}
public class MixinClientHooks_Hand {
    @WrapOperation(method = "handleCameraTransforms", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;applyTransform(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Z)Lnet/minecraft/client/resources/model/BakedModel;"))
    private static BakedModel applyCameraRotDuringTransform(BakedModel instance, ItemDisplayContext itemDisplayContext, PoseStack poseStack, boolean b, Operation<BakedModel> original) {
        if (HandRenderer.INSTANCE.isActive()) {
            // This is a horrible hack that temporarily left-multiplies the camera rotation onto the pose,
            // so that modded models (e.g. AE2 compass) see the camera rotation for their model computations.
            // We then multiply by the inverse rotation to remove it without removing the other transformations
            // that might have been applied. We have to remove the rotation because shaderpacks are not designed
            // to work with it applied.
            Quaternionf cameraRot = new Quaternionf(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());

            Matrix4f pose = poseStack.last().pose();
            Matrix3f normal = poseStack.last().normal();

            Matrix4f savedPose = new Matrix4f(pose);
            Matrix3f savedNormal = new Matrix3f(normal);
            pose.rotation(cameraRot).mul(savedPose);
            normal.rotation(cameraRot).mul(savedNormal);

            var result = original.call(instance, itemDisplayContext, poseStack, b);

            Quaternionf invCameraRot = cameraRot.conjugate(); // cameraRot not usable after this
            savedPose.set(pose);
            savedNormal.set(normal);
            pose.rotation(invCameraRot).mul(savedPose);
            normal.rotation(invCameraRot).mul(savedNormal);

            return result;
        } else {
            return original.call(instance, itemDisplayContext, poseStack, b);
        }
    }
}
//?}