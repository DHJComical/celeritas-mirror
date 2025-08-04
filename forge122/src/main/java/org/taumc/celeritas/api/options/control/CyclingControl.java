package org.taumc.celeritas.api.options.control;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.options.TextProvider;
import org.taumc.celeritas.impl.util.ComponentUtil;
import org.taumc.celeritas.impl.util.Dim2i;
import org.apache.commons.lang3.Validate;

public class CyclingControl<T extends Enum<T>> implements Control<T> {
    private final Option<T> option;
    private final T[] allowedValues;
    private final ITextComponent[] names;

    public CyclingControl(Option<T> option, Class<T> enumType) {
        this(option, enumType, enumType.getEnumConstants());
    }

    public CyclingControl(Option<T> option, Class<T> enumType, ITextComponent[] names) {
        T[] universe = enumType.getEnumConstants();

        Validate.isTrue(universe.length == names.length, "Mismatch between universe length and names array length");
        Validate.notEmpty(universe, "The enum universe must contain at least one item");

        this.option = option;
        this.allowedValues = universe;
        this.names = names;
    }

    public CyclingControl(Option<T> option, Class<T> enumType, T[] allowedValues) {
        T[] universe = enumType.getEnumConstants();

        this.option = option;
        this.allowedValues = allowedValues;
        this.names = new ITextComponent[universe.length];

        for (int i = 0; i < this.names.length; i++) {
            ITextComponent name;
            T value = universe[i];

            if (value instanceof TextProvider) {
                name = ((TextProvider) value).getLocalizedName();
            } else {
                name = ComponentUtil.literal(value.name());
            }

            this.names[i] = name;
        }
    }

    public ITextComponent[] getNames() {
        return this.names;
    }

    @Override
    public Option<T> getOption() {
        return this.option;
    }

    @Override
    public ControlElement<T> createElement(Dim2i dim) {
        return new CyclingControlElement<>(this.option, dim, this.allowedValues, this.names);
    }

    @Override
    public int getMaxWidth() {
        return 70;
    }

    private static class CyclingControlElement<T extends Enum<T>> extends ControlElement<T> {
        private final T[] allowedValues;
        private final ITextComponent[] names;

        public CyclingControlElement(Option<T> option, Dim2i dim, T[] allowedValues, ITextComponent[] names) {
            super(option, dim);

            this.allowedValues = allowedValues;
            this.names = names;
        }

        private int getCurrentIndex() {
            for (int i = 0; i < allowedValues.length; i++) {
                if (allowedValues[i] == option.getValue()) {
                    return i;
                }
            }

            return 0;
        }

        @Override
        public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);

            Enum<T> value = this.option.getValue();
            ITextComponent name = this.names[value.ordinal()];

            if(!this.option.isAvailable()) {
                name = ComponentUtil.empty().appendSibling(name).setStyle(new Style().setColor(TextFormatting.GRAY));
            }

            int strWidth = this.getStringWidth(name);
            this.drawString(drawContext, name, this.dim.getLimitX() - strWidth - 6, this.dim.getCenterY() - 4, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                cycleControl(GuiScreen.isShiftKeyDown());
                this.playClickSound();

                return true;
            }

            return false;
        }

        @Override
        public boolean keyPressed(int keyCode) {
            if (!isFocused()) return false;

            if (keySelected(keyCode)) {
                cycleControl(GuiScreen.isShiftKeyDown());
                return true;
            }

            return false;
        }

        public void cycleControl(boolean reverse) {
            int currentIndex = getCurrentIndex();
            if (reverse) {
                currentIndex = (currentIndex + this.allowedValues.length - 1) % this.allowedValues.length;
            } else {
                currentIndex = (currentIndex + 1) % this.allowedValues.length;
            }
            this.option.setValue(this.allowedValues[currentIndex]);
        }
    }
}
