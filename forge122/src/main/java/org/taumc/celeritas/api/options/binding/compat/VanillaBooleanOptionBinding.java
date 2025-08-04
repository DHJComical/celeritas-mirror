package org.taumc.celeritas.api.options.binding.compat;

import net.minecraft.client.settings.GameSettings;
import org.taumc.celeritas.api.options.binding.OptionBinding;

public class VanillaBooleanOptionBinding implements OptionBinding<GameSettings, Boolean> {
    private final GameSettings.Options option;

    public VanillaBooleanOptionBinding(GameSettings.Options option) {
        this.option = option;
    }

    @Override
    public void setValue(GameSettings settings, Boolean value) {
        settings.setOptionValue(this.option, value ? 1 : 0);
    }

    @Override
    public Boolean getValue(GameSettings settings) {
        return settings.getOptionOrdinalValue(this.option);
    }
}
