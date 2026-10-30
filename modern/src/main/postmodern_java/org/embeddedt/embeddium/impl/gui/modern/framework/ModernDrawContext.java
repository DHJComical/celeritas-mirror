package org.embeddedt.embeddium.impl.gui.modern.framework;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.loader.common.ModLogoUtil;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import org.embeddedt.embeddium.impl.util.PlatformUtil;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModernDrawContext implements DrawContext {
    /**
     * Null when this context is only used to measure text.
     */
    private final @Nullable GuiGraphicsExtractor gui;
    private final Font font;
    private final Map<TextComponent, Component> componentCache = new HashMap<>();

    private static final Map<String, String> MOD_LOGOS = new HashMap<>();

    private record FormattedWrapper(FormattedCharSequence sequence) implements TextComponent {}

    public ModernDrawContext(@Nullable GuiGraphicsExtractor gui, Font font) {
        this.gui = gui;
        this.font = font;
    }

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
                        case STRIKETHROUGH -> vanillaStyle.applyFormat(ChatFormatting.STRIKETHROUGH);
                        case UNDERLINE -> vanillaStyle.withUnderlined(true);
                        case ITALIC -> vanillaStyle.withItalic(true);
                        default -> throw new IllegalArgumentException("Unknown TextFormattingStyle: " + style.name());
                    };
                }
            }
            return vanillaStyle;
        });
    }

    private static String findKey(List<String> keys) {
        for (var str : keys) {
            if (I18n.exists(str)) {
                return str;
            }
        }
        return keys.get(0);
    }

    private Component convertComponent(TextComponent component) {
        if (component instanceof TextComponent.Literal literal) {
            return ComponentUtil.literal(literal.text());
        } else if (component instanceof TextComponent.Translatable translatable) {
            return ComponentUtil.translatable(findKey(translatable.keys()), translatable.args().stream().map(a -> {
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

    // The common GUI may pass text colors without alpha, which it expects to be drawn opaque, as older fonts did.
    private static int opaqueIfNoAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        gui.fill(x1, y1, x2, y2, color);
    }

    @Override
    public int drawString(TextComponent str, int x, int y, int color, boolean shadow) {
        color = opaqueIfNoAlpha(color);

        if (str instanceof FormattedWrapper wrapper) {
            gui.text(font, wrapper.sequence(), x, y, color, shadow);
            return x + font.width(wrapper.sequence());
        }

        Component component = compile(str);
        gui.text(font, component, x, y, color, shadow);
        return x + font.width(component);
    }

    @Override
    public void blitWholeImage(@NotNull String icon, int x, int y, int width, int height) {
        gui.blit(RenderPipelines.GUI_TEXTURED, ResourceLocationUtil.make(icon), x, y, 0.0f, 0.0f, width, height, width, height);
    }

    @Override
    public @Nullable String getModLogoPath(String modId) {
        if (modId.equals("celeritas")) {
            modId = "embeddium";
        }
        return MOD_LOGOS.computeIfAbsent(modId, id -> {
            var loc = ModLogoUtil.registerLogo(id);
            return loc != null ? loc.toString() : null;
        });
    }

    @Override
    public TextComponent getFriendlyModName(String modId) {
        if (modId.equals("celeritas")) {
            modId = "embeddium";
        }
        return TextComponent.literal(PlatformUtil.getModName(modId));
    }

    @Override
    public void pushMatrix() {
        gui.pose().pushMatrix();
    }

    // The GUI is two-dimensional; elements drawn later over earlier ones are layered above them regardless of z.
    @Override
    public void translate(double x, double y, double z) {
        gui.pose().translate((float) x, (float) y);
    }

    @Override
    public void popMatrix() {
        gui.pose().popMatrix();
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
    public String substrByWidth(String str, int maxWidth, boolean reverse) {
        return font.plainSubstrByWidth(str, maxWidth, reverse);
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
