package org.embeddedt.embeddium.impl.render.frapi;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.embeddedt.embeddium.impl.SodiumClientMod;
//? if ffapi
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
//? if >=1.18 {
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
//?} else
/*import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;*/
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
//? if neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
*///?}
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.*;

/**
 * Provides a mechanism for retrieving sprites by location on the atlas.
 */
//? if forge
@Mod.EventBusSubscriber(modid = SodiumClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SpriteFinderCache {
    private static final Finder NULL_FINDER = (u, v) -> null;
    private static final MethodHandle SPRITE_FINDER_HANDLE;

    private static Finder blockAtlasSpriteFinder = NULL_FINDER;

    static {
        MethodHandle mh;
        try {
            mh = MethodHandles.lookup().findVirtual(Class.forName("net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder"), "find", MethodType.methodType(TextureAtlasSprite.class, float.class, float.class));
        } catch(Throwable e) {
            mh = null;
        }
        SPRITE_FINDER_HANDLE = mh;
    }

    public interface Finder {
        @Nullable
        TextureAtlasSprite findNearestSprite(float u, float v);
    }

    //? if forge && <1.18 {
    /*@SubscribeEvent
    public static void onReload(ParticleFactoryRegisterEvent event) {
    *///?} else if forgelike && >=1.18 {
    @SubscribeEvent
    public static void onReload(RegisterClientReloadListenersEvent event) {
    //?} else {
    /*public static void registerListener() {
    *///?}
        if(SPRITE_FINDER_HANDLE != null) {
            var listener = new SimplePreparableReloadListener<>() {
                @Override
                protected Object prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                    return null;
                }

                @Override
                protected void apply(Object pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                    var modelManager = Minecraft.getInstance().getModelManager();
                    Finder finder = NULL_FINDER;
                    //? if ffapi {
                    var fabricFinder = SpriteFinder.get(modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS));
                    try {
                        MethodType bootstrapType = MethodType.methodType(Finder.class, SpriteFinder.class);
                        MethodType invocationType = MethodType.methodType(TextureAtlasSprite.class, float.class, float.class);
                        // Adapt to our Finder interface
                        finder = (Finder)LambdaMetafactory.metafactory(MethodHandles.lookup(),
                                "findNearestSprite",
                                bootstrapType, invocationType,
                                SPRITE_FINDER_HANDLE, invocationType).getTarget().invokeExact((SpriteFinder)fabricFinder);
                    } catch(Throwable e) {
                        e.printStackTrace();
                    }
                    //?}
                    blockAtlasSpriteFinder = finder;
                }
            };
            //? if forgelike && >=1.18
            event.registerReloadListener(listener);
            //? if fabric || (forge && <1.18)
            /*((ReloadableResourceManager)Minecraft.getInstance().getResourceManager()).registerReloadListener(listener);*/
        }
    }

    public static Finder forBlockAtlas() {
        return blockAtlasSpriteFinder;
    }
}
