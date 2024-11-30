package org.embeddedt.embeddium.impl.gui;

//? if >=1.15 <=1.19.2 {
/*import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL20C;

import java.util.List;

public class BatchedF3Renderer {
    public static void renderList(PoseStack matrixStack, List<String> list, boolean right) {
        renderBackdrop(matrixStack, list, right);
        renderStrings(matrixStack, list, right);
    }

    private static void renderStrings(PoseStack matrixStack, List<String> list, boolean right) {
        var font = Minecraft.getInstance().font;
        var window = Minecraft.getInstance().getWindow();
        MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        Matrix4f positionMatrix = matrixStack.last()
                .pose();

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);

            if (!Strings.isNullOrEmpty(string)) {
                int height = 9;
                int width = font.width(string);

                float x1 = right ? window.getGuiScaledWidth() - 2 - width : 2;
                float y1 = 2 + (height * i);

                font.drawInBatch(string, x1, y1, 0xe0e0e0, false, positionMatrix, immediate,
                        false, 0, 15728880/^? if >=1.16 {^/, font.isBidirectional()/^?}^/);
            }
        }

        immediate.endBatch();
    }

    private static void renderBackdrop(PoseStack matrixStack, List<String> list, boolean right) {
        var font = Minecraft.getInstance().font;
        var window = Minecraft.getInstance().getWindow();

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        int color = 0x90505050;

        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float k = (float) (color & 255) / 255.0F;

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        //? if >=1.17 {
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        //?} else
        /^bufferBuilder.begin(GL20C.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);^/

        Matrix4f matrix = matrixStack.last()
                .pose();

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);

            if (Strings.isNullOrEmpty(string)) {
                continue;
            }

            int height = 9;
            int width = font.width(string);

            int x = right ? window.getGuiScaledWidth() - 2 - width : 2;
            int y = 2 + height * i;

            float x1 = x - 1;
            float y1 = y - 1;
            float x2 = x + width + 1;
            float y2 = y + height - 1;

            bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(g, h, k, f).endVertex();
            bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(g, h, k, f).endVertex();
            bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(g, h, k, f).endVertex();
            bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(g, h, k, f).endVertex();
        }

        //? if >=1.19 {
        BufferBuilder.RenderedBuffer output = bufferBuilder.end();

        BufferUploader.drawWithShader(output);
        //?} else {
        /^bufferBuilder.end();

        BufferUploader.end(bufferBuilder);
        ^///?}
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
*///?}