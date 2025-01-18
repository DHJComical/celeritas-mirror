package org.embeddedt.embeddium.impl.mixin.features.options.render_layers;

import org.embeddedt.embeddium.impl.Celeritas;
//? if >=1.16
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemBlockRenderTypes.class)
public class RenderLayersMixin {
    @ModifyVariable(method = "setFancy", at = @At("HEAD"), argsOnly = true)
    private static boolean onSetFancyGraphicsOrBetter(boolean fancyGraphicsOrBetter) {
        return Celeritas.options().quality.leavesQuality.isFancy(/*? if >=1.16 {*/ fancyGraphicsOrBetter? GraphicsStatus.FANCY : GraphicsStatus.FAST /*?} else {*/ /*fancyGraphicsOrBetter *//*?}*/);
    }
}
