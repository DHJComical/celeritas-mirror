package org.embeddedt.embeddium.impl.gui.frame.components;

import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.InteractionContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.widgets.AbstractWidget;
import org.embeddedt.embeddium.impl.util.Dim2i;

public class SearchTextFieldComponent extends AbstractWidget {
    private static final TextComponent PLACEHOLDER = TextComponent.translatable("embeddium.search_bar_empty");

    private final Dim2i dim;
    private final SearchTextFieldModel model;

    public SearchTextFieldComponent(Dim2i dim, SearchTextFieldModel model) {
        this.dim = dim;
        this.model = model;
        this.model.innerWidth = dim.width() - 12;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawContext.fill(this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), this.model.focused ? 0xE0000000 : 0x90000000);

        int textX = this.dim.x() + 6;
        int textY = this.dim.y() + 6;

        if (this.model.text.isEmpty()) {
            drawContext.drawString(PLACEHOLDER, textX, textY, 0xFFAAAAAA);
        }

        String visible = drawContext.substrByWidth(this.model.text.substring(this.model.firstCharacterIndex), this.model.innerWidth);
        int cursor = this.model.selectionStart - this.model.firstCharacterIndex;
        int selectionEnd = Math.min(this.model.selectionEnd - this.model.firstCharacterIndex, visible.length());
        int lineTop = textY - 1;
        int lineBottom = textY + 1 + drawContext.lineHeight();

        int cursorX;
        if (cursor >= 0 && cursor <= visible.length()) {
            cursorX = textX + drawContext.getStringWidth(visible.substring(0, cursor));
        } else {
            cursorX = cursor > 0 ? textX + this.model.innerWidth : textX;
        }

        if (selectionEnd != cursor) {
            int selectionX = textX + drawContext.getStringWidth(visible.substring(0, selectionEnd));
            drawContext.fill(Math.min(cursorX, selectionX), lineTop, Math.min(Math.max(cursorX, selectionX), this.dim.getLimitX()), lineBottom, 0xFF0000FF);
        }

        if (!visible.isEmpty()) {
            drawContext.drawString(visible, textX, textY, 0xFFFFFFFF);
        }

        if (this.model.focused) {
            drawContext.fill(cursorX, lineTop, cursorX + 1, lineBottom, 0xFFD0D0D0);
        }
    }

    @Override
    public boolean mouseClicked(InteractionContext context, double mouseX, double mouseY, int button) {
        this.model.focused = this.dim.containsCursor(mouseX, mouseY);
        if (this.model.focused) {
            this.model.selecting = context.isSpecialKeyDown(InteractionContext.SpecialKey.SHIFT);
            this.model.setCursorFromX((int) Math.floor(mouseX) - this.dim.x() - 6);
        }
        return this.model.focused;
    }

    public boolean charTyped(int codePoint) {
        if (!this.model.focused || !SearchTextFieldModel.isAllowedCharacter(codePoint)) {
            return false;
        }

        this.model.write(Character.toString(codePoint));
        return true;
    }

    public boolean keyPressed(InteractionContext context, InteractionContext.EditAction action) {
        if (!this.model.focused) {
            return false;
        }

        boolean shift = context.isSpecialKeyDown(InteractionContext.SpecialKey.SHIFT);
        boolean ctrl = context.isSpecialKeyDown(InteractionContext.SpecialKey.CTRL);
        this.model.selecting = shift;

        switch (action) {
            case SELECT_ALL -> {
                this.model.setCursorToEnd();
                this.model.setSelectionEnd(0);
                return true;
            }
            case COPY -> {
                context.setClipboard(this.model.getSelectedText());
                return true;
            }
            case PASTE -> {
                this.model.write(context.getClipboard());
                return true;
            }
            case CUT -> {
                context.setClipboard(this.model.getSelectedText());
                this.model.write("");
                return true;
            }
            case BACKSPACE, DELETE -> {
                this.model.selecting = false;
                this.model.erase(action == InteractionContext.EditAction.BACKSPACE ? -1 : 1, ctrl);
                this.model.selecting = shift;
                return true;
            }
            case RIGHT -> {
                if (ctrl) {
                    this.model.setCursor(this.model.getWordSkipPosition(1));
                } else {
                    this.model.moveCursor(1);
                }
                boolean moved = this.model.getCursor() != this.model.lastCursorPosition && this.model.getCursor() != this.model.text.length() + 1;
                this.model.lastCursorPosition = this.model.getCursor();
                return moved;
            }
            case LEFT -> {
                if (ctrl) {
                    this.model.setCursor(this.model.getWordSkipPosition(-1));
                } else {
                    this.model.moveCursor(-1);
                }
                boolean moved = this.model.getCursor() != this.model.lastCursorPosition && this.model.getCursor() != 0;
                this.model.lastCursorPosition = this.model.getCursor();
                return moved;
            }
            case HOME -> {
                this.model.setCursorToStart();
                return true;
            }
            case END -> {
                this.model.setCursorToEnd();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }
}
