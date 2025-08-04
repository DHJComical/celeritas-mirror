package org.taumc.celeritas.impl.gui.compat;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface Element {
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    default boolean keyPressed(int keyCode) {
        return false;
    }

    default boolean charTyped(char codePoint) {
        return false;
    }

    default boolean isMouseOver(double d, double e) {
        return false;
    }


    default boolean isDragging() {
        return false;
    }

    default void setDragging(boolean dragging) {
    }

    void setFocused(boolean focused);

    boolean isFocused();

    default List<? extends Element> children() {
        return Collections.emptyList();
    }
}