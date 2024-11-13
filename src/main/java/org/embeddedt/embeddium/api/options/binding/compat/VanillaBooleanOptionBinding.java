package org.embeddedt.embeddium.api.options.binding.compat;

import org.embeddedt.embeddium.api.options.binding.OptionBinding;
import net.minecraft.client.Options;

//? if >=1.19 {
import net.minecraft.client.OptionInstance;

public class VanillaBooleanOptionBinding implements OptionBinding<Options, Boolean> {
    private final OptionInstance<Boolean> option;

    public VanillaBooleanOptionBinding(OptionInstance<Boolean> option) {
        this.option = option;
    }

    @Override
    public void setValue(Options storage, Boolean value) {
        this.option.set(value);
    }

    @Override
    public Boolean getValue(Options storage) {
        return this.option.get();
    }
}
//?} else if >=1.17 {
/*import net.minecraft.client.CycleOption;

public class VanillaBooleanOptionBinding implements OptionBinding<Options, Boolean> {
    private final CycleOption<Boolean> option;

    public VanillaBooleanOptionBinding(CycleOption<Boolean> option) {
        this.option = option;
    }

    @Override
    public void setValue(Options storage, Boolean value) {
        this.option.setter.accept(storage, this.option, value);
    }

    @Override
    public Boolean getValue(Options storage) {
        return this.option.getter.apply(storage);
    }
}
*///?} else {
/*import net.minecraft.client.BooleanOption;

public class VanillaBooleanOptionBinding implements OptionBinding<Options, Boolean> {
    private final BooleanOption option;

    public VanillaBooleanOptionBinding(BooleanOption option) {
        this.option = option;
    }

    @Override
    public void setValue(Options storage, Boolean value) {
        this.option.set(storage, value.toString());
    }

    @Override
    public Boolean getValue(Options storage) {
        return this.option.get(storage);
    }
}
*///?}