package org.embeddedt.embeddium.api.options.control;

import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.widgets.AbstractWidget;
import org.embeddedt.embeddium.impl.gui.widgets.FlatButtonWidget;
import org.embeddedt.embeddium.impl.util.Dim2i;
import net.minecraft.ChatFormatting;

import org.jetbrains.annotations.NotNull;

public class ControlElement<T> extends AbstractWidget implements OptionControlElement<T> {
    protected final Option<T> option;

    protected final Dim2i dim;

    private @NotNull FlatButtonWidget.Style style = FlatButtonWidget.Style.defaults();

    public ControlElement(Option<T> option, Dim2i dim) {
        this.option = option;
        this.dim = dim;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        String name = this.option.getName().getString();
        String label;

        boolean hovered = this.isMouseOver(mouseX, mouseY);

        if (hovered && drawContext.getStringWidth(name) > (this.dim.width() - this.option.getControl().getMaxWidth())) {
            name = name.substring(0, Math.min(name.length(), 10)) + "...";
        }

        if (this.option.isAvailable()) {
            if (this.option.hasChanged()) {
                label = ChatFormatting.ITALIC + name + " *";
            } else {
                label = ChatFormatting.WHITE + name;
            }
        } else {
            label = String.valueOf(ChatFormatting.GRAY) + ChatFormatting.STRIKETHROUGH + name;
        }

        drawContext.fill(this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), hovered ? style.bgHovered : style.bgDefault);
        drawContext.drawString(label, this.dim.x() + 6, this.dim.getCenterY() - 4, style.textDefault);
    }

    public Option<T> getOption() {
        return this.option;
    }

    public Dim2i getDimensions() {
        return this.dim;
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return this.dim.containsCursor(x, y);
    }
}
