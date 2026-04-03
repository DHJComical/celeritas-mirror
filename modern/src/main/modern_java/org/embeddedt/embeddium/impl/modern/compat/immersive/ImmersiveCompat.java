package org.embeddedt.embeddium.impl.modern.compat.immersive;

//? if >=1.18.2 && immersiveengineering {

import org.embeddedt.embeddium.impl.Celeritas;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.embeddedt.embeddium.api.ChunkMeshEvent;

@Mod.EventBusSubscriber(modid = Celeritas.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ImmersiveCompat {
    private static final boolean immersiveLoaded = FMLLoader.getLoadingModList().getModFileById("immersiveengineering") != null;
    private static boolean hasRegisteredMeshAppender;

    @SubscribeEvent
    public static void onResourceReload(RegisterClientReloadListenersEvent event) {
        if(!immersiveLoaded)
            return;

        event.registerReloadListener(new ImmersiveConnectionRenderer());
        if(!hasRegisteredMeshAppender) {
            hasRegisteredMeshAppender = true;
            ChunkMeshEvent.BUS.addListener(ImmersiveConnectionRenderer::meshAppendEvent);
        }
    }
}

//?}