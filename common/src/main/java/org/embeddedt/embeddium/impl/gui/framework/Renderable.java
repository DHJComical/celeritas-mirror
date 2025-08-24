package org.embeddedt.embeddium.impl.gui.framework;

public interface Renderable {
    void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks);
}
