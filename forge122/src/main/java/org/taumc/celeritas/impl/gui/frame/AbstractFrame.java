package org.taumc.celeritas.impl.gui.frame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20C;
import org.taumc.celeritas.api.options.control.ControlElement;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.compat.Renderable;
import org.taumc.celeritas.impl.gui.widgets.AbstractWidget;
import org.taumc.celeritas.impl.util.Dim2i;
import org.taumc.celeritas.impl.gui.compat.Element;

public abstract class AbstractFrame extends AbstractWidget implements Element {
    protected Dim2i dim;
    protected final List<AbstractWidget> children = new ArrayList<>();
    protected final List<Renderable> drawable = new ArrayList<>();
    protected final List<ControlElement<?>> controlElements = new ArrayList<>();
    protected boolean renderOutline;
    private Element focused;
    private boolean dragging;
    private Consumer<Element> focusListener;

    public AbstractFrame(Dim2i dim, boolean renderOutline) {
        this.dim = dim;
        this.renderOutline = renderOutline;
    }

    public void buildFrame() {
        for (Element element : this.children) {
            if (element instanceof AbstractFrame) {
                this.controlElements.addAll(((AbstractFrame) element).controlElements);
            }

            if (element instanceof ControlElement<?>) {
                this.controlElements.add((ControlElement<?>) element);
            }

            if (element instanceof Renderable) {
                this.drawable.add((Renderable) element);
            }
        }

    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (this.renderOutline) {
            this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), -5592406);
        }

        for (Renderable drawable : this.drawable) {
            drawable.render(drawContext, mouseX, mouseY, delta);
        }

    }

    public void applyScissor(int x, int y, int width, int height, Runnable action) {
        ScaledResolution scaled = new ScaledResolution(Minecraft.getMinecraft());
        double scale = (double) Minecraft.getMinecraft().displayWidth / (double) scaled.getScaledWidth();
        GL20C.glEnable(3089);
        GL20C.glScissor((int) ((double) x * scale), (int) ((double) Minecraft.getMinecraft().displayHeight - (double) (y + height) * scale), (int) ((double) width * scale), (int) ((double) height * scale));
        action.run();
        GL20C.glDisable(3089);
    }

    public void registerFocusListener(Consumer<Element> focusListener) {
        this.focusListener = focusListener;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Nullable
    public Element getFocused() {
        return this.focused;
    }

    public void setFocused(@Nullable Element focused) {
        this.focused = focused;
        if (this.focusListener != null) {
            this.focusListener.accept(focused);
        }
    }

    @Override
    public List<? extends Element> children() {
        return this.children;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    public Dim2i getDimensions() {
        return this.dim;
    }
}
