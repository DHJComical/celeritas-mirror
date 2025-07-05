package org.embeddedt.embeddium.impl.modern.compat.flywheel;

//? if flywheel {

/*import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.embeddedt.embeddium.api.ChunkDataBuiltEvent;
import org.embeddedt.embeddium.impl.Celeritas;

@Mod.EventBusSubscriber(modid = Celeritas.MODID, value = Dist.CLIENT)
public class FlywheelCompat {
    private static final boolean flywheelLoaded = ModList.get().isLoaded("flywheel");

    @SubscribeEvent
    public static void onChunkDataBuilt(ChunkDataBuiltEvent event) {
        if(flywheelLoaded) {
            event.getDataBuilder().removeBlockEntitiesIf(InstancedRenderRegistry/^? if <=1.16.5 {^//^.getInstance()^//^?}^/::shouldSkipRender);
        }
    }
}

*///?}