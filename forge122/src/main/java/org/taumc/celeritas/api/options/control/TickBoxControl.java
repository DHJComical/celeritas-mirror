package org.taumc.celeritas.api.options.control;

import org.embeddedt.embeddium.impl.util.collections.quadtree.Rect2i;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.theme.DefaultColors;
import org.taumc.celeritas.impl.util.Dim2i;

public class TickBoxControl implements Control<Boolean> {
    private final Option<Boolean> option;

    public TickBoxControl(Option<Boolean> option) {
        this.option = option;
    }

    public ControlElement<Boolean> createElement(Dim2i dim) {
        return new TickBoxControlElement(this.option, dim);
    }

    public int getMaxWidth() {
        return 30;
    }

    public Option<Boolean> getOption() {
        return this.option;
    }

    private static class TickBoxControlElement extends ControlElement<Boolean> {
        private final Rect2i button;

        public TickBoxControlElement(Option<Boolean> option, Dim2i dim) {
            super(option, dim);
            this.button = new Rect2i(dim.getLimitX() - 16, dim.getCenterY() - 5, 10, 10);
        }

        public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);
            int x = this.button.x();
            int y = this.button.y();
            int w = x + this.button.width();
            int h = y + this.button.height();
            boolean enabled = this.option.isAvailable();
            boolean ticked = this.option.getValue();
            int color;

            if (enabled) {
                color = ticked ? DefaultColors.ELEMENT_ACTIVATED : 0xFFFFFFFF;
            } else {
                color = 0xFFAAAAAA;
            }

            if (ticked) {
                this.drawRect(drawContext, x + 2, y + 2, w - 2, h - 2, color);
            }

            this.drawBorder(drawContext, x, y, w, h, color);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                this.toggleControl();
                this.playClickSound();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean keyPressed(int i) {
            if (!this.isFocused()) {
                return false;
            } else if (keySelected(i)) {
                this.toggleControl();
                this.playClickSound();
                return true;
            } else {
                return false;
            }
        }

        public void toggleControl() {
            this.option.setValue(!(Boolean)this.option.getValue());
        }
    }
}
