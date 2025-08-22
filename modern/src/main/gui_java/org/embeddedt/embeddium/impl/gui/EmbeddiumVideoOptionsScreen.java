package org.embeddedt.embeddium.impl.gui;

import com.google.common.collect.Multimap;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.impl.gui.frame.tab.Tab;
import org.embeddedt.embeddium.impl.gui.framework.ModernDrawContext;
import org.embeddedt.embeddium.impl.gui.framework.ModernInteractionContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public class EmbeddiumVideoOptionsScreen extends Screen {
    private final Screen prevScreen;
    private final CeleritasVideoOptionsController controller;

    public EmbeddiumVideoOptionsScreen(Screen prev) {
        super(ComponentUtil.literal("Embeddium Options"));
        this.prevScreen = prev;
        this.controller = new CeleritasVideoOptionsController(this::onClose, new ModernDrawContext(new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource()), Minecraft.getInstance().font)) {
            @Override
            protected void createExtraTabs(Multimap<String, Tab<?>> tabs) {
                if(ShaderModBridge.isShaderModPresent()) {
                    tabs.put(EmbeddiumConstants.MODID, Tab.createBuilder()
                            .setTitle(TextComponent.translatable("options.iris.shaderPackSelection"))
                            .setId(OptionIdentifier.create(EmbeddiumConstants.MODID, "shader_packs"))
                            .setOnSelectFunction(() -> {
                                if(ShaderModBridge.openShaderScreen(this) instanceof Screen screen) {
                                    Minecraft.getInstance().setScreen(screen);
                                }
                                return false;
                            })
                            .build());
                }
            }

            @Override
            protected void applyFlagSideEffects(Set<OptionFlag> flags) {
                super.applyFlagSideEffects(flags);

                Minecraft client = Minecraft.getInstance();

                if (client.level != null) {
                    if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
                        client.levelRenderer.allChanged();
                    } else if (flags.contains(OptionFlag.REQUIRES_RENDERER_UPDATE)) {
                        client.levelRenderer.needsUpdate();
                    }
                }

                if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
                    client.updateMaxMipLevel(client.options.mipmapLevels/*? if >=1.19 {*/().get()/*?}*/);
                    client.delayTextureReload();
                }
            }
        };
    }

    @Override
    protected void init() {
        this.controller.init(this.width, this.height);
    }

    //? if >=1.20 {
    @Override public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    //?} else if >=1.16 <1.20 {
    /*@Override public void render(PoseStack matrices, int mouseX, int mouseY, float delta) { GuiGraphics drawContext = new GuiGraphics(matrices);
    *///?} else
    /*@Override public void render(int mouseX, int mouseY, float delta) { GuiGraphics drawContext = new GuiGraphics();*/
        //? if >=1.20 <1.20.2 {
        this.renderBackground(drawContext);
        //?} else if >=1.20.2 {
        /*this.renderBackground(drawContext, mouseX, mouseY, delta);
        *///?} else if >=1.16 {
        /*this.renderBackground(drawContext.pose());
        *///?} else
        /*this.renderBackground();*/

        this.controller.render(new ModernDrawContext(drawContext, this.font), mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.controller.getFrame().mouseClicked(ModernInteractionContext.INSTANCE, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.controller.getFrame().mouseReleased(ModernInteractionContext.INSTANCE, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.controller.getFrame().mouseDragged(ModernInteractionContext.INSTANCE, mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, /*? if >=1.20.2 {*/ /*double horizontalAmount, *//*?}*/ double verticalAmount) {
        //? if <1.20.2
        double horizontalAmount = 0;
        return this.controller.getFrame().mouseScrolled(ModernInteractionContext.INSTANCE, mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_P && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            Minecraft.getInstance().setScreen(new VideoSettingsScreen(this.prevScreen, /*? if >=1.21 {*/ /*Minecraft.getInstance(), *//*?}*/ Minecraft.getInstance().options));

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.controller.isHasPendingChanges();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.prevScreen);
    }
}
