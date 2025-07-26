package org.embeddedt.embeddium.impl.gui.compat;

//? if <1.20 {
/*//? if >=1.16.2
import com.mojang.blaze3d.systems.RenderSystem;
//? if >=1.15 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;

import org.embeddedt.embeddium.impl.util.ComponentUtil;
import org.lwjgl.opengl.GL20C;

public class GuiGraphics {
    //? if >=1.15 {
    public final PoseStack stack;

    public GuiGraphics() {
        this(new PoseStack());
    }

    public GuiGraphics(PoseStack stack) {
        this.stack = stack;
    }

    public GuiGraphics(Minecraft mc, MultiBufferSource bufferSource) {
        this(new PoseStack());
    }

    public PoseStack pose() {
        return this.stack;
    }
    //?} else {
    /^public GuiGraphics() {}
    ^///?}

    public void flush() {

    }

    public int drawString(Font font, String str, int x, int y, int color) {
        return drawString(font, str, x, y, color, true);
    }

    public int drawString(Font font, Component component, int x, int y, int color) {
        return drawString(font, component.getString(), x, y, color, true);
    }

    public int drawString(Font font, String sequence, int x, int y, int color, boolean shadow) {
        if(shadow) {
            return font.drawShadow(/^? if >=1.16 {^/stack, /^?}^/ sequence, x, y, color);
        } else {
            return font.draw(/^? if >=1.16 {^/stack, /^?}^/ sequence, x, y, color);
        }
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        Gui.fill(/^? if >=1.16 {^/stack, /^?}^/ x1, y1, x2, y2, color);
    }

    public void enableScissor(int x, int y, int x2, int y2) {
        int width = (x2 - x) + 1;
        int height = (y2 - y) + 1;
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        //? if >=1.16.2 {
        RenderSystem.enableScissor((int) (x * scale), (int) (Minecraft.getInstance().getWindow().getHeight() - (y + height) * scale),
                (int) (width * scale), (int) (height * scale));
        //?} else {
        /^GL20C.glEnable(GL20C.GL_SCISSOR_TEST);
        GL20C.glScissor((int) (x * scale), (int) (Minecraft.getInstance().getWindow().getHeight() - (y + height) * scale),
                (int) (width * scale), (int) (height * scale));
        ^///?}
    }

    public void disableScissor() {
        //? if >=1.16.2 {
        RenderSystem.disableScissor();
        //?} else
        /^GL20C.glDisable(GL20C.GL_SCISSOR_TEST);^/
    }
}

*///?}