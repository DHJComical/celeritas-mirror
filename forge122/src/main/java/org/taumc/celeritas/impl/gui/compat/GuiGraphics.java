package org.taumc.celeritas.impl.gui.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL20C;

public class GuiGraphics {
    public int drawString(FontRenderer font, String str, int x, int y, int color) {
        return this.drawString(font, str, x, y, color, true);
    }

    public void drawString(FontRenderer font, ITextComponent component, int x, int y, int color) {
        this.drawString(font, component.getFormattedText(), x, y, color, true);
    }

    public int drawString(FontRenderer font, String str, int x, int y, int color, boolean shadow) {
        return shadow ? font.drawStringWithShadow(str, (float)x, (float)y, color) : font.drawString(str, x, y, color);
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        Gui.drawRect(x1, y1, x2, y2, color);
    }

    public void enableScissor(int x, int y, int x2, int y2) {
        int width = x2 - x + 1;
        int height = y2 - y + 1;
        var mc = Minecraft.getMinecraft();
        ScaledResolution scaledresolution = new ScaledResolution(mc);
        int scale = scaledresolution.getScaleFactor();
        GL20C.glEnable(3089);
        GL20C.glScissor(x * scale, mc.displayHeight - (y + height) * scale, width * scale, height * scale);
    }

    public void disableScissor() {
        GL20C.glDisable(3089);
    }

    public void blit(ResourceLocation icon, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(icon);
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, (float)textureWidth, (float)textureHeight);
    }
}
