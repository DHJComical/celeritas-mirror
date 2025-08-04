package org.taumc.celeritas.api.options.storage;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.api.options.structure.OptionFlag;
import org.taumc.celeritas.api.options.structure.OptionStorage;

public class MinecraftOptionsStorage implements OptionStorage<GameSettings> {
    private final Minecraft client;

    public MinecraftOptionsStorage() {
        this.client = Minecraft.getMinecraft();
    }

    @Override
    public GameSettings getData() {
        return this.client.gameSettings;
    }

    @Override
    public void save(Set<OptionFlag> flags) {
        this.getData().saveOptions();

        CeleritasVintage.logger().info("Flushed changes to Minecraft configuration");
    }
}