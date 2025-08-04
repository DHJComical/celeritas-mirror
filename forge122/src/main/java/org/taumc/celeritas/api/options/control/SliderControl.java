package org.taumc.celeritas.api.options.control;

import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.Validate;
import org.embeddedt.embeddium.impl.util.collections.quadtree.Rect2i;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.util.Dim2i;

public class SliderControl implements Control<Integer> {
    private final Option<Integer> option;
    private final int min;
    private final int max;
    private final int interval;
    private final ControlValueFormatter mode;

    public SliderControl(Option<Integer> option, int min, int max, int interval, ControlValueFormatter mode) {
        Validate.isTrue(max > min, "The maximum value must be greater than the minimum value");
        Validate.isTrue(interval > 0, "The slider interval must be greater than zero");
        Validate.isTrue((max - min) % interval == 0, "The maximum value must be divisable by the interval");
        Validate.notNull(mode, "The slider mode must not be null");
        this.option = option;
        this.min = min;
        this.max = max;
        this.interval = interval;
        this.mode = mode;
    }

    public ControlElement<Integer> createElement(Dim2i dim) {
        return new Button(this.option, dim, this.min, this.max, this.interval, this.mode);
    }

    public Option<Integer> getOption() {
        return this.option;
    }

    public int getMaxWidth() {
        return 130;
    }

    private static class Button extends ControlElement<Integer> {
        private static final int THUMB_WIDTH = 2;
        private static final int TRACK_HEIGHT = 1;
        private final Rect2i sliderBounds;
        private final ControlValueFormatter formatter;
        private final int min;
        private final int max;
        private final int range;
        private final int interval;
        private double thumbPosition;
        private boolean sliderHeld;

        public Button(Option<Integer> option, Dim2i dim, int min, int max, int interval, ControlValueFormatter formatter) {
            super(option, dim);
            this.min = min;
            this.max = max;
            this.range = max - min;
            this.interval = interval;
            this.thumbPosition = this.getThumbPositionForValue(option.getValue());
            this.formatter = formatter;
            this.sliderBounds = new Rect2i(dim.getLimitX() - 96, dim.getCenterY() - 5, 90, 10);
            this.sliderHeld = false;
        }

        public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);
            if (!this.option.isAvailable() || !this.hovered && !this.isFocused()) {
                this.renderStandaloneValue(drawContext);
            } else {
                this.renderSlider(drawContext);
            }

        }

        private void renderStandaloneValue(GuiGraphics drawContext) {
            int sliderX = this.sliderBounds.x();
            int sliderY = this.sliderBounds.y();
            int sliderWidth = this.sliderBounds.width();
            int sliderHeight = this.sliderBounds.height();
            ITextComponent label = this.formatter.format(this.option.getValue());
            int labelWidth = this.getStringWidth(label);
            this.drawString(drawContext, label, sliderX + sliderWidth - labelWidth, sliderY + sliderHeight / 2 - 4, -1);
        }

        private void renderSlider(GuiGraphics drawContext) {
            int sliderX = this.sliderBounds.x();
            int sliderY = this.sliderBounds.y();
            int sliderWidth = this.sliderBounds.width();
            int sliderHeight = this.sliderBounds.height();
            this.thumbPosition = this.getThumbPositionForValue(this.option.getValue());
            double thumbOffset = Math.clamp((double)(this.getIntValue() - this.min) / (double)this.range * (double)sliderWidth, 0.0F, sliderWidth);
            int thumbX = (int)((double)sliderX + thumbOffset - (double)2.0F);
            int trackY = (int)((double)((float)sliderY + (float)sliderHeight / 2.0F) - (double)0.5F);
            this.drawRect(drawContext, thumbX, sliderY, thumbX + 4, sliderY + sliderHeight, -1);
            this.drawRect(drawContext, sliderX, trackY, sliderX + sliderWidth, trackY + 1, -1);
            String label = this.formatter.format(this.getIntValue()).getFormattedText();
            int labelWidth = this.font.getStringWidth(label);
            this.drawString(drawContext, label, sliderX - labelWidth - 6, sliderY + sliderHeight / 2 - 4, -1);
        }

        public int getIntValue() {
            return this.min + this.interval * (int)Math.round(this.getSnappedThumbPosition() / (double)this.interval);
        }

        public double getSnappedThumbPosition() {
            return this.thumbPosition / ((double)1.0F / (double)this.range);
        }

        public double getThumbPositionForValue(int value) {
            return (double)(value - this.min) * ((double)1.0F / (double)this.range);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.sliderHeld = false;
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                if (this.sliderBounds.contains((int)mouseX, (int)mouseY)) {
                    this.setValueFromMouse(mouseX);
                    this.sliderHeld = true;
                }
                return true;
            } else {
                return false;
            }
        }

        private void setValueFromMouse(double d) {
            this.setValue((d - (double)this.sliderBounds.x()) / (double)this.sliderBounds.width());
        }

        public void setValue(double d) {
            this.thumbPosition = Math.clamp(d, 0.0F, 1.0F);
            int value = this.getIntValue();
            if (!this.option.getValue().equals(value)) {
                this.option.setValue(value);
            }

        }

        @Override
        public boolean keyPressed(int keyCode) {
            if (!this.isFocused()) {
                return false;
            } else if (keyCode == 203) {
                this.option.setValue(Math.clamp(this.option.getValue() - this.interval, this.min, this.max));
                return true;
            } else if (keyCode == 205) {
                this.option.setValue(Math.clamp(this.option.getValue() + this.interval, this.min, this.max));
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (this.option.isAvailable() && button == 0) {
                if (this.sliderHeld) {
                    this.setValueFromMouse(mouseX);
                }

                return true;
            } else {
                return false;
            }
        }
    }
}
