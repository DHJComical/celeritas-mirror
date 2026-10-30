package org.embeddedt.embeddium.impl.gui.modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gui.CeleritasVideoOptionsController;
import org.embeddedt.embeddium.impl.gui.frame.tab.Tab;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.modern.framework.ModernDrawContext;
import org.embeddedt.embeddium.impl.gui.modern.framework.ModernInteractionContext;
import org.embeddedt.embeddium.impl.gui.options.CommonOptionPages;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import org.lwjgl.glfw.GLFW;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.OptionFlag;

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
                CommonOptionPages.performance(Celeritas.options())
        ), new ModernDrawContext(null, Minecraft.getInstance().font)) {
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
                    client.updateMaxMipLevel(client.options.mipmapLevels().get());
                    client.delayTextureReload();
                }
            }
        };
    }

    @Override
    protected void init() {
        this.controller.init(this.width, this.height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.controller.render(new ModernDrawContext(graphics, this.font), mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.controller.getFrame().mouseClicked(ModernInteractionContext.INSTANCE, event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.controller.getFrame().mouseReleased(ModernInteractionContext.INSTANCE, event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return this.controller.getFrame().mouseDragged(ModernInteractionContext.INSTANCE, event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.controller.getFrame().mouseScrolled(ModernInteractionContext.INSTANCE, mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_P && event.hasShiftDown()) {
            Minecraft.getInstance().setScreen(new VideoSettingsScreen(this.prevScreen, Minecraft.getInstance(), Minecraft.getInstance().options));

            return true;
        }

        return super.keyPressed(event);
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
