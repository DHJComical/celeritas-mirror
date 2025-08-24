package org.taumc.celeritas.api.options.structure;

import org.taumc.celeritas.api.OptionGroupConstructionEvent;
import org.taumc.celeritas.api.options.OptionIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OptionGroup {
    public static final OptionIdentifier<Void> DEFAULT_ID = OptionIdentifier.create("celeritas", "empty");

    private final List<Option<?>> options;

    public final OptionIdentifier<Void> id;
    private OptionGroup(OptionIdentifier<Void> id, List<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public OptionIdentifier<Void> getId() {
        return id;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public List<Option<?>> getOptions() {
        return this.options;
    }

    public static class Builder {
        private final List<Option<?>> options = new ArrayList<>();

        private OptionIdentifier<Void> id;

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
                add(option.get());
            }

            return this;
        }

        public OptionGroup build() {
            if (this.id == null) {
                this.id = OptionGroup.DEFAULT_ID;
                // FIXME Actually enforce IDs on groups
                //Celeritas.logger().warn("Id must be specified in OptionGroup which contains {}, this might throw a exception on a next release", this.options.get(0).getName().getString());
            }

            OptionGroupConstructionEvent.BUS.post(new OptionGroupConstructionEvent(this.id, this.options));

            return new OptionGroup(this.id, List.copyOf(this.options));
        }
    }
}
