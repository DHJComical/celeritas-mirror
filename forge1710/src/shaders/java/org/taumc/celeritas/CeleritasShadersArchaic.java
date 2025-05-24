package org.taumc.celeritas;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.common.util.NativeBuffer;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Objects;

@Mod(modid = CeleritasShadersArchaic.MODID, useMetadata = true)
public class CeleritasShadersArchaic {
    public static final String MODID = "celeritas_shaders";
    private static final Logger LOGGER = LogManager.getLogger("Celeritas-Shaders");
    private static final String MODNAME = "Celeritas Shaders";
    private static final BufferPoolMXBean iris$directPool;
    private static final List<BufferPoolMXBean> iris$pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);

    static {
        BufferPoolMXBean found = null;

        for (BufferPoolMXBean pool : iris$pools) {
            if (pool.getName().equals("direct")) {
                found = pool;
                break;
            }
        }

        iris$directPool = Objects.requireNonNull(found);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onF3Text(RenderGameOverlayEvent.Text event) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo) {
            event.right.add(Math.min(event.right.size(), 2), "Direct Buffers: +" + iris$humanReadableByteCountBin(iris$directPool.getMemoryUsed()));

            event.right.add("");

            if (IrisCommon.getIrisConfig().areShadersEnabled()) {
                event.right.add("[" + MODNAME + "] Shaderpack: " + IrisCommon.getCurrentPackName() + (IrisCommon.isFallback() ? " (fallback)" : ""));
                IrisCommon.getCurrentPack().ifPresent(pack -> {
                    event.right.add(pack.getProfileInfo());
                });
                event.right.add("[" + MODNAME + "] Color space: " + IrisVideoSettings.colorSpace.name());
            }

            IrisCommon.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.addDebugText(event.left));

        }
    }

    private static String iris$humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.3f %ciB", value / 1024.0, ci.current());
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
