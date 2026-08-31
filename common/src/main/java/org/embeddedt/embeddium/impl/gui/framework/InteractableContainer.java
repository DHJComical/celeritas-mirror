package org.embeddedt.embeddium.impl.gui.framework;

import java.util.function.Predicate;
import java.util.stream.Stream;

public interface InteractableContainer extends Interactable {
    Stream<? extends Interactable> interactableChildren();

    /**
     * the child that most recently claimed a mouseClicked. while set, drag events
     * are routed straight to it instead of being re-checked against the cursor position each time,
     * so a drag started on e.g. a slider isn't dropped the moment the cursor strays off its narrow
     * hitbox. Cleared on mouseReleased.
     */
    Interactable getCapturedChild();
    void setCapturedChild(Interactable child);

    private boolean runSingleChildAction(Predicate<Interactable> action) {
        // Defensive copy to handle mutation
        return interactableChildren().toList().stream().anyMatch(action);
    }

    default boolean isMouseOver(double mouseX, double mouseY) {
        return interactableChildren().anyMatch(i -> i.isMouseOver(mouseX, mouseY));
    }

    @Override
    default boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
        setCapturedChild(null);
        return runSingleChildAction(i -> {
            if (i.mouseClicked(context, mouseX, mouseY, button)) {
                setCapturedChild(i);
                return true;
            }
            return false;
        });
    }

    @Override
    default boolean mouseReleased(InteractionContext context, double mouseX, double mouseY, int button) {
        Interactable captured = getCapturedChild();
        setCapturedChild(null);
        if (captured != null) {
            captured.mouseReleased(context, mouseX, mouseY, button);
            return true;
        }
        return runSingleChildAction(i -> i.mouseReleased(context, mouseX, mouseY, button));
    }

    @Override
    default boolean mouseDragged(InteractionContext context, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        Interactable captured = getCapturedChild();
        if (captured != null) {
            return captured.mouseDragged(context, mouseX, mouseY, button, deltaX, deltaY);
        }
        return runSingleChildAction(i -> i.isMouseOver(mouseX, mouseY) && i.mouseDragged(context, mouseX, mouseY, button, deltaX, deltaY));
    }

    @Override
    default boolean mouseScrolled(InteractionContext context, double mouseX, double mouseY, double deltaX, double deltaY) {
        return runSingleChildAction(i -> i.isMouseOver(mouseX, mouseY) && i.mouseScrolled(context, mouseX, mouseY, deltaX, deltaY));
    }
}
