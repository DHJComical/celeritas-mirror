package org.taumc.celeritas.api.options.control;

import org.taumc.celeritas.api.options.structure.Option;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.embeddedt.embeddium.impl.gui.theme.DefaultColors;

public class TickBoxControl implements Control<Boolean> {
    private final Option<Boolean> option;

    public TickBoxControl(Option<Boolean> option) {
        this.option = option;
    }

    @Override
    public ControlElement<Boolean> createElement(Dim2i dim) {
        return new TickBoxControlElement(this.option, dim);
    }

    @Override
    public int getMaxWidth() {
        return 30;
    }

    @Override
    public Option<Boolean> getOption() {
        return this.option;
    }

    private static class TickBoxControlElement extends ControlElement<Boolean> {
        private final Dim2i button;

        public TickBoxControlElement(Option<Boolean> option, Dim2i dim) {
            super(option, dim);

            this.button = new Dim2i(dim.getLimitX() - 16, dim.getCenterY() - 5, 10, 10);
        }

        @Override
        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);

            final int x = this.button.x();
            final int y = this.button.y();
            final int w = x + this.button.width();
            final int h = y + this.button.height();

            final boolean enabled = this.option.isAvailable();
            final boolean ticked = this.option.getValue();

            final int color;

            if (enabled) {
                color = ticked ? DefaultColors.ELEMENT_ACTIVATED : 0xFFFFFFFF;
            } else {
                color = 0xFFAAAAAA;
            }

            if (ticked) {
                drawContext.fill(x + 2, y + 2, w - 2, h - 2, color);
            }

            drawContext.drawBorder(x, y, w, h, color);
        }

        @Override
        public boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                toggleControl();
                context.playClickSound();

                return true;
            }

            return false;
        }

        public void toggleControl() {
            this.option.setValue(!this.option.getValue());
        }
    }
}
