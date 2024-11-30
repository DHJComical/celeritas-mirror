package org.embeddedt.embeddium.impl.mixin.features.options.render_layers;

import org.embeddedt.embeddium.impl.Celeritas;
//? if >=1.16
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemBlockRenderTypes.class)
public class RenderLayersMixin {
    @Unique
    private static boolean leavesFancy;

    @Redirect(
            method = { "getChunkRenderType" /*? if >=1.16 {*/, "getMovingBlockRenderType" /*?}*/ /*? if forgelike && >=1.19 {*/, "getRenderLayers"/*?}*//*? if forgelike && <1.19 {*//*, "canRenderInLayer(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/RenderType;)Z"*//*?}*/ },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;renderCutout:Z"))
    private static boolean redirectLeavesShouldBeFancy() {
        return leavesFancy;
    }

    @Inject(method = "setFancy", at = @At("RETURN"))
    private static void onSetFancyGraphicsOrBetter(boolean fancyGraphicsOrBetter, CallbackInfo ci) {
        leavesFancy = Celeritas.options().quality.leavesQuality.isFancy(/*? if >=1.16 {*/ fancyGraphicsOrBetter? GraphicsStatus.FANCY : GraphicsStatus.FAST /*?} else {*/ /*fancyGraphicsOrBetter *//*?}*/);
    }
}
