package org.taumc.celeritas.mixin.core;

//? if <1.8 {
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.debug.CeleritasDebugStrings;

import java.util.List;

@Mixin(GameGui.class)
public class InGameHudMixin {

    @Inject(method = "render", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;debug"
            //? if <1.4
            + "Profiler"
            + "Enabled:Z")), at = @At(value = "INVOKE", target ="Lorg/lwjgl/opengl/GL11;glPopMatrix()V", ordinal = 0))
    private void celeritas$renderDebug(CallbackInfo ci, @Local(ordinal = 0) Window scaler, @Local(ordinal = 0) TextRenderer font) {
        int currentY = 22;
        int yDiff = 10;

        int screenWidth = scaler.getWidth();

        List<Pair<String, Integer>> stringsToRender = CeleritasDebugStrings.getStringsToRender();

        for (var render : stringsToRender) {
            if (!render.left().isBlank()) {
                var width = font.getWidth(render.left());
                font.drawWithShadow(render.left(), screenWidth - width, currentY, render.right());
            }

            currentY += yDiff;
        }
    }
}
//?}