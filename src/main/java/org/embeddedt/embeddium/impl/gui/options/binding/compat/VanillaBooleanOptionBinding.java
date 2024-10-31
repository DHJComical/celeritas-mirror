package org.embeddedt.embeddium.impl.gui.options.binding.compat;

import org.embeddedt.embeddium.impl.gui.options.binding.OptionBinding;
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
//?} else {
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
*///?}