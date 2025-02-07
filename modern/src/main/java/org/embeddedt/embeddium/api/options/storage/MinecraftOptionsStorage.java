package org.embeddedt.embeddium.api.options.storage;

import org.embeddedt.embeddium.api.options.structure.OptionFlag;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;
import org.embeddedt.embeddium.impl.Celeritas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.util.Set;

public class MinecraftOptionsStorage implements OptionStorage<Options> {
    private final Minecraft client;

    public MinecraftOptionsStorage() {
        this.client = Minecraft.getInstance();
    }

    @Override
    public Options getData() {
        return this.client.options;
    }

    @Override
    public void save(Set<OptionFlag> flags) {
        this.getData().save();

        Celeritas.logger().info("Flushed changes to Minecraft configuration");
    }

    public static int getMipmapLevels() {
        //? if >=1.19 {
        return Minecraft.getInstance().options.mipmapLevels().get();
        //?} else
        /*return Minecraft.getInstance().options.mipmapLevels;*/
    }
}
