package org.embeddedt.embeddium.impl.gui.widgets;

//? if >=1.20 {
import net.minecraft.client.InputType;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.impl.gui.compat.GuiGraphics;
import org.embeddedt.embeddium.impl.gui.compat.Renderable;
*///?}
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractWidget implements Renderable, GuiEventListener, NarratableEntry {
    protected final Font font;
    protected boolean focused;
    protected boolean hovered;

    protected AbstractWidget() {
        this.font = Minecraft.getInstance().font;
    }

    //? if <1.20 {
    /*@Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        render(new GuiGraphics(poseStack), mouseX, mouseY, partialTick);
    }
    *///?}

    protected void drawString(GuiGraphics drawContext, String str, int x, int y, int color) {
        drawContext.drawString(this.font, str, x, y, color);
    }

    protected void drawString(GuiGraphics drawContext, Component text, int x, int y, int color) {
        drawContext.drawString(this.font, text, x, y, color);
    }

    public boolean isHovered() {
        return this.hovered;
    }

    protected void drawRect(GuiGraphics drawContext, int x1, int y1, int x2, int y2, int color) {
        drawContext.fill(x1, y1, x2, y2, color);
    }

    protected void playClickSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK /*? if >=1.20 {*/ .value() /*?}*/, 1.0F));
    }

    protected int getStringWidth(FormattedText text) {
        return this.font.width(text);
    }

    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        }
        if (this.hovered) {
            return NarratableEntry.NarrationPriority.HOVERED;
        }
        return NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        if (this.focused) {
            builder.add(NarratedElementType.USAGE, ComponentUtil.translatable("narration.button.usage.focused"));
        } else if (this.hovered) {
            builder.add(NarratedElementType.USAGE, ComponentUtil.translatable("narration.button.usage.hovered"));
        }
    }

    //? if >=1.20 {

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        return !this.isFocused() ? ComponentPath.leaf(this) : null;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) {
            this.focused = false;
        } else {
            InputType guiNavigationType = Minecraft.getInstance().getLastInputType();
            if (guiNavigationType == InputType.KEYBOARD_TAB || guiNavigationType == InputType.KEYBOARD_ARROW) {
                this.focused = true;
            }
        }
    }

    //?} else {
    /*public boolean isFocused() {
        return this.focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
    *///?}

    protected void drawBorder(GuiGraphics drawContext, int x1, int y1, int x2, int y2, int color) {
        drawContext.fill(x1, y1, x2, y1 + 1, color);
        drawContext.fill(x1, y2 - 1, x2, y2, color);
        drawContext.fill(x1, y1, x1 + 1, y2, color);
        drawContext.fill(x2 - 1, y1, x2, y2, color);
    }

    protected static boolean keySelected(int keyCode) {
        return keyCode == InputConstants.KEY_SPACE || keyCode == InputConstants.KEY_RETURN;
    }
}
