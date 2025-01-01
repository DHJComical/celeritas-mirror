package net.irisshaders.iris.mixin.gui;

import net.irisshaders.iris.gui.screen.HudHideable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeGui.class)
public class MixinForgeGui {

    @Shadow
    public Minecraft getMinecraft() {
        return null;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void iris$handleHudHidingScreens(GuiGraphics pGui0, float pFloat1, CallbackInfo ci) {
        Screen screen = this.getMinecraft().screen;

        if (screen instanceof HudHideable) {
            ci.cancel();
        }
    }
}
