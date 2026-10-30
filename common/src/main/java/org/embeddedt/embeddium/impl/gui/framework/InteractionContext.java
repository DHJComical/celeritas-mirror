package org.embeddedt.embeddium.impl.gui.framework;

public interface InteractionContext {
    default void playClickSound() {

    }

    default boolean isSpecialKeyDown(SpecialKey key) {
        return false;
    }

    String getClipboard();

    void setClipboard(String text);

    enum SpecialKey {
        SHIFT,
        CTRL,
        ALT
    }

    /**
     * Text editing keys and shortcuts, translated from each platform's key events by its screen.
     */
    enum EditAction {
        BACKSPACE,
        DELETE,
        LEFT,
        RIGHT,
        HOME,
        END,
        SELECT_ALL,
        COPY,
        PASTE,
        CUT
    }
}
