package org.taumc.celeritas.impl.gui.frame;

import com.google.common.collect.UnmodifiableIterator;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.Control;
import org.taumc.celeritas.api.options.control.ControlElement;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpact;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.impl.gui.compat.GuiGraphics;
import org.taumc.celeritas.impl.gui.theme.DefaultColors;
import org.taumc.celeritas.impl.util.ComponentUtil;
import org.taumc.celeritas.impl.util.Dim2i;
import org.taumc.celeritas.impl.util.PlatformUtil;

public class OptionPageFrame extends AbstractFrame {
    protected final OptionPage page;
    private long lastTime = 0L;
    private ControlElement<?> lastHoveredElement = null;
    protected final Predicate<Option<?>> optionFilter;

    public OptionPageFrame(Dim2i dim, boolean renderOutline, OptionPage page, Predicate<Option<?>> optionFilter) {
        super(dim, renderOutline);
        this.page = page;
        this.optionFilter = optionFilter;
        this.setupFrame();
        this.buildFrame();
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public void setupFrame() {
        this.children.clear();
        this.drawable.clear();
        this.controlElements.clear();

        int y = 0;
        if (!this.page.getGroups().isEmpty()) {
            OptionGroup lastGroup = this.page.getGroups().get(this.page.getGroups().size() - 1);

            for (OptionGroup group : this.page.getGroups()) {
                int visibleOptionCount = (int) group.getOptions().stream().filter(optionFilter::test).count();
                y += visibleOptionCount * 18;
                if (visibleOptionCount > 0 && group != lastGroup) {
                    y += 4;
                }
            }
        }

        this.dim = this.dim.withHeight(y);
    }

    @Override
    public void buildFrame() {
        if (this.page == null) return;

        this.children.clear();
        this.drawable.clear();
        this.controlElements.clear();

        int y = 0;
        for (OptionGroup group : this.page.getGroups()) {
            boolean needPadding = false;
            // Add each option's control element
            for (Option<?> option : group.getOptions()) {
                if (!optionFilter.test(option)) {
                    continue;
                }
                Control<?> control = option.getControl();
                Dim2i dim = new Dim2i(0, y, this.dim.width(), 18).withParentOffset(this.dim);
                ControlElement<?> element = control.createElement(dim);
                this.children.add(element);

                // Move down to the next option
                y += 18;
                needPadding = true;
            }

            if (needPadding) {
                // Add padding beneath each option group
                y += 4;
            }
        }

        super.buildFrame();
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        ControlElement<?> hoveredElement = this.controlElements.stream()
                .filter(ControlElement::isHovered)
                .findFirst()
                .orElse(this.controlElements.stream()
                        .filter(ControlElement::isFocused)
                        .findFirst()
                        .orElse(null));
        super.render(drawContext, mouseX, mouseY, delta);
        if (hoveredElement != null && this.lastHoveredElement == hoveredElement &&
                ((this.dim.containsCursor(mouseX, mouseY) && hoveredElement.isHovered() && hoveredElement.isMouseOver(mouseX, mouseY))
                        || hoveredElement.isFocused())) {
            if (this.lastTime == 0) {
                this.lastTime = System.currentTimeMillis();
            }
            this.renderOptionTooltip(drawContext, hoveredElement);
        } else {
            this.lastTime = 0;
            this.lastHoveredElement = hoveredElement;
        }
    }

    private static String normalizeModForTooltip(@Nullable String mod) {
        if (mod == null) {
            return null;
        } else {
            return switch (mod) {
                case "minecraft" -> "celeritas";
                default -> mod;
            };
        }
    }

    private void renderOptionTooltip(GuiGraphics drawContext, ControlElement<?> element) {
        if (this.lastTime + 500 > System.currentTimeMillis()) return;

        Dim2i dim = element.getDimensions();

        int textPadding = 3;
        int boxPadding = 3;

        int boxWidth = dim.width();

        int boxY = dim.getLimitY();
        int boxX = dim.x();

        Option<?> option = element.getOption();
        ArrayList<String> tooltip = new ArrayList<>(split(option.getTooltip().getFormattedText(), boxWidth - textPadding * 2));
        OptionImpact impact = option.getImpact();
        if (impact != null) {
            ITextComponent impactString = ComponentUtil.translatable("sodium.options.performance_impact_string", impact.getLocalizedName()).setStyle((new Style()).setColor(TextFormatting.GRAY));
            tooltip.add(impactString.getFormattedText());
        }

        OptionIdentifier<?> id = option.getId();
        if (OptionIdentifier.isPresent(this.page.getId()) && OptionIdentifier.isPresent(id) && !Objects.equals(normalizeModForTooltip(this.page.getId().getModId()), normalizeModForTooltip(id.getModId()))) {
            ITextComponent addedByModString = ComponentUtil.translatable("celeritas.options.added_by_mod_string", ComponentUtil.literal(PlatformUtil.getModName(id.getModId())).setStyle((new Style()).setColor(TextFormatting.WHITE).setColor(TextFormatting.GRAY)));
            tooltip.add(addedByModString.getFormattedText());
        }

        int boxHeight = tooltip.size() * 12 + boxPadding;
        int boxYLimit = boxY + boxHeight;
        int boxYCutoff = this.dim.getLimitY();
        if (boxYLimit > boxYCutoff) {
            boxY -= boxHeight + dim.height();
        }

        if (boxY < 0) {
            boxY = dim.getLimitY();
        }

        GL11.glPushMatrix();
        GL11.glTranslated(0.0F, 0.0F, 90.0F);
        this.drawRect(drawContext, boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);
        this.drawBorder(drawContext, boxX, boxY, boxX + boxWidth, boxY + boxHeight, DefaultColors.ELEMENT_ACTIVATED);

        for (int i = 0; i < tooltip.size(); ++i) {
            drawContext.drawString(Minecraft.getMinecraft().fontRenderer, tooltip.get(i), boxX + textPadding, boxY + textPadding + i * 12, -1, true);
        }

        GL11.glPopMatrix();

    }

    public static class Builder {
        private Dim2i dim;
        private boolean renderOutline;
        private OptionPage page;
        private Predicate<Option<?>> optionFilter = o -> true;

        public Builder setDimension(Dim2i dim) {
            this.dim = dim;
            return this;
        }

        public Builder shouldRenderOutline(boolean renderOutline) {
            this.renderOutline = renderOutline;
            return this;
        }

        public Builder setOptionPage(OptionPage page) {
            this.page = page;
            return this;
        }

        public Builder setOptionFilter(Predicate<Option<?>> optionFilter) {
            this.optionFilter = optionFilter;
            return this;
        }

        public OptionPageFrame build() {
            Validate.notNull(this.dim, "Dimension must be specified");
            Validate.notNull(this.page, "Option Page must be specified");
            return new OptionPageFrame(this.dim, this.renderOutline, this.page, this.optionFilter);
        }
    }
}
