package org.embeddedt.embeddium.impl.gui;

import com.google.common.collect.Multimap;
//? if >=1.15
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.impl.gui.console.Console;
import org.embeddedt.embeddium.impl.gui.console.message.MessageLevel;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.gui.widgets.FlatButtonWidget;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import org.embeddedt.embeddium.impl.util.Dim2i;
import net.minecraft.client.Minecraft;
//? if <1.20 {
/*import org.embeddedt.embeddium.impl.gui.compat.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
*///?}
//? if >=1.20
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21 {
/*import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
*///?} else
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.frame.AbstractFrame;
import org.embeddedt.embeddium.impl.gui.frame.BasicFrame;
import org.embeddedt.embeddium.impl.gui.frame.components.SearchTextFieldComponent;
import org.embeddedt.embeddium.impl.gui.frame.components.SearchTextFieldModel;
import org.embeddedt.embeddium.impl.gui.frame.tab.Tab;
import org.embeddedt.embeddium.impl.gui.frame.tab.TabFrame;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class EmbeddiumVideoOptionsScreen extends Screen {
    private static final AtomicReference<Component> tabFrameSelectedTab = new AtomicReference<>(null);
    private final AtomicReference<Integer> tabFrameScrollBarOffset = new AtomicReference<>(0);
    private final AtomicReference<Integer> optionPageScrollBarOffset = new AtomicReference<>(0);

    private final Screen prevScreen;
    private final List<OptionPage> pages = new ArrayList<>();
    private AbstractFrame frame;
    private FlatButtonWidget applyButton, closeButton, undoButton;

    private boolean hasPendingChanges;

    private SearchTextFieldComponent searchTextField;
    private final SearchTextFieldModel searchTextModel;

    private boolean firstInit = true;

    public EmbeddiumVideoOptionsScreen(Screen prev) {
        super(ComponentUtil.literal("Embeddium Options"));
        this.prevScreen = prev;
        this.pages.add(SodiumGameOptionPages.general());
        this.pages.add(SodiumGameOptionPages.quality());
        this.pages.add(SodiumGameOptionPages.performance());
        this.pages.add(SodiumGameOptionPages.advanced());

        OptionGUIConstructionEvent.BUS.post(new OptionGUIConstructionEvent(this.pages));

        this.searchTextModel = new SearchTextFieldModel(this.pages, this);
        registerTextures();
    }

    private void registerTextures() {
    }


    public void rebuildUI() {
        // Remember if the search bar was previously focused since we'll lose that information after recreating
        // the widget.
        boolean wasSearchFocused = this.searchTextField.isFocused();
        //? if >=1.19 {
        this.rebuildWidgets();
        //?} else
        /*this.init();*/
        if(wasSearchFocused) {
            this.setFocused(this.searchTextField);
        }
    }

    @Override
    protected void init() {
        this.frame = this.parentFrameBuilder().build();
        //? if >=1.18 {
        this.addRenderableWidget(this.frame);
        //?} else if >=1.17 {
        /*this.addRenderableOnly(this.frame);
        ((List)this.children()).add(this.frame);
        *///?} else if >=1.16 {
        /*this.addWidget(this.frame);
        *///?} else
        /*this.children.add(this.frame);*/

        this.setFocused(this.frame);

        if(firstInit) {
            this.setFocused(this.searchTextField);
            firstInit = false;
        }
    }

    private static final float ASPECT_RATIO = 5f / 4f;
    private static final int MINIMUM_WIDTH = 550;

    protected BasicFrame.Builder parentFrameBuilder() {
        BasicFrame.Builder basicFrameBuilder;

        // Apply aspect ratio clamping on wide enough screens
        int newWidth = this.width;
        if (newWidth > MINIMUM_WIDTH && (float) this.width / (float) this.height > ASPECT_RATIO) {
            newWidth = Math.max(MINIMUM_WIDTH, (int) (this.height * ASPECT_RATIO));
        }

        Dim2i basicFrameDim = new Dim2i((this.width - newWidth) / 2, 0, newWidth, this.height);
        Dim2i tabFrameDim = new Dim2i(basicFrameDim.x() + basicFrameDim.width() / 20 / 2, basicFrameDim.y() + basicFrameDim.height() / 4 / 2, basicFrameDim.width() - (basicFrameDim.width() / 20), basicFrameDim.height() / 4 * 3);

        Dim2i undoButtonDim = new Dim2i(tabFrameDim.getLimitX() - 203, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i applyButtonDim = new Dim2i(tabFrameDim.getLimitX() - 134, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i closeButtonDim = new Dim2i(tabFrameDim.getLimitX() - 65, tabFrameDim.getLimitY() + 5, 65, 20);


        this.undoButton = new FlatButtonWidget(undoButtonDim, ComponentUtil.translatable("sodium.options.buttons.undo"), this::undoChanges);
        this.applyButton = new FlatButtonWidget(applyButtonDim, ComponentUtil.translatable("sodium.options.buttons.apply"), this::applyChanges);
        this.closeButton = new FlatButtonWidget(closeButtonDim, ComponentUtil.translatable("gui.done"), this::onClose);

        Dim2i searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width(), 20);

        basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);

        this.searchTextField = new SearchTextFieldComponent(searchTextFieldDim, this.pages, this.searchTextModel);

        basicFrameBuilder.addChild(dim -> this.searchTextField);

        return basicFrameBuilder;
    }

    private boolean canShowPage(OptionPage page) {
        if(page.getGroups().isEmpty()) {
            return false;
        }

        // Check if any options on this page are visible
        var predicate = searchTextModel.getOptionPredicate();

        for(OptionGroup group : page.getGroups()) {
            for(Option<?> option : group.getOptions()) {
                if(predicate.test(option)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void createShaderPackButton(Multimap<String, Tab<?>> tabs) {
        if(this.searchTextModel.getOptionPredicate().test(null) && ShaderModBridge.isShaderModPresent()) {
            tabs.put(EmbeddiumConstants.MODID, Tab.createBuilder()
                    .setTitle(ComponentUtil.translatable("options.iris.shaderPackSelection"))
                    .setId(OptionIdentifier.create(EmbeddiumConstants.MODID, "shader_packs"))
                    .setOnSelectFunction(() -> {
                        if(ShaderModBridge.openShaderScreen(this) instanceof Screen screen) {
                            this.minecraft.setScreen(screen);
                        }
                        return false;
                    })
                    .build());
        }
    }

    private AbstractFrame createTabFrame(Dim2i tabFrameDim) {
        // TabFrame will automatically expand its height to fit all tabs, so the scrollable frame can handle it
        return TabFrame.createBuilder()
                .setDimension(tabFrameDim)
                .shouldRenderOutline(false)
                .setTabSectionScrollBarOffset(tabFrameScrollBarOffset)
                .setTabSectionSelectedTab(tabFrameSelectedTab)
                .addTabs(tabs -> this.pages
                        .stream()
                        .filter(this::canShowPage)
                        .forEach(page -> tabs.put(page.getId().getModId(), Tab.createBuilder().from(page, searchTextModel.getOptionPredicate(), optionPageScrollBarOffset)))
                )
                .addTabs(this::createShaderPackButton)
                .onSetTab(() -> {
                    optionPageScrollBarOffset.set(0);
                })
                .build();
    }

    public BasicFrame.Builder parentBasicFrameBuilder(Dim2i parentBasicFrameDim, Dim2i tabFrameDim) {
        return BasicFrame.createBuilder()
                .setDimension(parentBasicFrameDim)
                .shouldRenderOutline(false)
                .addChild(parentDim -> this.createTabFrame(tabFrameDim))
                .addChild(dim -> this.undoButton)
                .addChild(dim -> this.applyButton)
                .addChild(dim -> this.closeButton);
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

        this.updateControls();

        this.frame.render(drawContext, mouseX, mouseY, delta);
    }

    private void updateControls() {
        boolean hasChanges = this.getAllOptions()
                .anyMatch(Option::hasChanged);

        this.applyButton.setEnabled(hasChanges);
        this.undoButton.setVisible(hasChanges);
        this.closeButton.setEnabled(!hasChanges);

        this.hasPendingChanges = hasChanges;
    }

    private Stream<Option<?>> getAllOptions() {
        return this.pages.stream()
                .flatMap(s -> s.getOptions().stream());
    }

    private void applyChanges() {
        final HashSet<OptionStorage<?>> dirtyStorages = new HashSet<>();
        final EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);

        this.getAllOptions().forEach((option -> {
            if (!option.hasChanged()) {
                return;
            }

            option.applyChanges();

            flags.addAll(option.getFlags());
            dirtyStorages.add(option.getStorage());
        }));

        if (flags.contains(OptionFlag.REQUIRES_GAME_RESTART)) {
            Console.instance().logMessage(MessageLevel.WARN,
                    ComponentUtil.translatable("sodium.console.game_restart"), 10.0);
        }

        for (OptionStorage<?> storage : dirtyStorages) {
            storage.save(flags);
        }

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

    private void undoChanges() {
        this.getAllOptions()
                .forEach(Option::reset);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_P && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 && !(this.searchTextField != null && this.searchTextField.isFocused())) {
            Minecraft.getInstance().setScreen(new VideoSettingsScreen(this.prevScreen, /*? if >=1.21 {*/ /*Minecraft.getInstance(), *//*?}*/ Minecraft.getInstance().options));

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.hasPendingChanges;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.prevScreen);
    }
}
