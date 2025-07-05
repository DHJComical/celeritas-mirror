package org.embeddedt.embeddium.impl.mixin.features.render.gui.debug;

//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public interface DebugScreenOverlayAccessor {
    @Invoker("renderLines")
    void invokeRenderLines(GuiGraphics pGuiGraphics, List<String> pLines, boolean pLeftSide);
}
//?}