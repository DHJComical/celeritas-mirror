package org.embeddedt.embeddium.impl.gui.frame.components;

import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;
import org.embeddedt.embeddium.impl.gui.widgets.AbstractWidget;
import org.embeddedt.embeddium.impl.util.Dim2i;

import java.util.function.Consumer;

public class ScrollBarComponent extends AbstractWidget {
    protected static final int SCROLL_OFFSET = 6;

    protected final Dim2i dim;

    private final Mode mode;
    private final int frameLength;
    private final int viewPortLength;
    private final int maxScrollBarOffset;
    private final Consumer<Integer> onSetOffset;
    private int offset = 0;
    private boolean isDragging;

    private Dim2i scrollThumb = null;
    private int scrollThumbClickOffset;

    private Dim2i extendedScrollArea = null;

    public ScrollBarComponent(Dim2i trackArea, Mode mode, int frameLength, int viewPortLength, Consumer<Integer> onSetOffset) {
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
        int scrollThumbLength = (this.viewPortLength * (this.mode == Mode.VERTICAL ? this.dim.height() : this.dim.width() - 6)) / this.frameLength;
        int maximumScrollThumbOffset = this.viewPortLength - scrollThumbLength;
        int scrollThumbOffset = this.offset * maximumScrollThumbOffset / this.maxScrollBarOffset;
        this.scrollThumb = new Dim2i(this.dim.x() + 2 + (this.mode == Mode.HORIZONTAL ? scrollThumbOffset : 0), this.dim.y() + 2 + (this.mode == Mode.VERTICAL ? scrollThumbOffset : 0), (this.mode == Mode.VERTICAL ? this.dim.width() : scrollThumbLength) - 4, (this.mode == Mode.VERTICAL ? scrollThumbLength : this.dim.height()) - 4);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawContext.drawBorder(this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), 0xFFAAAAAA);
        drawContext.fill(this.scrollThumb.x(), this.scrollThumb.y(), this.scrollThumb.getLimitX(), this.scrollThumb.getLimitY(), 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
        if (this.dim.containsCursor(mouseX, mouseY)) {
            if (this.scrollThumb.containsCursor(mouseX, mouseY)) {
                if (this.mode == Mode.VERTICAL) {
                    this.scrollThumbClickOffset = (int) (mouseY - (this.scrollThumb.y() + this.scrollThumb.height() / 2));
                } else {
                    this.scrollThumbClickOffset = (int) (mouseX - (this.scrollThumb.x() + this.scrollThumb.width() / 2));
                }
                this.isDragging = true;
            } else {
                int value;
                if (this.mode == Mode.VERTICAL) {
                    value = (int) ((mouseY - this.dim.y() - (this.scrollThumb.height() / 2)) / (this.dim.height() - this.scrollThumb.height()) * this.maxScrollBarOffset);
                } else {
                    value = (int) ((mouseX - this.dim.x() - (this.scrollThumb.width() / 2)) / (this.dim.width() - this.scrollThumb.width()) * this.maxScrollBarOffset);
                }
                this.setOffset(value);
                this.isDragging = false;
            }
            return true;
        }
        this.isDragging = false;
        return false;
    }

    @Override
    public boolean mouseReleased(InteractionContext context, double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(InteractionContext context, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isDragging) {
            int value;
            if (this.mode == Mode.VERTICAL) {
                value = (int) ((mouseY - this.scrollThumbClickOffset - this.dim.y() - (this.scrollThumb.height() / 2)) / (this.dim.height() - this.scrollThumb.height()) * this.maxScrollBarOffset);
            } else {
                value = (int) ((mouseX - this.scrollThumbClickOffset - this.dim.x() - (this.scrollThumb.width() / 2)) / (this.dim.width() - this.scrollThumb.width()) * this.maxScrollBarOffset);
            }
            this.setOffset(value);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(InteractionContext context, double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.dim.containsCursor(mouseX, mouseY) || this.extendedScrollArea != null && this.extendedScrollArea.containsCursor(mouseX, mouseY)) {
            if (this.offset <= this.maxScrollBarOffset && this.offset >= 0) {
                int value = (int) (this.offset - verticalAmount * SCROLL_OFFSET); // todo: horizontal separation
                this.setOffset(value);
                return true;
            }
        }
        return false;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int value) {
        this.offset = MathUtil.clamp(value, 0, this.maxScrollBarOffset);
        this.updateThumbPosition();
        this.onSetOffset.accept(this.offset);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY) || this.extendedScrollArea.containsCursor(mouseX, mouseY);
    }

    public enum Mode {
        HORIZONTAL,
        VERTICAL
    }
}