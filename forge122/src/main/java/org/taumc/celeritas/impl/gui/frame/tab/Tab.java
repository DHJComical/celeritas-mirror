package org.taumc.celeritas.impl.gui.frame.tab;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.impl.gui.frame.AbstractFrame;
import org.taumc.celeritas.impl.gui.frame.OptionPageFrame;
import org.taumc.celeritas.impl.gui.frame.ScrollableFrame;
import org.taumc.celeritas.impl.util.ComponentUtil;
import org.taumc.celeritas.impl.util.Dim2i;
import org.taumc.celeritas.impl.util.PlatformUtil;

public record Tab<T extends AbstractFrame>(OptionIdentifier<Void> id, ITextComponent title, Supplier<Boolean> onSelectFunction, Function<Dim2i, T> frameFunction) {
    static ITextComponent idComponent(String namespace) {
        return ComponentUtil.literal(PlatformUtil.getModName(namespace)).setStyle((new Style()).setBold(true));
    }

    public static Tab.Builder<?> createBuilder() {
        return new Tab.Builder<>();
    }

    public Function<Dim2i, T> getFrameFunction() {
        return this.frameFunction;
    }

    public static class Builder<T extends AbstractFrame> {
        private ITextComponent title;
        private OptionIdentifier<Void> id;
        private Function<Dim2i, T> frameFunction = (d) -> null;
        private Supplier<Boolean> onSelectFunction = () -> true;

        public Builder<T> setTitle(ITextComponent title) {
            this.title = title;
            return this;
        }

        public Builder<T> setFrameFunction(Function<Dim2i, T> frameFunction) {
            this.frameFunction = frameFunction;
            return this;
        }

        public Builder<T> setOnSelectFunction(Supplier<Boolean> onSelectFunction) {
            this.onSelectFunction = onSelectFunction;
            return this;
        }

        public Builder<T> setId(OptionIdentifier<Void> id) {
            this.id = id;
            return this;
        }

        public Tab<T> build() {
            return new Tab<T>(this.id, this.title, this.onSelectFunction, this.frameFunction);
        }

        public Tab<ScrollableFrame> from(OptionPage page, Predicate<Option<?>> optionFilter, AtomicReference<Integer> verticalScrollBarOffset) {
            Function<Dim2i, ScrollableFrame> frameFunction = (dim2i) -> ScrollableFrame.createBuilder().setDimension(dim2i).setFrame(OptionPageFrame.createBuilder().setDimension(new Dim2i(dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height())).setOptionPage(page).setOptionFilter(optionFilter).build()).setVerticalScrollBarOffset(verticalScrollBarOffset).build();
            return (new Builder<ScrollableFrame>()).setTitle(page.getName()).setId(page.getId()).setFrameFunction(frameFunction).build();
        }
    }
}
