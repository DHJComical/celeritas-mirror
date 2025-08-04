package org.taumc.celeritas.api.options.structure;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.util.ResourceLocation;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.api.OptionGroupConstructionEvent;
import org.taumc.celeritas.api.options.OptionIdentifier;

public class OptionGroup {
    public static final OptionIdentifier<Void> DEFAULT_ID = OptionIdentifier.create("celeritas", "empty");
    private final ImmutableList<Option<?>> options;
    public final OptionIdentifier<Void> id;

    private OptionGroup(OptionIdentifier<Void> id, ImmutableList<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public OptionIdentifier<Void> getId() {
        return this.id;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public ImmutableList<Option<?>> getOptions() {
        return this.options;
    }

    public static class Builder {
        private final List<Option<?>> options = new ArrayList<>();
        private OptionIdentifier<Void> id;

        public Builder setId(ResourceLocation id) {
            this.id = OptionIdentifier.create(id);
            return this;
        }

        public Builder setId(OptionIdentifier<Void> id) {
            this.id = id;
            return this;
        }

        public Builder add(Option<?> option) {
            this.options.add(option);
            return this;
        }

        public Builder addConditionally(boolean shouldAdd, Supplier<Option<?>> option) {
            if (shouldAdd) {
                this.add(option.get());
            }

            return this;
        }

        public OptionGroup build() {
            if (this.options.isEmpty()) {
                CeleritasVintage.logger().warn("OptionGroup must contain at least one option. ignoring empty group...");
            }

            if (this.id == null) {
                this.id = OptionGroup.DEFAULT_ID;
            }

            OptionGroupConstructionEvent.BUS.post(new OptionGroupConstructionEvent(this.id, this.options));
            return new OptionGroup(this.id, ImmutableList.copyOf(this.options));
        }
    }
}
