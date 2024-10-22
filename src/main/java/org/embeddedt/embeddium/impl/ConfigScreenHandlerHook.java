package org.embeddedt.embeddium.impl;

import org.embeddedt.embeddium.impl.gui.SodiumOptionsGUI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import org.embeddedt.embeddium.api.EmbeddiumConstants;

@Mod.EventBusSubscriber(modid = EmbeddiumConstants.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigScreenHandlerHook {
    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onModConstruct(FMLConstructModEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new SodiumOptionsGUI(screen)));
    }
}
