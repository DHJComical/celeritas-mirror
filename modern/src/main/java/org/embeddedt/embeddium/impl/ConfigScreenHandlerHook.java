package org.embeddedt.embeddium.impl;

//? if forge && >=1.18 {
import net.minecraftforge.api.distmarker.Dist;
//? if <1.19 {
/*import net.minecraftforge.client.ConfigGuiHandler;
*///?} else
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.gui.EmbeddiumVideoOptionsScreen;

@Mod.EventBusSubscriber(modid = EmbeddiumConstants.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigScreenHandlerHook {
    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onModConstruct(FMLConstructModEvent event) {
        //? if >=1.19 {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new EmbeddiumVideoOptionsScreen(screen)));
        //?} else
        /*ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class, () -> new ConfigGuiHandler.ConfigGuiFactory((mc, screen) -> new EmbeddiumVideoOptionsScreen(screen)));*/
    }
}
//?}