package org.taumc.celeritas.api.options.control;

import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.NotNull;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.widgets.AbstractWidget;
import org.taumc.celeritas.impl.gui.widgets.FlatButtonWidget;
import org.taumc.celeritas.impl.gui.widgets.FlatButtonWidget.Style;
import org.taumc.celeritas.impl.util.Dim2i;

public class ControlElement<T> extends AbstractWidget implements OptionControlElement<T> {
    protected final Option<T> option;

    protected final Dim2i dim;

    private final @NotNull FlatButtonWidget.Style style = FlatButtonWidget.Style.defaults();

    public ControlElement(Option<T> option, Dim2i dim) {
        this.option = option;
        this.dim = dim;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        String name = this.option.getName().getFormattedText();
        String label;

        if ((this.hovered || this.isFocused()) && this.font.getStringWidth(name) > (this.dim.width() - this.option.getControl().getMaxWidth())) {
            name = name.substring(0, Math.min(name.length(), 10)) + "...";
        }

        if (this.option.isAvailable()) {
            if (this.option.hasChanged()) {
                label = TextFormatting.ITALIC + name + " *";
            } else {
                label = TextFormatting.WHITE + name;
            }
        } else {
            label = String.valueOf(TextFormatting.GRAY) + TextFormatting.STRIKETHROUGH + name;
        }

        this.hovered = this.dim.containsCursor(mouseX, mouseY);

        this.drawRect(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), this.hovered ? this.style.bgHovered : this.style.bgDefault);
        this.drawString(drawContext, label, this.dim.x() + 6, this.dim.getCenterY() - 4, this.style.textDefault);
        if (this.isFocused()) {
            this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), -1);
        }

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
