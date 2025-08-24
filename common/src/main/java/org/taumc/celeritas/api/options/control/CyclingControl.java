package org.taumc.celeritas.api.options.control;

import org.taumc.celeritas.api.options.structure.Option;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;
import org.embeddedt.embeddium.impl.util.Dim2i;

public class CyclingControl<T> implements Control<T> {
    private final Option<T> option;
    private final T[] allowedValues;
    private final TextComponent[] names;

    public CyclingControl(Option<T> option, Class<T> enumType) {
        this(option, enumType.getEnumConstants(), determineNames(enumType.getEnumConstants()));
    }

    public CyclingControl(Option<T> option, Class<T> enumType, TextComponent[] names) {
        this(option, enumType.getEnumConstants(), names);
    }

    public CyclingControl(Option<T> option, Class<T> enumType, T[] allowedValues) {
        this(option, allowedValues, determineNames(allowedValues));
    }

    public CyclingControl(Option<T> option, T[] allowedValues, TextComponent[] names) {
        this.option = option;
        if (allowedValues.length != names.length) {
            throw new IllegalArgumentException();
        }
        this.allowedValues = allowedValues;
        this.names = names;
    }

    private static TextComponent[] determineNames(Object[] universe) {
        TextComponent[] names = new TextComponent[universe.length];
        for (int i = 0; i < names.length; i++) {
            TextComponent name;
            Object value = universe[i];

            if (value instanceof TextProvider) {
                name = ((TextProvider) value).getLocalizedName();
            } else if (value instanceof Enum<?> e) {
                name = TextComponent.literal(e.name());
            } else {
                throw new IllegalArgumentException("Could not figure out name of object " + value);
            }

            names[i] = name;
        }
        return names;
    }

    public TextComponent[] getNames() {
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

    private static class CyclingControlElement<T> extends ControlElement<T> {
        private final T[] allowedValues;
        private final TextComponent[] names;

        public CyclingControlElement(Option<T> option, Dim2i dim, T[] allowedValues, TextComponent[] names) {
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
        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);

            TextComponent name = this.names[getCurrentIndex()];

            if(!this.option.isAvailable()) {
                name = name.withStyle(TextFormattingStyle.GRAY, TextFormattingStyle.STRIKETHROUGH);
            }

            int strWidth = drawContext.getStringWidth(name);
            drawContext.drawString(name, this.dim.getLimitX() - strWidth - 6, this.dim.getCenterY() - 4, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                cycleControl(context.isSpecialKeyDown(InteractionContext.SpecialKey.SHIFT));
                context.playClickSound();

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
