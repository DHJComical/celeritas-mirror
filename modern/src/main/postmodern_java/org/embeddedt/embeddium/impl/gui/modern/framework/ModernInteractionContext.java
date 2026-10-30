package org.embeddedt.embeddium.impl.gui.modern.framework;

import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;

public class ModernInteractionContext implements InteractionContext {
    public static final ModernInteractionContext INSTANCE = new ModernInteractionContext();

    ModernInteractionContext() {

    }

    @Override
    public boolean isSpecialKeyDown(SpecialKey key) {
        Minecraft minecraft = Minecraft.getInstance();

        return switch (key) {
            case SHIFT -> minecraft.hasShiftDown();
            case CTRL -> minecraft.hasControlDown();
            case ALT -> minecraft.hasAltDown();
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
