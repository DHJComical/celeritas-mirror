package net.irisshaders.iris.gui.element;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.function.Function;

public class ShaderPackSelectionList extends IrisObjectSelectionList<ShaderPackSelectionList.BaseEntry> {
	private static final Component PACK_LIST_LABEL = Component.translatable("pack.iris.list.label").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);

	private final ShaderPackScreen screen;
	private final TopButtonRowEntry topButtonRow;
	private final WatchService watcher;
	private final WatchKey key;
	private boolean keyValid;
	private ShaderPackEntry applied = null;

	public ShaderPackSelectionList(ShaderPackScreen screen, Minecraft client, int width, int height, int top, int bottom, int left, int right) {
		super(client, width, bottom, top + 4, bottom, left, right, 20);
		WatchKey key1;
		WatchService watcher1;

		this.screen = screen;
		this.topButtonRow = new TopButtonRowEntry(this, Iris.getIrisConfig().areShadersEnabled());
		try {
			watcher1 = FileSystems.getDefault().newWatchService();
			key1 = Iris.getShaderpacksDirectory().register(watcher1,
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_MODIFY,
				StandardWatchEventKinds.ENTRY_DELETE);
			keyValid = true;
		} catch (IOException e) {
			CeleritasShaders.logger().error("Couldn't register file watcher!", e);
			watcher1 = null;
			key1 = null;
			keyValid = false;
		}

		this.key = key1;
		this.watcher = watcher1;
		refresh();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_UP) {
			if (getFocused() == children().getFirst()) return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor pAbstractSelectionList0, int pInt1, int pInt2, float pFloat3) {
		if (keyValid) {
			for (WatchEvent<?> event : key.pollEvents()) {
				if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

				refresh();
				break;
			}

			keyValid = key.reset();
		}

		super.extractWidgetRenderState(pAbstractSelectionList0, pInt1, pInt2, pFloat3);
	}

	public void close() throws IOException {
		if (key != null) {
			key.cancel();
		}

		if (watcher != null) {
			watcher.close();
		}
	}

	@Override
	public int getRowWidth() {
		return Math.min(308, width - 50);
	}

	public void refresh() {
		this.clearEntries();

		List<String> names;

		try {
			names = Iris.getShaderpacksDirectoryManager().enumerate();
		} catch (Throwable e) {
			CeleritasShaders.logger().error("Error reading files while constructing selection UI", e);

			// Not translating this since it's going to be seen very rarely,
			// We're just trying to get more information on a seemingly untraceable bug:
			// - https://github.com/IrisShaders/Iris/issues/785
			this.addLabelEntries(
				Component.empty(),
				Component.literal("There was an error reading your shaderpacks directory")
					.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.empty(),
				Component.literal("Check your logs for more information."),
				Component.literal("Please file an issue report including a log file."),
				Component.literal("If you are able to identify the file causing this, " +
					"please include it in your report as well."),
				Component.literal("Note that this might be an issue with folder " +
					"permissions; ensure those are correct first.")
			);

			return;
		}

		this.addEntry(topButtonRow);

		// Only allow the enable/disable shaders button if the user has
		// added a shader pack. Otherwise, the button will be disabled.
		topButtonRow.allowEnableShadersButton = !names.isEmpty();

		int index = 0;

		for (String name : names) {
			index++;
			addPackEntry(index, name);
		}

		this.addLabelEntries(PACK_LIST_LABEL);
	}

	public void addPackEntry(int index, String name) {
		ShaderPackEntry entry = new ShaderPackEntry(index, this, name);

		Iris.getIrisConfig().getShaderPackName().ifPresent(currentPackName -> {
			if (name.equals(currentPackName)) {
				setSelected(entry);
				setFocused(entry);
				centerScrollOn(entry);
				setApplied(entry);
			}
		});

		this.addEntry(entry);
	}

	public void addLabelEntries(Component... lines) {
		for (Component text : lines) {
			this.addEntry(new LabelEntry(text));
		}
	}

	public void select(String name) {
		for (int i = 0; i < getItemCount(); i++) {
			BaseEntry entry = children().get(i);

			if (entry instanceof ShaderPackEntry && ((ShaderPackEntry) entry).packName.equals(name)) {
				setSelected(entry);

				return;
			}
		}
	}

	public ShaderPackEntry getApplied() {
		return this.applied;
	}

	public void setApplied(ShaderPackEntry entry) {
		this.applied = entry;
	}

	public TopButtonRowEntry getTopButtonRow() {
		return topButtonRow;
	}

	public static abstract class BaseEntry extends AbstractSelectionList.Entry<BaseEntry> {
		protected BaseEntry() {
		}
	}

	public static class LabelEntry extends BaseEntry {
		private final Component label;

		public LabelEntry(Component label) {
			this.label = label;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			int x = getContentX();
			int y = getContentY();
			int entryWidth = getWidth();
			int entryHeight = getContentHeight();

			guiGraphics.centeredText(Minecraft.getInstance().font, label, (x + entryWidth / 2) - 2, y + (entryHeight - 11) / 2, 0xFFC2C2C2);
		}
	}

	public static class TopButtonRowEntry extends BaseEntry {
		private static final Component NONE_PRESENT_LABEL = Component.translatable("options.iris.shaders.nonePresent").withStyle(ChatFormatting.GRAY);
		private static final Component SHADERS_DISABLED_LABEL = Component.translatable("options.iris.shaders.disabled");
		private static final Component SHADERS_ENABLED_LABEL = Component.translatable("options.iris.shaders.enabled");

		private final ShaderPackSelectionList list;

		public boolean allowEnableShadersButton = true;
		public boolean shadersEnabled;

		public TopButtonRowEntry(ShaderPackSelectionList list, boolean shadersEnabled) {
			this.list = list;
			this.shadersEnabled = shadersEnabled;
		}

		public void setShadersEnabled(boolean shadersEnabled) {
			this.shadersEnabled = shadersEnabled;
			this.list.screen.refreshScreenSwitchButton();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			int x = getContentX();
			int y = getContentY();
			int entryWidth = getWidth();
			int entryHeight = getContentHeight();

			GuiUtil.drawButton(guiGraphics, x - 2, y - 2, entryWidth, entryHeight + 2, hovered, !allowEnableShadersButton);
			guiGraphics.centeredText(Minecraft.getInstance().font, getEnableDisableLabel(), (x + entryWidth / 2) - 2, y + (entryHeight - 11) / 2, 0xFFFFFFFF);
		}

		private Component getEnableDisableLabel() {
			return this.allowEnableShadersButton ? this.shadersEnabled ? SHADERS_ENABLED_LABEL : SHADERS_DISABLED_LABEL : NONE_PRESENT_LABEL;
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			if (this.allowEnableShadersButton) {
				setShadersEnabled(!this.shadersEnabled);
				GuiUtil.playButtonClickSound();
				return true;
			}

			return false;
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (event.key() == GLFW.GLFW_KEY_ENTER) {
				if (this.allowEnableShadersButton) {
					setShadersEnabled(!this.shadersEnabled);
					GuiUtil.playButtonClickSound();
					return true;
				}
			}

			return false;
		}

		@Nullable
		@Override
		public ComponentPath nextFocusPath(FocusNavigationEvent pGuiEventListener0) {
			return (!isFocused()) ? ComponentPath.leaf(this) : null;
		}


		public boolean isFocused() {
			return this.list.getFocused() == this;
		}

		// Renders the label at an offset as to not look misaligned with the rest of the menu
		public static class EnableShadersButtonElement extends IrisElementRow.TextButtonElement {
			private int centerX;

			public EnableShadersButtonElement(Component text, Function<IrisElementRow.TextButtonElement, Boolean> onClick) {
				super(text, onClick);
			}

			@Override
			public void renderLabel(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
				int textX = this.centerX - (int) (this.font.width(this.text) * 0.5);
				int textY = y + (int) ((height - 8) * 0.5);

				guiGraphics.text(this.font, this.text, textX, textY, 0xFFFFFFFF);
			}
		}
	}

	public class ShaderPackEntry extends BaseEntry {
		private final String packName;
		private final ShaderPackSelectionList list;
		private final int index;

		public ShaderPackEntry(int index, ShaderPackSelectionList list, String packName) {
			this.packName = packName;
			this.list = list;
			this.index = index;
		}

		public boolean isApplied() {
			return list.getApplied() == this;
		}

		public boolean isSelected() {
			return list.getSelected() == this;
		}

		public String getPackName() {
			return packName;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
			int x = getContentX();
			int y = getContentY();
			int entryWidth = getWidth();
			int entryHeight = getContentHeight();

			Font font = Minecraft.getInstance().font;
			int color = 0xFFFFFFFF;
			String name = packName;

			if (hovered) {
				GuiUtil.drawButton(guiGraphics, x - 2, y - 2, entryWidth, entryHeight + 4, hovered, false);
			}

			boolean shadersEnabled = list.getTopButtonRow().shadersEnabled;

			if (font.width(Component.literal(name).withStyle(ChatFormatting.BOLD)) > this.list.getRowWidth() - 3) {
				name = font.plainSubstrByWidth(name, this.list.getRowWidth() - 8) + "...";
			}

			MutableComponent text = Component.literal(name);

			if (this.isMouseOver(mouseX, mouseY)) {
				text = text.withStyle(ChatFormatting.BOLD);
			}

			if (shadersEnabled && this.isApplied()) {
				color = 0xFFFFF263;
			}

			if (!shadersEnabled && !this.isMouseOver(mouseX, mouseY)) {
				color = 0xFFA2A2A2;
			}

			guiGraphics.centeredText(font, text, (x + entryWidth / 2) - 2, y + (entryHeight - 11) / 2, color);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			// Only do anything on left-click
			if (event.button() != 0) {
				return false;
			}

			return doThing();
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			// Only do anything on key-press
			if (event.key() != GLFW.GLFW_KEY_ENTER) {
				return false;
			}

			return doThing();
		}

		private boolean doThing() {
			boolean didAnything = false;

			// UX: If shaders are disabled, then clicking a shader in the list will also
			//     enable shaders on apply. Previously, it was not possible to select
			//     a pack when shaders were disabled, but this was a source of confusion
			//     - people did not realize that they needed to enable shaders before
			//     selecting a shader pack.
			if (!list.getTopButtonRow().shadersEnabled) {
				list.getTopButtonRow().setShadersEnabled(true);
				didAnything = true;
			}

			if (!this.isSelected()) {
				this.list.select(this.index);
				didAnything = true;
			}

			ShaderPackSelectionList.this.screen.setFocused(ShaderPackSelectionList.this.screen.getBottomRowOption());

			return didAnything;
		}

		@Nullable
		@Override
		public ComponentPath nextFocusPath(FocusNavigationEvent pGuiEventListener0) {
			return (!isFocused()) ? ComponentPath.leaf(this) : null;
		}


		public boolean isFocused() {
			return this.list.getFocused() == this;
		}
	}
}
