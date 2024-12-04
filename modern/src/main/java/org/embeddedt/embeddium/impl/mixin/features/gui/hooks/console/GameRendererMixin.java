package org.embeddedt.embeddium.impl.mixin.features.gui.hooks.console;

//? if >=1.21
/*import net.minecraft.client.DeltaTracker;*/
import org.embeddedt.embeddium.impl.gui.console.ConsoleHooks;
import net.minecraft.client.Minecraft;
//$ guigfx
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.embeddedt.embeddium.impl.util.ProfilerUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Unique
    private static boolean HAS_RENDERED_OVERLAY_ONCE = false;

    @Inject(method = "render", at = @At(value = "INVOKE", target =
            //? if >=1.20 {
            "Lnet/minecraft/client/gui/GuiGraphics;flush()V",
            //?} else if >=1.16 {
            /*"Lnet/minecraft/client/gui/Gui;render(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
            *///?} else
            /*"Lnet/minecraft/client/gui/Gui;render(F)V",*/
            shift = At.Shift.AFTER))
    private void onRender(
            //? if <1.21
            float tickDelta, long startTime,
            //? if >=1.21
            /*DeltaTracker deltaTracker,*/
            boolean tick, CallbackInfo ci) {
        // Do not start updating the console overlay until the font renderer is ready
        // This prevents the console from using tofu boxes for everything during early startup
        if (Minecraft.getInstance().getOverlay() != null) {
            if (!HAS_RENDERED_OVERLAY_ONCE) {
                return;
            }
        }

        var profiler = ProfilerUtil.get();
        profiler.push("sodium_console_overlay");

        GuiGraphics drawContext = new GuiGraphics(this.minecraft, this.renderBuffers.bufferSource());

        ConsoleHooks.render(drawContext, GLFW.glfwGetTime());

        drawContext.flush();

        profiler.pop();

        HAS_RENDERED_OVERLAY_ONCE = true;
    }
}
