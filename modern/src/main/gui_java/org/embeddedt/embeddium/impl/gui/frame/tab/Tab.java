package org.embeddedt.embeddium.impl.gui.frame.tab;

import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.impl.util.ComponentUtil;
import org.embeddedt.embeddium.impl.util.Dim2i;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.frame.AbstractFrame;
import org.embeddedt.embeddium.impl.gui.frame.OptionPageFrame;
import org.embeddedt.embeddium.impl.gui.frame.ScrollableFrame;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.embeddedt.embeddium.compat.mc.PlatformUtilService.PLATFORM_UTIL;


public record Tab<T extends AbstractFrame>(OptionIdentifier<Void> id, Component title, Supplier<Boolean> onSelectFunction, Function<Dim2i, T> frameFunction) {

    static Component idComponent(String namespace) {
        return ComponentUtil.literal(PLATFORM_UTIL.getModName(namespace))
                //? if <1.16 {
                /*.withStyle(s -> s.setBold(true))
                *///?} else
                .withStyle(s -> s.withBold(true))
                ;
    }

    public static Tab.Builder<?> createBuilder() {
        return new Tab.Builder<>();
    }

    public Function<Dim2i, T> getFrameFunction() {
        return this.frameFunction;
    }

    public static class Builder<T extends AbstractFrame> {
        private Component title;
        private OptionIdentifier<Void> id;
        private Function<Dim2i, T> frameFunction = d -> null;
        private Supplier<Boolean> onSelectFunction = () -> true;

        public Builder<T> setTitle(Component title) {
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
            Function<Dim2i, ScrollableFrame> frameFunction = dim2i -> ScrollableFrame
                    .createBuilder()
                    .setDimension(dim2i)
                    .setFrame(OptionPageFrame
                            .createBuilder()
                            .setDimension(new Dim2i(dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height()))
                            .setOptionPage(page)
                            .setOptionFilter(optionFilter)
                            .build())
                    .setVerticalScrollBarOffset(verticalScrollBarOffset)
                    .build();
            return new Builder<ScrollableFrame>()
                    .setTitle(page.getName())
                    .setId(page.getId())
                    .setFrameFunction(frameFunction)
                    .build();
        }
    }
}