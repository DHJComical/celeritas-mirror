package org.embeddedt.embeddium.impl.gui.frame;

import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpact;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.api.options.control.Control;
import org.taumc.celeritas.api.options.control.ControlElement;
import org.embeddedt.embeddium.impl.gui.framework.DrawContext;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.theme.DefaultColors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

public class OptionPageFrame extends AbstractFrame {
    protected final OptionPage page;
    private long lastTime = 0;
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
                int visibleOptionCount = (int)group.getOptions().stream().filter(optionFilter::test).count();
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
                if(!optionFilter.test(option)) {
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

            if(needPadding) {
                // Add padding beneath each option group
                y += 4;
            }
        }

        super.buildFrame();
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        ControlElement<?> hoveredElement = this.isMouseOver(mouseX, mouseY) ? this.controlElements.stream()
                .filter(c -> c.isMouseOver(mouseX, mouseY))
                .findFirst().orElse(null) : null;
        super.render(drawContext, mouseX, mouseY, delta);
        if (hoveredElement != null && this.lastHoveredElement == hoveredElement) {
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
        if(mod == null) {
            return null;
        } else {
            return switch(mod) {
                case "minecraft" -> "embeddium";
                default -> mod;
            };
        }
    }

    private void renderOptionTooltip(DrawContext drawContext, ControlElement<?> element) {
        if (this.lastTime + 500 > System.currentTimeMillis()) return;

        Dim2i dim = element.getDimensions();

        int textPadding = 3;
        int boxPadding = 3;

        int boxWidth = dim.width();

        //Offset based on mouse position, width and height of content and width and height of the window
        int boxY = dim.getLimitY();
        int boxX = dim.x();

        Option<?> option = element.getOption();
        var tooltip = new ArrayList<>(drawContext.split(option.getTooltip(), boxWidth - (textPadding * 2)));

        OptionImpact impact = option.getImpact();

        if (impact != null) {
            var impactString = TextComponent.translatable("sodium.options.performance_impact_string", impact.getLocalizedName()).withStyle(TextFormattingStyle.GRAY);
            tooltip.add(impactString);
        }

        var id = option.getId();

        if (OptionIdentifier.isPresent(page.getId()) && OptionIdentifier.isPresent(id) && !Objects.equals(normalizeModForTooltip(page.getId().getModId()), normalizeModForTooltip(id.getModId()))) {
            var addedByModString = TextComponent.translatable("embeddium.options.added_by_mod_string", TextComponent.literal(id.getModId()).withStyle(TextFormattingStyle.WHITE)).withStyle(TextFormattingStyle.GRAY);
            tooltip.add(addedByModString);
        }

        int boxHeight = (tooltip.size() * 12) + boxPadding;
        int boxYLimit = boxY + boxHeight;
        int boxYCutoff = this.dim.getLimitY();

        // If the box is going to be cutoff on the Y-axis, move it back up the difference
        if (boxYLimit > boxYCutoff) {
            boxY -= boxHeight + dim.height();
        }

        if (boxY < 0) {
            boxY = dim.getLimitY();
        }

        drawContext.pushMatrix();

        drawContext.translate(0, 0, 90);

        drawContext.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);
        drawContext.drawBorder(boxX, boxY, boxX + boxWidth, boxY + boxHeight, DefaultColors.ELEMENT_ACTIVATED);

        for (int i = 0; i < tooltip.size(); i++) {
            drawContext.drawString(tooltip.get(i), boxX + textPadding, boxY + textPadding + (i * 12), 0xFFFFFFFF, true);
        }

        drawContext.popMatrix();
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
            Objects.requireNonNull(this.dim, "Dimension must be specified");
            Objects.requireNonNull(this.page, "Option Page must be specified");

            return new OptionPageFrame(this.dim, this.renderOutline, this.page, this.optionFilter);
        }
    }
}
