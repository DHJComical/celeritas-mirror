package org.taumc.celeritas.impl.gui.options.storage;

import java.io.IOException;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.api.options.structure.OptionStorage;
import org.taumc.celeritas.impl.gui.SodiumGameOptions;

public class SodiumOptionsStorage implements OptionStorage<SodiumGameOptions> {
    private final SodiumGameOptions options;

    public SodiumOptionsStorage() {
        this.options = CeleritasVintage.options();
    }

    @Override
    public SodiumGameOptions getData() {
        return this.options;
    }

    @Override
    public void save() {
        try {
            this.options.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't save configuration changes", e);
        }

        CeleritasVintage.logger().info("Flushed changes to Embeddium configuration");
    }
}
