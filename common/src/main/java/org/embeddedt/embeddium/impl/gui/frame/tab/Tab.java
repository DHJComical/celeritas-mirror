package org.embeddedt.embeddium.impl.gui.frame.tab;

import lombok.Builder;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.embeddedt.embeddium.impl.gui.framework.TextFormattingStyle;
import org.embeddedt.embeddium.impl.util.Dim2i;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.embeddedt.embeddium.impl.gui.frame.AbstractFrame;
import org.embeddedt.embeddium.impl.gui.frame.OptionPageFrame;
import org.embeddedt.embeddium.impl.gui.frame.ScrollableFrame;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Builder(builderClassName = "Builder", setterPrefix = "set")
public record Tab<T extends AbstractFrame>(OptionIdentifier<Void> id, TextComponent title, Supplier<Boolean> onSelectFunction, Function<Dim2i, T> frameFunction) {
    public static Tab.Builder<?> createBuilder() {
        return new Tab.Builder<>();
    }

    public T createFrame(Dim2i dim) {
        return this.frameFunction != null ? this.frameFunction.apply(dim) : null;
    }

    public static Tab<ScrollableFrame> from(OptionPage page, Predicate<Option<?>> optionFilter, AtomicReference<Integer> verticalScrollBarOffset) {
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
        return Tab.<ScrollableFrame>builder()
                .setTitle(page.getName())
                .setId(page.getId())
                .setFrameFunction(frameFunction)
                .build();
    }
}