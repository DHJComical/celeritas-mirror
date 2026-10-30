package org.embeddedt.embeddium.impl.gui.modern.framework;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;

public class ModernInteractionContext implements InteractionContext {
    public static final ModernInteractionContext INSTANCE = new ModernInteractionContext();

    ModernInteractionContext() {

    }

    @Override
    public boolean isSpecialKeyDown(SpecialKey key) {
        return switch (key) {
            case SHIFT -> Screen.hasShiftDown();
            case CTRL -> Screen.hasControlDown();
            case ALT -> Screen.hasAltDown();
        };
    }

    @Override
    public String getClipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }
}
