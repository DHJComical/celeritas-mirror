package org.embeddedt.embeddium.impl.gui.widgets;

import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.embeddedt.embeddium.impl.gui.theme.DefaultColors;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FlatButtonWidget extends AbstractWidget {
    protected final Dim2i dim;
    private final Runnable action;

    private @NotNull Style style = Style.defaults();

    private boolean selected;
    private boolean enabled = true;
    private boolean visible = true;
    private boolean leftAligned;

    private TextComponent label;

    public FlatButtonWidget(Dim2i dim, TextComponent label, Runnable action) {
        this.dim = dim;
        this.label = label;
        this.action = action;
    }

    protected int getLeftAlignedTextOffset(DrawContext drawContext) {
        return 10;
    }

    protected boolean isHovered(int mouseX, int mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        boolean hovered = this.isHovered(mouseX, mouseY);

        int backgroundColor = this.enabled ? (hovered ? this.style.bgHovered : this.style.bgDefault) : this.style.bgDisabled;
        int textColor = this.enabled ? this.style.textDefault : this.style.textDisabled;

        int strWidth = drawContext.getStringWidth(this.label);

        drawContext.fill(this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), backgroundColor);
        int textX;
        if (this.leftAligned) {
            textX = this.dim.x() + this.getLeftAlignedTextOffset(drawContext);
        } else {
            textX = this.dim.getCenterX() - (strWidth / 2);
        }
        drawContext.drawString(this.label, textX, this.dim.getCenterY() - 4, textColor);

        if (this.enabled && this.selected) {
            drawContext.fill(this.dim.x(), this.leftAligned ? this.dim.y() : (this.dim.getLimitY() - 1), this.leftAligned ? (this.dim.x() + 1) : this.dim.getLimitX(), this.dim.getLimitY(), DefaultColors.ELEMENT_ACTIVATED);
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

    @Override
    public boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
        if (!this.enabled || !this.visible) {
            return false;
        }

        if (button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
            doAction(context);

            return true;
        }

        return false;
    }

    private void doAction(InteractionContext context) {
        this.action.run();
        context.playClickSound();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setLabel(TextComponent text) {
        this.label = text;
    }

    public TextComponent getLabel() {
        return this.label;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    public static class Style {
        public int bgHovered, bgDefault, bgDisabled;
        public int textDefault, textDisabled;

        public static Style defaults() {
            var style = new Style();
            style.bgHovered = 0xE0202020;
            style.bgDefault = 0x90000000;
            style.bgDisabled = 0x60000000;
            style.textDefault = 0xFFFFFFFF;
            style.textDisabled = 0x90FFFFFF;

            return style;
        }
    }
}
