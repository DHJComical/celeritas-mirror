package org.embeddedt.embeddium.impl.gui.framework;

public interface Interactable {
    boolean isMouseOver(double mouseX, double mouseY);

    default boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseReleased(InteractionContext context, double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(InteractionContext context, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    default boolean mouseScrolled(InteractionContext context, double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
    }
}
