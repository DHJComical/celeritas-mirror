package org.embeddedt.embeddium.impl.gui.frame;

import org.taumc.celeritas.api.options.control.ControlElement;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.Interactable;
import org.embeddedt.embeddium.impl.gui.framework.InteractableContainer;
import org.embeddedt.embeddium.impl.gui.framework.Renderable;
import org.embeddedt.embeddium.impl.gui.widgets.AbstractWidget;
import org.embeddedt.embeddium.impl.util.Dim2i;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractFrame extends AbstractWidget implements InteractableContainer {
    protected Dim2i dim;
    protected final List<AbstractWidget> children = new ArrayList<>();
    protected final List<Renderable> drawable = new ArrayList<>();
    protected final List<ControlElement<?>> controlElements = new ArrayList<>();
    protected boolean renderOutline;

    public AbstractFrame(Dim2i dim, boolean renderOutline) {
        this.dim = dim;
        this.renderOutline = renderOutline;
    }

    public void buildFrame() {
        for (AbstractWidget element : this.children) {
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
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (this.renderOutline) {
            drawContext.drawBorder(this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), 0xFFAAAAAA);
        }
        for (Renderable drawable : this.drawable) {
            drawable.render(drawContext, mouseX, mouseY, delta);
        }
    }

    @Override
    public Stream<? extends Interactable> interactableChildren() {
        return this.children.stream();
    }

    public Dim2i getDimensions() {
        return this.dim;
    }
}
