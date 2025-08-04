package org.taumc.celeritas.impl.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.taumc.celeritas.api.OptionGUIConstructionEvent;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionFlag;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.api.options.structure.OptionStorage;
import org.taumc.celeritas.impl.gui.compat.Element;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.frame.AbstractFrame;
import org.taumc.celeritas.impl.gui.frame.BasicFrame;
import org.taumc.celeritas.impl.gui.frame.OptionPageFrame;
import org.taumc.celeritas.impl.gui.frame.components.SearchTextFieldComponent;
import org.taumc.celeritas.impl.gui.frame.components.SearchTextFieldModel;
import org.taumc.celeritas.impl.gui.frame.tab.Tab;
import org.taumc.celeritas.impl.gui.frame.tab.TabFrame;
import org.taumc.celeritas.impl.gui.widgets.FlatButtonWidget;
import org.taumc.celeritas.impl.util.ComponentUtil;
import org.taumc.celeritas.impl.util.Dim2i;

public class CeleritasVideoOptionsScreen extends GuiScreen {
    private static final AtomicReference<ITextComponent> tabFrameSelectedTab = new AtomicReference<>(null);
    private final AtomicReference<Integer> tabFrameScrollBarOffset = new AtomicReference<>(0);
    private final AtomicReference<Integer> optionPageScrollBarOffset = new AtomicReference<>(0);

    public final GuiScreen prevScreen;
    private final List<OptionPage> pages = new ArrayList<>();
    private AbstractFrame frame;
    private FlatButtonWidget applyButton, closeButton, undoButton;

    private boolean hasPendingChanges;

    private SearchTextFieldComponent searchTextField;
    private final SearchTextFieldModel searchTextModel;

    private double lastMouseX = -1;
    private double lastMouseY = -1;

    public CeleritasVideoOptionsScreen(GuiScreen prev) {
        this.prevScreen = prev;
        this.pages.add(SodiumGameOptionPages.general());
        this.pages.add(SodiumGameOptionPages.quality());
        this.pages.add(SodiumGameOptionPages.performance());
        this.pages.add(SodiumGameOptionPages.advanced());

        OptionGUIConstructionEvent.BUS.post(new OptionGUIConstructionEvent(this.pages));

        this.searchTextModel = new SearchTextFieldModel(this.pages, this);
    }

    public void rebuildUI() {
        boolean wasSearchFocused = this.searchTextField.isFocused();
        this.initGui();
        if (wasSearchFocused) {
            this.searchTextField.setFocused(true);
        }
    }

    @Override
    public void initGui() {
        this.frame = this.parentFrameBuilder().build();
    }

    private static final float ASPECT_RATIO = 5f / 4f;
    private static final int MINIMUM_WIDTH = 550;

    protected BasicFrame.Builder parentFrameBuilder() {


        int newWidth = this.width;
        if (newWidth > MINIMUM_WIDTH && (float) this.width / (float) this.height > ASPECT_RATIO) {
            newWidth = Math.max(MINIMUM_WIDTH, (int) (this.height * ASPECT_RATIO));
        }

        Dim2i basicFrameDim = new Dim2i((this.width - newWidth) / 2, 0, newWidth, this.height);
        Dim2i tabFrameDim = new Dim2i(basicFrameDim.x() + basicFrameDim.width() / 20 / 2, basicFrameDim.y() + basicFrameDim.height() / 4 / 2, basicFrameDim.width() - basicFrameDim.width() / 20, basicFrameDim.height() / 4 * 3);

        Dim2i undoButtonDim = new Dim2i(tabFrameDim.getLimitX() - 203, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i applyButtonDim = new Dim2i(tabFrameDim.getLimitX() - 134, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i closeButtonDim = new Dim2i(tabFrameDim.getLimitX() - 65, tabFrameDim.getLimitY() + 5, 65, 20);

        this.undoButton = new FlatButtonWidget(undoButtonDim, ComponentUtil.translatable("sodium.options.buttons.undo"), this::undoChanges);
        this.applyButton = new FlatButtonWidget(applyButtonDim, ComponentUtil.translatable("sodium.options.buttons.apply"), this::applyChanges);
        this.closeButton = new FlatButtonWidget(closeButtonDim, ComponentUtil.translatable("gui.done"), this::onClose);

        Dim2i searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width(), 20);

        BasicFrame.Builder basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);

        this.searchTextField = new SearchTextFieldComponent(searchTextFieldDim, this.pages, this.searchTextModel);

        basicFrameBuilder.addChild((dim) -> this.searchTextField);

        return basicFrameBuilder;
    }

    private boolean canShowPage(OptionPage page) {
        if (page.getGroups().isEmpty()) {
            return false;
        }

        // Check if any options on this page are visible
        var predicate = searchTextModel.getOptionPredicate();

        for (OptionGroup group : page.getGroups()) {
            for (Option<?> option : group.getOptions()) {
                if (predicate.test(option)) {
                    return true;
                }
            }
        }

        return false;
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

    @Override
    public void drawScreen(int mouseX, int mouseY, float delta) {
        this.drawDefaultBackground();

        this.updateControls();

        this.frame.render(new GuiGraphics(), mouseX, mouseY, delta);
    }

    private void updateControls() {
        boolean hasChanges = this.getAllOptions().anyMatch(Option::hasChanged);

        this.applyButton.setEnabled(hasChanges);
        this.undoButton.setVisible(hasChanges);
        this.closeButton.setEnabled(!hasChanges);

        this.hasPendingChanges = hasChanges;
    }

    private Stream<Option<?>> getAllOptions() {
        return this.pages.stream().flatMap((s) -> s.getOptions().stream());
    }

    private void applyChanges() {
        HashSet<OptionStorage<?>> dirtyStorages = new HashSet<>();
        EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);

        this.getAllOptions().forEach((option -> {
            if (!option.hasChanged()) {
                return;
            }

            option.applyChanges();

            flags.addAll(option.getFlags());
            dirtyStorages.add(option.getStorage());
        }));

        if (flags.contains(OptionFlag.REQUIRES_GAME_RESTART)) {
            //Console.instance().logMessage(MessageLevel.WARN, ComponentUtil.translatable("sodium.console.game_restart", new Object[0]), (double)10.0F);
        }

        for (OptionStorage<?> storage : dirtyStorages) {
            storage.save(flags);
        }

        Minecraft client = Minecraft.getMinecraft();

        if (client.world != null) {
            if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
                client.renderGlobal.loadRenderers();
            }

            if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
                client.getTextureMapBlocks().setMipmapLevels(this.mc.gameSettings.mipmapLevels);
                client.refreshResources();
            }
        }

    }

    private void undoChanges() {
        this.getAllOptions().forEach(Option::reset);
    }

    public boolean shouldCloseOnEsc() {
        return !this.hasPendingChanges;
    }

    public void onClose() {
        this.mc.displayGuiScreen(this.prevScreen);
        super.onGuiClosed();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        class RecursiveProcessor {
            void process(Element element, boolean applyOffset, int mouseX, int mouseY, int mouseButton) {
                int localMouseY = applyOffset ? mouseY + optionPageScrollBarOffset.get() : mouseY;

                element.mouseClicked(mouseX, localMouseY, mouseButton);

                boolean nextApplyOffset;
                if (element instanceof TabFrame) {
                    nextApplyOffset = false;
                } else if (element instanceof OptionPageFrame) {
                    nextApplyOffset = true;
                } else {
                    nextApplyOffset = applyOffset;
                }

                new ArrayList<>(element.children()).forEach(child -> process(child, nextApplyOffset, mouseX, mouseY, mouseButton));
            }
        }

        new RecursiveProcessor().process(this.frame, false, mouseX, mouseY, mouseButton);
    }


    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);

        class RecursiveProcessor {
            void process(Element element, boolean applyOffset, int mouseX, int mouseY, int mouseButton) {
                int localMouseY = applyOffset ? mouseY + optionPageScrollBarOffset.get() : mouseY;

                element.mouseReleased(mouseX, localMouseY, mouseButton);

                boolean nextApplyOffset;
                if (element instanceof TabFrame) {
                    nextApplyOffset = false;
                } else if (element instanceof OptionPageFrame) {
                    nextApplyOffset = true;
                } else {
                    nextApplyOffset = applyOffset;
                }

                new ArrayList<>(element.children()).forEach(child ->
                        process(child, nextApplyOffset, mouseX, mouseY, mouseButton));
            }
        }

        new RecursiveProcessor().process(this.frame, false, mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        double dx = lastMouseX >= 0 ? mouseX - lastMouseX : 0;
        double dy = lastMouseY >= 0 ? mouseY - lastMouseY : 0;

        class RecursiveProcessor {
            void process(Element element, boolean applyOffset, int mouseX, int mouseY, int mouseButton, double dx, double dy) {
                int localMouseY = applyOffset ? mouseY + optionPageScrollBarOffset.get() : mouseY;

                element.mouseDragged(mouseX, localMouseY, mouseButton, dx, dy);

                boolean nextApplyOffset;
                if (element instanceof TabFrame) {
                    nextApplyOffset = false;
                } else if (element instanceof OptionPageFrame) {
                    nextApplyOffset = true;
                } else {
                    nextApplyOffset = applyOffset;
                }

                new ArrayList<>(element.children()).forEach(child ->
                        process(child, nextApplyOffset, mouseX, mouseY, mouseButton, dx, dy));
            }
        }

        new RecursiveProcessor().process(this.frame, false, mouseX, mouseY, clickedMouseButton, dx, dy);

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if (keyCode != Keyboard.KEY_ESCAPE || this.shouldCloseOnEsc()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.onClose();
            } else if (keyCode == Keyboard.KEY_P && isShiftKeyDown()) {
                this.mc.displayGuiScreen(new GuiVideoSettings(this.prevScreen, this.mc.gameSettings));
            }
        }

        class RecursiveProcessor {
            void process(Element element) {
                List<Element> children = new ArrayList<>(element.children());
                children.forEach(child -> {
                    child.keyPressed(keyCode);
                    child.charTyped(typedChar);
                    process(child);
                });
            }
        }

        new RecursiveProcessor().process(this.frame);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            double scrollDelta = dWheel > 0 ? 1 : -1;

            class RecursiveProcessor {
                void process(Element element, int mouseX, int mouseY, double scrollDelta) {
                    int adjustedMouseY = mouseY + optionPageScrollBarOffset.get();

                    element.mouseScrolled(mouseX, adjustedMouseY, scrollDelta);

                    new ArrayList<>(element.children()).forEach(child ->
                            process(child, mouseX, mouseY, scrollDelta));
                }
            }

            new RecursiveProcessor().process(this.frame, mouseX, mouseY, scrollDelta);
        }
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h){
        super.onResize(mcIn, w, h);
        optionPageScrollBarOffset.set(0);
        rebuildUI();
    }
}
