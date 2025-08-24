package org.taumc.celeritas.impl.gui;

import net.minecraft.client.gui.GuiScreen;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;

public enum VintageInteractionContext implements InteractionContext {
    INSTANCE;

    @Override
    public boolean isSpecialKeyDown(SpecialKey key) {
        return switch (key) {
            case SHIFT -> GuiScreen.isShiftKeyDown();
            case CTRL -> GuiScreen.isCtrlKeyDown();
            case ALT -> GuiScreen.isAltKeyDown();
        };
    }
}
