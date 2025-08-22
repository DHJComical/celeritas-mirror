package org.embeddedt.embeddium.impl.gui.modern.framework;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModernDrawContext implements DrawContext {
    private final GuiGraphics gui;
    private final Font font;
    private final Map<TextComponent, Component> componentCache;

    public ModernDrawContext(GuiGraphics gui, Font font) {
        this.gui = gui;
        this.font = font;
        this.componentCache = new HashMap<>();
    }

    private record FormattedWrapper(FormattedCharSequence sequence) implements TextComponent {}

    private Component applyStyles(Component c, Set<TextFormattingStyle> styles) {
        if (styles.isEmpty()) {
            return c;
        }
        return ComponentUtil.empty().append(c).withStyle(vanillaStyle -> {
            for (var style : styles) {
                if (style.isColor()) {
                    vanillaStyle = vanillaStyle.withColor(ChatFormatting.getById(style.ordinal()));
                } else {
                    vanillaStyle = switch (style) {
                        case STRIKETHROUGH -> vanillaStyle.withStrikethrough(true);
                        case UNDERLINE -> vanillaStyle.withUnderlined(true);
                        case ITALIC -> vanillaStyle.withItalic(true);
                        default -> throw new IllegalArgumentException("Unknown TextFormattingStyle: " + style.name());
                    };
                }
            }
            return vanillaStyle;
        });
    }

    private Component convertComponent(TextComponent component) {
        if (component instanceof TextComponent.Literal literal) {
            return ComponentUtil.literal(literal.text());
        } else if (component instanceof TextComponent.Translatable translatable) {
            return ComponentUtil.translatable(translatable.key(), translatable.args().stream().map(a -> {
                if (a instanceof TextComponent c) {
                    return compile(c);
                } else {
                    return a;
                }
            }).toArray());
        } else if (component instanceof TextComponent.Styled styled) {
            var innerComponent = compile(styled.inner());
            return applyStyles(innerComponent, styled.styles());
        } else {
            throw new IllegalArgumentException("Unexpected component class: " + component.getClass().getName());
        }
    }

    private Component compile(TextComponent component) {
        var compiled = this.componentCache.get(component);
        if (compiled == null) {
            compiled = this.convertComponent(component);
            this.componentCache.put(component, compiled);
        }
        return compiled;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        gui.fill(x1, y1, x2, y2, color);
    }

    @Override
    public int drawString(TextComponent str, int x, int y, int color, boolean shadow) {
        if (str instanceof FormattedWrapper wrapper) {
            return gui.drawString(font, wrapper.sequence(), x, y, color, shadow);
        }
        return gui.drawString(font, compile(str), x, y, color, shadow);
    }

    @Override
    public void blitWholeImage(String icon, int x, int y, int width, int height) {
        gui.blit(ResourceLocationUtil.make(icon), x, y, 0.0f, 0.0f, width, height, width, height);
    }

    @Override
    public void pushMatrix() {
        gui.pose().pushPose();
    }

    @Override
    public void translate(double x, double y, double z) {
        gui.pose().translate(x, y, z);
    }

    @Override
    public void popMatrix() {
        gui.pose().popPose();
    }

    @Override
    public void enableScissor(int x1, int y1, int x2, int y2) {
        gui.enableScissor(x1, y1, x2, y2);
    }

    @Override
    public void disableScissor() {
        gui.disableScissor();
    }

    @Override
    public int getStringWidth(TextComponent component) {
        return font.width(compile(component));
    }

    @Override
    public String substrByWidth(String str, int maxWidth) {
        return font.plainSubstrByWidth(str, maxWidth);
    }

    @Override
    public List<TextComponent> split(TextComponent component, int maxWidth) {
        return font.split(compile(component), maxWidth).stream().map(FormattedWrapper::new).map(TextComponent.class::cast).toList();
    }

    @Override
    public String extractString(TextComponent component) {
        return compile(component).getString();
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }
}
