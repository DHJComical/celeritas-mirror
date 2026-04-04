package org.embeddedt.embeddium.impl;

import net.minecraft.client.Minecraft;

//? if forge && >=1.18 {
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
//? if >=1.19
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.IExtensionPoint;
//?}

//? if forge {
import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.eventbus.api.IEventBus;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
//? if forge && >=1.18 && <1.20.2
import net.minecraftforge.network.NetworkConstants;
//?}

//? if fabric {
/*import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
*///?}

//? if neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
//? if >=1.21.11
/^import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;^/
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
*///?}

import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.util.MixinAuditUtil;
import org.embeddedt.embeddium.impl.util.PlatformUtil;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;
import org.embeddedt.embeddium.impl.util.sodium.FlawlessFrames;
import org.embeddedt.embeddium.impl.commands.DevCommands;
import org.embeddedt.embeddium.impl.gui.SodiumGameOptions;
//? if >=1.18 {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else {
/*import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
*///?}

import java.io.IOException;
import java.util.ArrayList;

//? if forgelike
@Mod(Celeritas.MODID)
public class Celeritas /*? if fabric {*/ /*implements ClientModInitializer *//*?}*/
{
    public static final String MODID = EmbeddiumConstants.MODID;
    public static String MODNAME = EmbeddiumConstants.MODNAME;

    //? if >=1.18 {
    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    //?} else
    /*private static final Logger LOGGER = LogManager.getLogger(MODNAME);*/
    private static SodiumGameOptions CONFIG = loadConfig();

    private static String MOD_VERSION;

    //? if forgelike {
    public Celeritas(/*? if neoforge {*/ /*IEventBus modEventBus *//*?}*/) {
        MOD_VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
        //? if forge && >=1.18 && <1.20.2
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        if (!EarlyLoaderServices.INSTANCE.getDistribution().isClient()) {
            return;
        }

        commonClientInit();

        if (Boolean.getBoolean("embeddium.auditAndExit")) {
            MixinAuditUtil.auditAndExit();
        }

        //? if forge {
        IEventBus mainEventBus = MinecraftForge.EVENT_BUS;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        //?} else {
        /*IEventBus mainEventBus = NeoForge.EVENT_BUS;
        *///?}

        modEventBus.addListener(this::onClientSetup);
        if(PlatformUtil.isDevelopmentEnvironment()) {
            //? if >=1.18
            mainEventBus.addListener((RegisterClientCommandsEvent event) -> DevCommands.register(event.getDispatcher()));
        }

        //? if >=1.21.11 {
        /*modEventBus.addListener((RegisterDebugEntriesEvent event) -> {
            var rendererInfo = ResourceLocationUtil.make(MODID, "renderer_info");
            event.register(rendererInfo, (displayer, level, clientChunk, serverChunk) -> {
                ArrayList<String> strings = new ArrayList<>();
                strings.add("%s%s Renderer (%s)".formatted(net.minecraft.ChatFormatting.AQUA, MODNAME, getVersion()));
                var renderer = CeleritasWorldRenderer.instanceNullable();
                if (renderer != null) {
                    strings.addAll(renderer.getDebugStrings());
                }
                displayer.addToGroup(rendererInfo, strings);
            });
            event.includeInProfile(rendererInfo,
                    net.minecraft.client.gui.components.debug.DebugScreenProfile.DEFAULT,
                    net.minecraft.client.gui.components.debug.DebugScreenEntryStatus.IN_OVERLAY);
        });
        *///?}

        // TODO remove
        //? if shaders {
        modEventBus.addListener((RegisterKeyMappingsEvent ev) -> {
            ev.register(net.irisshaders.iris.Iris.reloadKeybind);
            ev.register(net.irisshaders.iris.Iris.shaderpackScreenKeybind);
            ev.register(net.irisshaders.iris.Iris.toggleShadersKeybind);
            ev.register(net.irisshaders.iris.Iris.wireframeKeybind);
        });
        //?}
    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        FlawlessFrames.onClientInitialization();
    }
    
    //?} else if fabric {
    /*@Override
    public void onInitializeClient() {
        MOD_VERSION = FabricLoader.getInstance().getModContainer(MODID).orElseThrow().getMetadata().getVersion().toString();
        FlawlessFrames.onClientInitialization();

        if (Boolean.getBoolean("embeddium.auditAndExit")) {
            MixinAuditUtil.auditAndExit();
        }

        commonClientInit();
    }
    *///?}

    private static void commonClientInit() {
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> {
            //? if >=1.17 <1.21.11
            com.mojang.blaze3d.vertex.BufferUploader.reset();
            //? if <1.17
            /*com.mojang.blaze3d.vertex.VertexBuffer.unbind();*/
        };
    }

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
        return !Celeritas.options().performance.useCompactVertexFormat;
    }

    public static boolean canApplyTranslucencySorting() {
        return Celeritas.options().performance.useTranslucentFaceSorting && !ShaderModBridge.isNvidiumEnabled();
    }

    public static boolean areGraphicsFancy() {
        //? if <1.21.11 {
        return Minecraft.getInstance().options.graphicsMode/*? if >=1.19 {*/().get()/*?}*/ != net.minecraft.client.GraphicsStatus.FAST;
        //?} else
        /*return Minecraft.getInstance().options.cutoutLeaves().get();*/
    }
}