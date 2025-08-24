package org.embeddedt.embeddium.impl.gui.framework;

import java.util.function.Predicate;
import java.util.stream.Stream;

public interface InteractableContainer extends Interactable {
    Stream<? extends Interactable> interactableChildren();

    private boolean runSingleChildAction(Predicate<Interactable> action) {
        // Defensive copy to handle mutation
        return interactableChildren().toList().stream().anyMatch(action);
    }

    default boolean isMouseOver(double mouseX, double mouseY) {
        return interactableChildren().anyMatch(i -> i.isMouseOver(mouseX, mouseY));
    }

    @Override
    default boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
        return runSingleChildAction(i -> i.mouseClicked(context, mouseX, mouseY, button));
    }

    @Override
    default boolean mouseReleased(InteractionContext context, double mouseX, double mouseY, int button) {
        return runSingleChildAction(i -> i.mouseReleased(context, mouseX, mouseY, button));
    }

    @Override
    default boolean mouseDragged(InteractionContext context, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return runSingleChildAction(i -> i.isMouseOver(mouseX, mouseY) && i.mouseDragged(context, mouseX, mouseY, button, deltaX, deltaY));
    }

    @Override
    default boolean mouseScrolled(InteractionContext context, double mouseX, double mouseY, double deltaX, double deltaY) {
        return runSingleChildAction(i -> i.isMouseOver(mouseX, mouseY) && i.mouseScrolled(context, mouseX, mouseY, deltaX, deltaY));
    }
}
