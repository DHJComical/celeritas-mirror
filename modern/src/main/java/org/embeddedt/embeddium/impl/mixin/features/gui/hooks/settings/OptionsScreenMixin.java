package org.embeddedt.embeddium.impl.mixin.features.gui.hooks.settings;

//? if >=1.21 {
/*import net.minecraft.client.gui.screens.options.OptionsScreen;
*///?} else
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.impl.gui.EmbeddiumVideoOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionsScreen.class)
public class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    //? if >=1.20 {
    @Inject(method = "lambda$init$2", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("removal")
    private void open(CallbackInfoReturnable<Screen> ci) {
        ci.setReturnValue(new EmbeddiumVideoOptionsScreen(this));
    }
    //?} else {
    /*@Inject(method = "*", at = @At(value = "NEW", target = "net/minecraft/client/gui/screens/VideoSettingsScreen"), cancellable = true)
    @SuppressWarnings("removal")
    private void open(CallbackInfo ci) {
        ci.cancel();
        this.minecraft.setScreen(new EmbeddiumVideoOptionsScreen(this.minecraft.screen));
    }
    *///?}
}
