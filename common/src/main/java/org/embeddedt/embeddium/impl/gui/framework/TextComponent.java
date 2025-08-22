package org.embeddedt.embeddium.impl.gui.framework;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public interface TextComponent {
    default List<TextComponent> children() {
        return List.of();
    }

    record Literal(String text) implements TextComponent {
        @Override
        public @NotNull String toString() {
            return text;
        }
    }

    record Translatable(String key, List<?> args) implements TextComponent {
        @Override
        public @NotNull String toString() {
            if (args.isEmpty()) {
                return key;
            }
            return key + args;
        }
    }

    record Styled(TextComponent inner, EnumSet<TextFormattingStyle> styles) implements TextComponent {
        @Override
        public List<TextComponent> children() {
            return List.of(inner);
        }

        @Override
        public TextComponent withStyle(TextFormattingStyle style, TextFormattingStyle... rest) {
            var newSet = EnumSet.copyOf(styles);
            newSet.add(style);
            newSet.addAll(Arrays.asList(rest));
            return new Styled(inner, newSet);
        }

        @Override
        public @NotNull String toString() {
            return inner.toString() + "[styled]";
        }
    }

    default TextComponent withStyle(TextFormattingStyle style, TextFormattingStyle... rest) {
        return new Styled(this, EnumSet.of(style, rest));
    }

    static TextComponent literal(String text) {
        return new Literal(text);
    }

    static TextComponent translatable(String key, Object... args) {
        return new Translatable(key, Arrays.asList(args));
    }
}
