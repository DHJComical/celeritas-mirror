package org.taumc.celeritas.impl.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;
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

    @Override
    public void playClickSound() {
        Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }
}
