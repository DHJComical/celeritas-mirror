package org.embeddedt.embeddium.impl.gui.frame.components;

import net.minecraft.util.StringUtil;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.impl.gui.widgets.AbstractWidget;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import org.embeddedt.embeddium.impl.util.Dim2i;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=1.20 {
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
//?} else {
/*import org.embeddedt.embeddium.impl.gui.compat.GuiGraphics;
*///?}
import net.minecraft.client.gui.screens.Screen;
//? if >=1.20
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
//? if >=1.16.5
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BiFunction;

public class SearchTextFieldComponent extends AbstractWidget {
    protected final Dim2i dim;
    protected final List<OptionPage> pages;
    private final Font textRenderer = Minecraft.getInstance().font;
    private final SearchTextFieldModel model;

    public SearchTextFieldComponent(Dim2i dim, List<OptionPage> pages, SearchTextFieldModel model) {
        this.dim = dim;
        this.pages = pages;
        this.model = model;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.model.innerWidth = this.getInnerWidth();

        if (!this.isVisible()) {
            return;
        }
        if (this.model.text.isEmpty()) {
            this.drawString(context, ComponentUtil.translatable("embeddium.search_bar_empty"), this.dim.x() + 6, this.dim.y() + 6, 0xFFAAAAAA);
        }

        this.drawRect(context, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), this.isFocused() ? 0xE0000000 : 0x90000000);
        int j = this.model.selectionStart - this.model.firstCharacterIndex;
        int k = this.model.selectionEnd - this.model.firstCharacterIndex;
        //? if <1.16 {
        /*String string = this.textRenderer.substrByWidth(this.model.text.substring(this.model.firstCharacterIndex), this.getInnerWidth());
        *///?} else
        String string = this.textRenderer.plainSubstrByWidth(this.model.text.substring(this.model.firstCharacterIndex), this.getInnerWidth());
        boolean bl = j >= 0 && j <= string.length();
        int l = this.dim.x() + 6;
        int m = this.dim.y() + 6;
        int n = l;
        if (k > string.length()) {
            k = string.length();
        }
        if (!string.isEmpty()) {
            String string2 = bl ? string.substring(0, j) : string;
            n = context.drawString(this.textRenderer, string2, n, m, 0xFFFFFFFF);
        }
        boolean bl3 = this.model.selectionStart < this.model.text.length() || this.model.text.length() >= this.model.getMaxLength();
        int o = n;
        if (!bl) {
            o = j > 0 ? l + this.dim.width() - 12 : l;
        } else if (bl3) {
            --o;
            --n;
        }
        if (!string.isEmpty() && bl && j < string.length()) {
            context.drawString(this.textRenderer, string.substring(j), n, m, 0xFFFFFFFF);
        }
        // Cursor
        if (this.isFocused()) {
            context.fill(/*? if >=1.20 {*/RenderType.guiOverlay(),/*?}*/ o, m - 1, o + 1, m + 1 + this.textRenderer.lineHeight, -3092272);
        }
        // Highlighted text
        if (k != j) {
            int p = l + this.textRenderer.width(string.substring(0, k));
            this.drawSelectionHighlight(context, o, m - 1, p - 1, m + 1 + this.textRenderer.lineHeight);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = Mth.floor(mouseX) - this.dim.x() - 6;
        //? if <1.16 {
        /*String string = this.textRenderer.substrByWidth(this.model.text.substring(this.model.firstCharacterIndex), this.getInnerWidth());
        *///?} else
        String string = this.textRenderer.plainSubstrByWidth(this.model.text.substring(this.model.firstCharacterIndex), this.getInnerWidth());
        //? if <1.16 {
        /*this.model.setCursor(this.textRenderer.substrByWidth(string, i).length() + this.model.firstCharacterIndex);
        *///?} else
        this.model.setCursor(this.textRenderer.plainSubstrByWidth(string, i).length() + this.model.firstCharacterIndex);

        this.setFocused(this.dim.containsCursor(mouseX, mouseY));
        return this.isFocused();
    }

    // fixme: this is here because of 0.5.1's https://github.com/CaffeineMC/sodium-fabric/commit/20006a85fb7a64889f507eb13521e55693ae0d7e
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    private void drawSelectionHighlight(GuiGraphics context, int x1, int y1, int x2, int y2) {
        int i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }
        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }
        if (x2 > this.dim.x() + this.dim.width()) {
            x2 = this.dim.x() + this.dim.width();
        }
        if (x1 > this.dim.x() + this.dim.width()) {
            x1 = this.dim.x() + this.dim.width();
        }
        context.fill(/*? if >=1.20 {*/RenderType.guiTextHighlight(),/*?}*/ x1, y1, x2, y2, -16776961);
    }
    

    public boolean isActive() {
        return this.isVisible() && this.isFocused() && this.isEditable();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!this.isActive()) {
            return false;
        }
        //? if <1.20.6
        if (SharedConstants.isAllowedChatCharacter(chr)) {
        //? if >=1.20.6
        /*if (StringUtil.isAllowedChatCharacter(chr)) {*/
            if (this.model.editable) {
                this.model.write(Character.toString(chr));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isActive()) {
            return false;
        } else {
            this.model.selecting = Screen.hasShiftDown();
            if (Screen.isSelectAll(keyCode)) {
                this.model.setCursorToEnd();
                this.model.setSelectionEnd(0);
                return true;
            } else if (Screen.isCopy(keyCode)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.model.getSelectedText());
                return true;
            } else if (Screen.isPaste(keyCode)) {
                if (this.model.editable) {
                    this.model.write(Minecraft.getInstance().keyboardHandler.getClipboard());
                }

                return true;
            } else if (Screen.isCut(keyCode)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.model.getSelectedText());
                if (this.model.editable) {
                    this.model.write("");
                }

                return true;
            } else {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_BACKSPACE -> {
                        if (this.model.editable) {
                            this.model.selecting = false;
                            this.model.erase(-1);
                            this.model.selecting = Screen.hasShiftDown();
                        }
                        return true;
                    }
                    case GLFW.GLFW_KEY_DELETE -> {
                        if (this.model.editable) {
                            this.model.selecting = false;
                            this.model.erase(1);
                            this.model.selecting = Screen.hasShiftDown();
                        }
                        return true;
                    }
                    case GLFW.GLFW_KEY_RIGHT -> {
                        if (Screen.hasControlDown()) {
                            this.model.setCursor(this.model.getWordSkipPosition(1));
                        } else {
                            this.model.moveCursor(1);
                        }
                        boolean state = this.model.getCursor() != this.model.lastCursorPosition && this.model.getCursor() != this.model.text.length() + 1;
                        this.model.lastCursorPosition = this.model.getCursor();
                        return state;
                    }
                    case GLFW.GLFW_KEY_LEFT -> {
                        if (Screen.hasControlDown()) {
                            this.model.setCursor(this.model.getWordSkipPosition(-1));
                        } else {
                            this.model.moveCursor(-1);
                        }
                        boolean state = this.model.getCursor() != this.model.lastCursorPosition && this.model.getCursor() != 0;
                        this.model.lastCursorPosition = this.model.getCursor();
                        return state;
                    }
                    case GLFW.GLFW_KEY_HOME -> {
                        this.model.setCursorToStart();
                        return true;
                    }
                    case GLFW.GLFW_KEY_END -> {
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
        return model.visible;
    }

    public boolean isEditable() {
        return model.editable;
    }

    public int getInnerWidth() {
        return this.dim.width() - 12;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    //? if >=1.20 {
    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        if (!this.model.visible)
            return null;
        return super.nextFocusPath(navigation);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }
    //?}
}