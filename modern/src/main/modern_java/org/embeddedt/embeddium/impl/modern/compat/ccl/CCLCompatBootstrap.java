package org.embeddedt.embeddium.impl.modern.compat.ccl;

//? if codechickenlib {

import org.embeddedt.embeddium.impl.Celeritas;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//?}
//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
*///?}

//? if forge
@Mod.EventBusSubscriber(modid = Celeritas.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
//? if neoforge
/*@EventBusSubscriber(modid = Celeritas.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)*/
public class CCLCompatBootstrap {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("codechickenlib")) {
            CCLCompat.onClientSetup(event);
        }
    }
}

//?}