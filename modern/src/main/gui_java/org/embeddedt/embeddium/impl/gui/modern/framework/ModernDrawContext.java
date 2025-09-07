package org.embeddedt.embeddium.impl.gui.modern.framework;

//? if <1.20
/*import com.mojang.blaze3d.platform.Window;*/
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.RequiredArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <1.20
/*import net.minecraft.client.gui.Gui;*/
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

@RequiredArgsConstructor
public class ModernDrawContext implements DrawContext {
    //? if >=1.20
    private final net.minecraft.client.gui.GuiGraphics gui;
    //? if <1.20
    /*private final PoseStack pose;*/
    private final Font font;
    private final Map<TextComponent, Component> componentCache = new HashMap<>();

    private static final Map<String, String> MOD_LOGOS = new HashMap<>();

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

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        //? if >=1.20 {
        gui.fill(x1, y1, x2, y2, color);
        //?} else
        /*Gui.fill(pose, x1, y1, x2, y2, color);*/
    }

    @Override
    public int drawString(TextComponent str, int x, int y, int color, boolean shadow) {
        if (str instanceof FormattedWrapper wrapper) {
            //? if >=1.20 {
            return gui.drawString(font, wrapper.sequence(), x, y, color, shadow);
            //?} else
            /*return shadow ? font.drawShadow(pose, wrapper.sequence(), x, y, color) : font.draw(pose, wrapper.sequence(), x, y, color);*/
        }

        //? if >=1.20 {
        return gui.drawString(font, compile(str), x, y, color, shadow);
        //?} else
        /*return shadow ? font.drawShadow(pose, compile(str), x, y, color) : font.draw(pose, compile(str), x, y, color);*/
    }

    @Override
    public void blitWholeImage(@NotNull String icon, int x, int y, int width, int height) {
        //? if >=1.20 {
        gui.blit(ResourceLocationUtil.make(icon), x, y, 0.0f, 0.0f, width, height, width, height);
        //?} else {
        /*//? if >=1.17 {
        RenderSystem.setShaderTexture(0, ResourceLocationUtil.make(icon));
        //?} else
        /^Minecraft.getInstance().getTextureManager().bind(ResourceLocationUtil.make(icon));^/
        Gui.blit(pose, x, y, 0.0f, 0.0f, width, height, width, height);
        *///?}
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
        //? if >=1.20
        var pose = gui.pose();
        pose.pushPose();
    }

    @Override
    public void translate(double x, double y, double z) {
        //? if >=1.20
        var pose = gui.pose();
        pose.translate(x, y, z);
    }

    @Override
    public void popMatrix() {
        //? if >=1.20
        var pose = gui.pose();
        pose.popPose();
    }

    @Override
    public void enableScissor(int x1, int y1, int x2, int y2) {
        //? if <1.20 {
        /*Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        RenderSystem.enableScissor((int)(x1 * scale), (int)(y1 * scale), (int)((x2 - x1) * scale), (int)((y2 - y1) * scale));
        *///?} else
        gui.enableScissor(x1, y1, x2, y2);
    }

    @Override
    public void disableScissor() {
        //? if <1.20 {
        /*RenderSystem.disableScissor();
        *///?} else
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
