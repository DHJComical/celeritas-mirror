package org.embeddedt.embeddium.impl;

//? if forge {
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
//? if forge && <1.20.2
import net.minecraftforge.network.NetworkConstants;
//?}

//? if fabric {
/*import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
*///?}

import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.util.sodium.FlawlessFrames;
import org.embeddedt.embeddium.impl.commands.DevCommands;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

//? if forge
@Mod(SodiumClientMod.MODID)
public class SodiumClientMod /*? if fabric {*/ /*implements ClientModInitializer *//*?}*/
{
    public static final String MODID = EmbeddiumConstants.MODID;
    public static final String MODNAME = EmbeddiumConstants.MODNAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    private static SodiumGameOptions CONFIG = loadConfig();

    private static String MOD_VERSION;

    //? if forgelike {
    public SodiumClientMod() {
        MOD_VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
        //? if forge && <1.20.2
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        if (!FMLLoader.getDist().isClient()) {
            return;
        }

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        if(!FMLLoader.isProduction()) {
            MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> DevCommands.register(event.getDispatcher()));
        }
    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        FlawlessFrames.onClientInitialization();
    }
    
    //?} else if fabric {
    /*@Override
    public void onInitializeClient() {
        MOD_VERSION = FabricLoader.getInstance().getModContainer(MODID).orElseThrow().getMetadata().getVersion().toString();
        FlawlessFrames.onClientInitialization();
    }
    *///?}

    public static SodiumGameOptions options() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }

        return CONFIG;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not yet available");
        }

        return LOGGER;
    }

    private static SodiumGameOptions loadConfig() {
        try {
            return SodiumGameOptions.load();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file", e);
            LOGGER.error("Using default configuration file in read-only mode");

            var config = new SodiumGameOptions();
            config.setReadOnly();

            return config;
        }
    }

    public static void restoreDefaultOptions() {
        CONFIG = SodiumGameOptions.defaults();

        try {
            CONFIG.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file", e);
        }
    }

    public static String getVersion() {
        if (MOD_VERSION == null) {
            throw new NullPointerException("Mod version hasn't been populated yet");
        }

        return MOD_VERSION;
    }

    public static boolean canUseVanillaVertices() {
        return !SodiumClientMod.options().performance.useCompactVertexFormat && !ShaderModBridge.areShadersEnabled();
    }

    public static boolean canApplyTranslucencySorting() {
        return SodiumClientMod.options().performance.useTranslucentFaceSorting && !ShaderModBridge.isNvidiumEnabled();
    }
}