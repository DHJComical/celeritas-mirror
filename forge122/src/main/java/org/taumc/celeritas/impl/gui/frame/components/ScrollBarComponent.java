package org.taumc.celeritas.impl.gui.frame.components;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.impl.gui.compat.Element;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.widgets.AbstractWidget;
import org.taumc.celeritas.impl.util.Dim2i;

public class ScrollBarComponent extends AbstractWidget {
    protected static final int SCROLL_OFFSET = 6;
    protected final Dim2i dim;
    private final Mode mode;
    private final int frameLength;
    private final int viewPortLength;
    private final int maxScrollBarOffset;
    private final Consumer<Integer> onSetOffset;
    private int offset;
    private boolean isDragging;
    private Dim2i scrollThumb;
    private int scrollThumbClickOffset;
    private Dim2i extendedScrollArea;

    public ScrollBarComponent(Dim2i trackArea, Mode mode, int frameLength, int viewPortLength, Consumer<Integer> onSetOffset) {
        this.offset = 0;
        this.scrollThumb = null;
        this.extendedScrollArea = null;
        this.dim = trackArea;
        this.mode = mode;
        this.frameLength = frameLength;
        this.viewPortLength = viewPortLength;
        this.onSetOffset = onSetOffset;
        this.maxScrollBarOffset = this.frameLength - this.viewPortLength;
    }

    public ScrollBarComponent(Dim2i scrollBarArea, Mode mode, int frameLength, int viewPortLength, Consumer<Integer> onSetOffset, Dim2i extendedTrackArea) {
        this(scrollBarArea, mode, frameLength, viewPortLength, onSetOffset);
        this.extendedScrollArea = extendedTrackArea;
    }

    public void updateThumbPosition() {
        int scrollThumbLength = this.viewPortLength * (this.mode == ScrollBarComponent.Mode.VERTICAL ? this.dim.height() : this.dim.width() - 6) / this.frameLength;
        int maximumScrollThumbOffset = this.viewPortLength - scrollThumbLength;
        int scrollThumbOffset = this.offset * maximumScrollThumbOffset / this.maxScrollBarOffset;
        this.scrollThumb = new Dim2i(this.dim.x() + 2 + (this.mode == ScrollBarComponent.Mode.HORIZONTAL ? scrollThumbOffset : 0), this.dim.y() + 2 + (this.mode == ScrollBarComponent.Mode.VERTICAL ? scrollThumbOffset : 0), (this.mode == ScrollBarComponent.Mode.VERTICAL ? this.dim.width() : scrollThumbLength) - 4, (this.mode == ScrollBarComponent.Mode.VERTICAL ? scrollThumbLength : this.dim.height()) - 4);
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), -5592406);
        this.drawRect(drawContext, this.scrollThumb.x(), this.scrollThumb.y(), this.scrollThumb.getLimitX(), this.scrollThumb.getLimitY(), -5592406);
        if (this.isFocused()) {
            this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), -1);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.dim.containsCursor(mouseX, mouseY)) {
            if (this.scrollThumb.containsCursor(mouseX, mouseY)) {
                if (this.mode == ScrollBarComponent.Mode.VERTICAL) {
                    this.scrollThumbClickOffset = (int)(mouseY - ((double)this.scrollThumb.y() + (double)this.scrollThumb.height() / (double)2.0F));
                } else {
                    this.scrollThumbClickOffset = (int)(mouseX - ((double)this.scrollThumb.x() + (double)this.scrollThumb.width() / (double)2.0F));
                }

                this.isDragging = true;
            } else {
                int value;
                if (this.mode == ScrollBarComponent.Mode.VERTICAL) {
                    value = (int)((mouseY - (double)this.dim.y() - (double)this.scrollThumb.height() / (double)2.0F) / (double)(this.dim.height() - this.scrollThumb.height()) * (double)this.maxScrollBarOffset);
                } else {
                    value = (int)((mouseX - (double)this.dim.x() - (double)this.scrollThumb.width() / (double)2.0F) / (double)(this.dim.width() - this.scrollThumb.width()) * (double)this.maxScrollBarOffset);
                }

                this.setOffset(value);
                this.isDragging = false;
            }

            return true;
        } else {
            this.isDragging = false;
            return false;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isDragging) {
            int value;
            if (this.mode == ScrollBarComponent.Mode.VERTICAL) {
                value = (int)((mouseY - (double)this.scrollThumbClickOffset - (double)this.dim.y() - (double)this.scrollThumb.height() / (double)2.0F) / (double)(this.dim.height() - this.scrollThumb.height()) * (double)this.maxScrollBarOffset);
            } else {
                value = (int)((mouseX - (double)this.scrollThumbClickOffset - (double)this.dim.x() - (double)this.scrollThumb.width() / (double)2.0F) / (double)(this.dim.width() - this.scrollThumb.width()) * (double)this.maxScrollBarOffset);
            }

            this.setOffset(value);
            return true;
        } else {
            return false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if ((this.dim.containsCursor(mouseX, mouseY) || this.extendedScrollArea != null && this.extendedScrollArea.containsCursor(mouseX, mouseY)) && this.offset <= this.maxScrollBarOffset && this.offset >= 0) {
            int value = (int)((double)this.offset - verticalAmount * (double)6.0F);
            this.setOffset(value);
            return true;
        } else {
            return false;
        }
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int value) {
        this.offset = Math.clamp(value, 0, this.maxScrollBarOffset);
        this.updateThumbPosition();
        this.onSetOffset.accept(this.offset);
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!this.isFocused()) {
            return false;
        } else {
            if (this.mode == ScrollBarComponent.Mode.VERTICAL) {
                if (keyCode == 200) {
                    this.setOffset(this.getOffset() - 6);
                    return true;
                }

                if (keyCode == 208) {
                    this.setOffset(this.getOffset() + 6);
                    return true;
                }
            } else {
                if (keyCode == 203) {
                    this.setOffset(this.getOffset() - 6);
                    return true;
                }

                if (keyCode == 205) {
                    this.setOffset(this.getOffset() + 6);
                    return true;
                }
            }

            return false;
        }
    }

    public enum Mode {
        HORIZONTAL,
        VERTICAL
    }
}
