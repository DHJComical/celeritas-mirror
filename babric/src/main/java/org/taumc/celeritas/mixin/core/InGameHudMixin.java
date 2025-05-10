package org.taumc.celeritas.mixin.core;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.ScreenScaler;
import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.Celeritas;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;debugHud:Z")), at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V", ordinal = 0))
    private void celeritas$renderDebug(float screenOpen, boolean mouseX, int mouseY, int par4, CallbackInfo ci, @Local(ordinal = 0) ScreenScaler scaler, @Local(ordinal = 0) TextRenderer font) {
        int currentY = 22;
        int yDiff = 10;

        int screenWidth = scaler.getScaledWidth();

        List<Pair<String, Integer>> stringsToRender = new ArrayList<>();

        stringsToRender.add(Pair.of(getNativeMemoryString(), -1));
        stringsToRender.add(Pair.of("", -1));

        stringsToRender.add(Pair.of("%s Renderer (%s)".formatted("Celeritas", Celeritas.VERSION), 0xFF55FF55));
        stringsToRender.add(Pair.of("", -1));

        var renderer = CeleritasWorldRenderer.instanceNullable();

        if (renderer != null) {
            renderer.getDebugStrings().forEach(str -> stringsToRender.add(Pair.of(str, -1)));
        }

        for (var render : stringsToRender) {
            if (!render.left().isBlank()) {
                var width = font.getWidth(render.left());
                font.drawWithShadow(render.left(), screenWidth - width, currentY, render.right());
            }

            currentY += yDiff;
        }
    }

    private static String getNativeMemoryString() {
        return "Off-Heap: +" + MathUtil.toMib(getNativeMemoryUsage()) + "MB";
    }

    private static long getNativeMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() + NativeBuffer.getTotalAllocated();
    }
}
