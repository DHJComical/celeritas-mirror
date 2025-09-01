package org.embeddedt.embeddium.impl.gui.modern;

import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gui.CeleritasVideoOptionsController;
import org.embeddedt.embeddium.impl.gui.frame.tab.Tab;
import org.embeddedt.embeddium.impl.gui.modern.framework.ModernDrawContext;
import org.embeddedt.embeddium.impl.gui.modern.framework.ModernInteractionContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.options.CommonOptionPages;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens./*? if >=1.21 {*//*options.*//*?}*/VideoSettingsScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EmbeddiumVideoOptionsScreen extends Screen {
    private final Screen prevScreen;
    private final CeleritasVideoOptionsController controller;

    public EmbeddiumVideoOptionsScreen(Screen prev) {
        super(ComponentUtil.literal("Embeddium Options"));
        this.prevScreen = prev;
        this.controller = new CeleritasVideoOptionsController(this::onClose, List.of(
                SodiumGameOptionPages.general(),
                SodiumGameOptionPages.quality(),
                CommonOptionPages.performance(Celeritas.options()),
                SodiumGameOptionPages.advanced()
        ), new ModernDrawContext(
                //? if >=1.20 {
                new net.minecraft.client.gui.GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource()),
                //?} else
                /*new com.mojang.blaze3d.vertex.PoseStack(),*/
                Minecraft.getInstance().font)) {
            @Override
            protected void createExtraTabs(Map<String, List<Tab<?>>> tabs) {
                if(ShaderModBridge.isShaderModPresent()) {
                    var id = OptionIdentifier.create(EmbeddiumConstants.MODID, "shader_packs");
                    tabs.computeIfAbsent(id.getModId(), $ -> new ArrayList<>()).add(Tab.createBuilder()
                            .setTitle(TextComponent.translatable("options.iris.shaderPackSelection"))
                            .setId(id)
                            .setOnSelectFunction(() -> {
                                if(ShaderModBridge.openShaderScreen(EmbeddiumVideoOptionsScreen.this) instanceof Screen screen) {
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
    @Override public void render(net.minecraft.client.gui.GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    //?} else if >=1.16 <1.20 {
    /*@Override public void render(com.mojang.blaze3d.vertex.PoseStack drawContext, int mouseX, int mouseY, float delta) {
    *///?} else
    /*@Override public void render(int mouseX, int mouseY, float delta) {*/
        //? if <1.20.2 {
        this.renderBackground(drawContext);
        //?} else {
        /*this.renderBackground(drawContext, mouseX, mouseY, delta);
        *///?}

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
