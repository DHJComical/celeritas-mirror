package org.embeddedt.embeddium.impl.mixin.features.render.gui.debug;

//? if forge && <1.20.6 {
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
//? if >=1.20
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.entity.ItemRenderer;
//? if >=1.19 {
import net.minecraftforge.client.gui.overlay.ForgeGui;
//?} else
/*import net.minecraftforge.client.gui.ForgeIngameGui;*/
//? if >=1.19
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
//? if <1.20
/*import org.embeddedt.embeddium.impl.gui.BatchedF3Renderer;*/
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

//? if >=1.19 {
@Mixin(ForgeGui.class)
//?} else
/*@Mixin(ForgeIngameGui.class)*/
public abstract class ForgeGuiMixin extends Gui {
    private DebugScreenOverlay embeddium$debugOverlay;

    public ForgeGuiMixin(Minecraft pMinecraft, ItemRenderer pItemRenderer) {
        super(pMinecraft/*? if >=1.19 {*/, pItemRenderer/*?}*/);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void accessDebugOverlay(Minecraft mc, CallbackInfo ci) {
        //? if >=1.19 {
        embeddium$debugOverlay = ObfuscationReflectionHelper.getPrivateValue(ForgeGui.class, (ForgeGui)(Object)this, "debugOverlay");
        //?}
    }

    /**
     * @author embeddedt
     * @reason Use the vanilla code to render lines, which fills all backgrounds first before drawing text, so that
     * batching works correctly. Also, ensure the lines are rendered in a managed context.
     */
    //? if >=1.20 {
    @Inject(method = "renderHUDText", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", shift = At.Shift.AFTER, remap = false), remap = false)
    private void renderLinesVanilla(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci, @Local(ordinal = 0) ArrayList<String> listL, @Local(ordinal = 1) ArrayList<String> listR) {
        DebugScreenOverlayAccessor accessor = (DebugScreenOverlayAccessor)embeddium$debugOverlay;
        guiGraphics.drawManaged(() -> {
            accessor.invokeRenderLines(guiGraphics, listL, true);
            accessor.invokeRenderLines(guiGraphics, listR, false);
        });
        // Prevent Forge from rendering any lines
        listL.clear();
        listR.clear();
    }
    //?} else {
    /*@Inject(method = "renderHUDText", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", shift = At.Shift.AFTER), remap = false)
    private void embeddium$renderTextFast(int width, int height, /^? if >=1.16 {^/ PoseStack poseStack, /^?}^/ CallbackInfo ci, @Local(ordinal = 0) ArrayList<String> listL, @Local(ordinal = 1) ArrayList<String> listR) {
        //? if <1.16
        /^var poseStack = new PoseStack();^/
        BatchedF3Renderer.renderList(poseStack, listL, false);
        BatchedF3Renderer.renderList(poseStack, listR, true);

        // Prevent Forge from rendering any lines
        listL.clear();
        listR.clear();
    }
    *///?}
}
//?}