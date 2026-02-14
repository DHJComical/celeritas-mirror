package org.taumc.celeritas.impl.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class VintageDrawContext implements DrawContext {
    private final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    private final Map<TextComponent, ITextComponent> componentCache;

    private static final Map<String, String> MOD_LOGOS = new HashMap<>();

    public VintageDrawContext() {
        this.componentCache = new HashMap<>();
    }

    private ITextComponent applyStyles(ITextComponent c, Set<TextFormattingStyle> styles) {
        if (styles.isEmpty()) {
            return c;
        }
        var mutable = new TextComponentString("").appendSibling(c);
        var vanillaStyle = mutable.getStyle();
        for (var style : styles) {
            if (style.isColor()) {
                vanillaStyle = vanillaStyle.setColor(TextFormatting.fromColorIndex(style.ordinal()));
            } else {
                vanillaStyle = switch (style) {
                    case STRIKETHROUGH -> vanillaStyle.setStrikethrough(true);
                    case UNDERLINE -> vanillaStyle.setUnderlined(true);
                    case ITALIC -> vanillaStyle.setItalic(true);
                    default -> throw new IllegalArgumentException("Unknown TextFormattingStyle: " + style.name());
                };
            }
        }
        mutable.setStyle(vanillaStyle);
        return mutable;
    }

    private static String findKey(List<String> keys) {
        for (var str : keys) {
            if (I18n.hasKey(str)) {
                return str;
            }
        }
        return keys.get(0);
    }

    private ITextComponent convertComponent(TextComponent component) {
        if (component instanceof TextComponent.Literal literal) {
            return new TextComponentString(literal.text());
        } else if (component instanceof TextComponent.Translatable translatable) {
            return new TextComponentTranslation(findKey(translatable.keys()), translatable.args().stream().map(a -> {
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

    private ITextComponent compile(TextComponent component) {
        var compiled = this.componentCache.get(component);
        if (compiled == null) {
            compiled = this.convertComponent(component);
            this.componentCache.put(component, compiled);
        }
        return compiled;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        Gui.drawRect(x1, y1, x2, y2, color);
    }

    @Override
    public int drawString(TextComponent str, int x, int y, int color, boolean shadow) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        int len = font.drawString(compile(str).getFormattedText(), x, y, color, shadow);
        GlStateManager.disableBlend();
        return len;
    }

    @Override
    public void blitWholeImage(String icon, int x, int y, int width, int height) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(icon));
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, (float)width, (float)height);
    }

    @Override
    public void pushMatrix() {
        GlStateManager.pushMatrix();
    }

    @Override
    public void translate(double x, double y, double z) {
        GlStateManager.translate(x, y, z);
    }

    @Override
    public void popMatrix() {
        GlStateManager.popMatrix();
    }

    @Override
    public void enableScissor(int x, int y, int x2, int y2) {
        int width = x2 - x + 1;
        int height = y2 - y + 1;
        var mc = Minecraft.getMinecraft();
        ScaledResolution scaledresolution = new ScaledResolution(mc);
        int scale = scaledresolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, mc.displayHeight - (y + height) * scale, width * scale, height * scale);
    }

    @Override
    public void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public int getStringWidth(TextComponent component) {
        return font.getStringWidth(compile(component).getUnformattedText());
    }

    @Override
    public String substrByWidth(String str, int maxWidth) {
        return font.trimStringToWidth(str, maxWidth);
    }

    @Override
    public List<TextComponent> split(TextComponent component, int maxWidth) {
        return font.listFormattedStringToWidth(compile(component).getFormattedText(), maxWidth).stream().map(TextComponent::literal).collect(Collectors.toList());
    }

    @Override
    public String extractString(TextComponent component) {
        return compile(component).getUnformattedText();
    }

    @Override
    public int lineHeight() {
        return font.FONT_HEIGHT;
    }

    @Override
    public TextComponent getFriendlyModName(String modId) {
        var container = Loader.instance().getIndexedModList().get(modId);
        if (container == null) {
            return DrawContext.super.getFriendlyModName(modId);
        }
        return TextComponent.literal(container.getName());
    }

    @Override
    public @Nullable String getModLogoPath(String modId) {
        return MOD_LOGOS.computeIfAbsent(modId, id -> {
            var container = Loader.instance().getIndexedModList().get(id);
            if (container == null) {
                return null;
            }
            String file = container.getMetadata().logoFile;
            if (file == null || file.isEmpty()) {
                return null;
            }
            TextureManager tm = Minecraft.getMinecraft().getTextureManager();
            IResourcePack pack = FMLClientHandler.instance().getResourcePackFor(container.getModId());

            BufferedImage logo = null;
            try {
                if (pack != null) {
                    logo = pack.getPackImage();
                } else {
                    InputStream logoResource = this.getClass().getResourceAsStream(file);
                    if (logoResource != null) {
                        logo = TextureUtil.readBufferedImage(logoResource);
                    }
                }
            } catch (IOException ignored) {
            }

            if (logo == null) {
                return null;
            }

            return tm.getDynamicTextureLocation("modlogo", new DynamicTexture(logo)).toString();
        });
    }
}
