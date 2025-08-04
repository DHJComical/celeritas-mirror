package org.taumc.celeritas.impl.gui.widgets;

import java.util.Objects;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.theme.DefaultColors;
import org.taumc.celeritas.impl.util.Dim2i;

public class FlatButtonWidget extends AbstractWidget {
    protected final Dim2i dim;
    private final Runnable action;
    private @NotNull Style style = FlatButtonWidget.Style.defaults();
    private boolean selected;
    private boolean enabled = true;
    private boolean visible = true;
    private boolean leftAligned;
    private ITextComponent label;

    public FlatButtonWidget(Dim2i dim, ITextComponent label, Runnable action) {
        this.dim = dim;
        this.label = label;
        this.action = action;
    }

    protected int getLeftAlignedTextOffset() {
        return 10;
    }

    protected boolean isHovered(int mouseX, int mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.enabled || !this.visible) {
            return false;
        }

        if (button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
            this.action.run();
            this.playClickSound();

            return true;
        }

        return false;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (this.visible) {
            this.hovered = this.isHovered(mouseX, mouseY);
            int backgroundColor = this.enabled ? (this.hovered ? this.style.bgHovered : this.style.bgDefault) : this.style.bgDisabled;
            int textColor = this.enabled ? this.style.textDefault : this.style.textDisabled;
            int strWidth = this.getStringWidth(this.label);
            this.drawRect(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), backgroundColor);
            int textX;
            if (this.leftAligned) {
                textX = this.dim.x() + this.getLeftAlignedTextOffset();
            } else {
                textX = this.dim.getCenterX() - strWidth / 2;
            }

            this.drawString(drawContext, this.label, textX, this.dim.getCenterY() - 4, textColor);
            if (this.enabled && this.selected) {
                this.drawRect(drawContext, this.dim.x(), this.leftAligned ? this.dim.y() : this.dim.getLimitY() - 1, this.leftAligned ? this.dim.x() + 1 : this.dim.getLimitX(), this.dim.getLimitY(), DefaultColors.ELEMENT_ACTIVATED);
            }

        }
    }

    public void setStyle(@NotNull Style style) {
        Objects.requireNonNull(style);
        this.style = style;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setLeftAligned(boolean leftAligned) {
        this.leftAligned = leftAligned;
    }

    private void doAction() {
        this.action.run();
        this.playClickSound();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setLabel(ITextComponent text) {
        this.label = text;
    }

    public ITextComponent getLabel() {
        return this.label;
    }

    public Dim2i getDimensions() {
        return this.dim;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    public static class Style {
        public int bgHovered;
        public int bgDefault;
        public int bgDisabled;
        public int textDefault;
        public int textDisabled;

        public static Style defaults() {
            Style style = new Style();
            style.bgHovered = 0xE0202020;
            style.bgDefault = 0x90000000;
            style.bgDisabled = 0x60000000;
            style.textDefault = 0xFFFFFFFF;
            style.textDisabled = 0x90FFFFFF;
            return style;
        }
    }
}
