package org.taumc.celeritas;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;
import org.taumc.celeritas.impl.command.TogglePassCommand;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

import java.lang.management.ManagementFactory;

@Mod(modid = CeleritasVintage.MODID, useMetadata = true)
public class CeleritasVintage {
    public static final String MODID = "celeritas";
    private static final Logger LOGGER = LogManager.getLogger("Celeritas");
    public static String VERSION;

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        LOGGER.info("Hello from Forge!");
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> {
            OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
        };
        VERSION = Loader.instance().getIndexedModList().get(MODID).getVersion();
        MinecraftForge.EVENT_BUS.register(this);

        if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            ClientCommandHandler.instance.registerCommand(new TogglePassCommand());
        }
    }

    @SubscribeEvent
    public void onF3Text(RenderGameOverlayEvent.Text event) {
        if (!Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            return;
        }

        var strings = event.getRight();
        strings.add("");
        strings.add("%s%s Renderer (%s)".formatted(ChatFormatting.GREEN, "Celeritas", VERSION));

        // Embeddium: Show a lot less with reduced debug info
        if (Minecraft.getMinecraft().isReducedDebug()) {
            return;
        }

        var renderer = CeleritasWorldRenderer.instanceNullable();

        if (renderer != null) {
            strings.addAll(renderer.getDebugStrings());
        }

        for (int i = 0; i < strings.size(); i++) {
            String str = strings.get(i);

            if (str.startsWith("Allocated:")) {
                strings.add(i + 1, getNativeMemoryString());

                break;
            }
        }
    }

    private static String getNativeMemoryString() {
        return "Off-Heap: +" + MathUtil.toMib(getNativeMemoryUsage()) + "MB";
    }

    private static long getNativeMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() + NativeBuffer.getTotalAllocated();
    }

    public static Logger logger() {
        return LOGGER;
    }
}
