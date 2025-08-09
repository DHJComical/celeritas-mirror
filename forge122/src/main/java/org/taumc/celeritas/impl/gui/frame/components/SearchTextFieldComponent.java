package org.taumc.celeritas.impl.gui.frame.components;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.widgets.AbstractWidget;
import org.taumc.celeritas.impl.util.ComponentUtil;
import org.taumc.celeritas.impl.util.Dim2i;

public class SearchTextFieldComponent extends AbstractWidget {
    protected final Dim2i dim;
    protected final List<OptionPage> pages;
    private final FontRenderer textRenderer;
    private final SearchTextFieldModel model;

    public SearchTextFieldComponent(Dim2i dim, List<OptionPage> pages, SearchTextFieldModel model) {
        this.textRenderer = Minecraft.getMinecraft().fontRenderer;
        this.dim = dim;
        this.pages = pages;
        this.model = model;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.model.innerWidth = this.getInnerWidth();
        if (this.isVisible()) {
            if (this.model.text.isEmpty()) {
                this.drawString(context, ComponentUtil.translatable("celeritas.search_bar_empty"), this.dim.x() + 6, this.dim.y() + 6, -5592406);
            }

            this.drawRect(context, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), this.isFocused() ? -536870912 : -1879048192);
            int j = this.model.selectionStart - this.model.firstCharacterIndex;
            int k = this.model.selectionEnd - this.model.firstCharacterIndex;
            String string = this.textRenderer.trimStringToWidth(this.model.text.substring(this.model.firstCharacterIndex), this.getInnerWidth());
            boolean bl = j >= 0 && j <= string.length();
            int l = this.dim.x() + 6;
            int m = this.dim.y() + 6;
            int n = l;
            if (k > string.length()) {
                k = string.length();
            }

            if (!string.isEmpty()) {
                String string2 = bl ? string.substring(0, j) : string;
                n = context.drawString(this.textRenderer, string2, l, m, -1);
            }

            boolean bl3 = this.model.selectionStart < this.model.text.length() || this.model.text.length() >= this.model.getMaxLength();
            int o = n;
            if (!bl) {
                o = j > 0 ? l + this.dim.width() - 12 : l;
            } else if (bl3) {
                o = n - 1;
                --n;
            }

            if (!string.isEmpty() && bl && j < string.length()) {
                context.drawString(this.textRenderer, string.substring(j), n, m, -1);
            }

            if (this.isFocused()) {
                context.fill(o, m - 1, o + 1, m + 1 + this.textRenderer.FONT_HEIGHT, -3092272);
            }

            if (k != j) {
                int p = l + this.textRenderer.getStringWidth(string.substring(0, k));
                this.drawSelectionHighlight(context, o, m - 1, p - 1, m + 1 + this.textRenderer.FONT_HEIGHT);
            }

        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = (int)(Math.floor(mouseX) - (double)this.dim.x() - (double)6.0F);
        String string = this.textRenderer.trimStringToWidth(this.model.text.substring(this.model.firstCharacterIndex), this.getInnerWidth());
        this.model.setCursor(this.textRenderer.trimStringToWidth(string, i).length() + this.model.firstCharacterIndex);

        this.setFocused(this.dim.containsCursor(mouseX, mouseY));
        return this.isFocused();
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    private void drawSelectionHighlight(GuiGraphics context, int x1, int y1, int x2, int y2) {
        if (x1 < x2) {
            int i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            int i = y1;
            y1 = y2;
            y2 = i;
        }

        if (x2 > this.dim.x() + this.dim.width()) {
            x2 = this.dim.x() + this.dim.width();
        }

        if (x1 > this.dim.x() + this.dim.width()) {
            x1 = this.dim.x() + this.dim.width();
        }

        context.fill(x1, y1, x2, y2, -16776961);
    }

    public boolean isActive() {
        return this.isVisible() && this.isFocused() && this.isEditable();
    }

    @Override
    public boolean charTyped(char chr) {
        if (!this.isActive()) {
            return false;
        } else if (ChatAllowedCharacters.isAllowedCharacter(chr)) {
            if (this.model.editable) {
                this.model.write(Character.toString(chr));
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!this.isActive()) {
            return false;
        } else {
            this.model.selecting = GuiScreen.isShiftKeyDown();
            if (GuiScreen.isKeyComboCtrlA(keyCode)) {
                this.model.setCursorToEnd();
                this.model.setSelectionEnd(0);
                return true;
            } else if (GuiScreen.isKeyComboCtrlC(keyCode)) {
                GuiScreen.setClipboardString(this.model.getSelectedText());
                return true;
            } else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
                if (this.model.editable) {
                    this.model.write(GuiScreen.getClipboardString());
                }

                return true;
            } else if (GuiScreen.isKeyComboCtrlX(keyCode)) {
                GuiScreen.setClipboardString(this.model.getSelectedText());
                if (this.model.editable) {
                    this.model.write("");
                }

                return true;
            } else {
                switch (keyCode) {
                    case 14 -> { // Backspace
                        if (this.model.editable) {
                            this.model.selecting = false;
                            this.model.erase(-1);
                            this.model.selecting = GuiScreen.isShiftKeyDown();
                        }
                        return true;
                    }
                    case 211 -> { // Delete
                        if (this.model.editable) {
                            this.model.selecting = false;
                            this.model.erase(1);
                            this.model.selecting = GuiScreen.isShiftKeyDown();
                        }
                        return true;
                    }
                    case 205 -> { // Right arrow
                        if (GuiScreen.isCtrlKeyDown()) {
                            this.model.setCursor(this.model.getWordSkipPosition(1));
                        } else {
                            this.model.moveCursor(1);
                        }
                        boolean state = this.model.getCursor() != this.model.lastCursorPosition && this.model.getCursor() != this.model.text.length() + 1;
                        this.model.lastCursorPosition = this.model.getCursor();
                        return state;
                    }
                    case 203 -> { // Left arrow
                        if (GuiScreen.isCtrlKeyDown()) {
                            this.model.setCursor(this.model.getWordSkipPosition(-1));
                        } else {
                            this.model.moveCursor(-1);
                        }
                        boolean state = this.model.getCursor() != this.model.lastCursorPosition && this.model.getCursor() != 0;
                        this.model.lastCursorPosition = this.model.getCursor();
                        return state;
                    }
                    case 199 -> { // Home
                        this.model.setCursorToStart();
                        return true;
                    }
                    case 207 -> { // End
                        this.model.setCursorToEnd();
                        return true;
                    }
                    default -> {
                        return false;
                    }
                }
            }
        }
    }

    public boolean isVisible() {
        return this.model.visible;
    }

    public boolean isEditable() {
        return this.model.editable;
    }

    public int getInnerWidth() {
        return this.dim.width() - 12;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }
}
