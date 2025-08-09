package org.taumc.celeritas.impl.compat.modernui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.taumc.celeritas.CeleritasVintage;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * Ugly hack to get around Modern UI overwriting calculateScaleFactor and not conforming to vanilla standards
 * by returning the max size when scale = 0.
 */
public class MuiGuiScaleHook {
    private static final Method calcGuiScalesMethod;

    static {
        calcGuiScalesMethod = Stream.of(
                "icyllis.modernui.forge.MForgeCompat",
                "icyllis.modernui.forge.MuiForgeApi",
                "icyllis.modernui.mc.forge.MuiForgeApi",
                "icyllis.modernui.mc.MuiModApi"
        ).flatMap(clzName -> {
            try {
                return Stream.of(Class.forName(clzName));
            } catch (Throwable e) {
                return Stream.of();
            }
        }).flatMap(clz -> {
            try {
                Method m = clz.getDeclaredMethod("calcGuiScales");
                m.setAccessible(true);
                return Stream.of(m);
            } catch (Throwable e) {
                return Stream.of();
            }
        }).findFirst().orElse(null);
        if (calcGuiScalesMethod != null)
            CeleritasVintage.logger().info("Found ModernUI GUI scale hook");
    }

    public static int getMaxGuiScale() {
        if (calcGuiScalesMethod != null) {
            try {
                return (int) calcGuiScalesMethod.invoke(null) & 0xf;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        boolean forceUnicode = Minecraft.getMinecraft().gameSettings.forceUnicodeFont;
        return calculateScale(0, forceUnicode);
    }

    public static int calculateScale(int guiScale, boolean forceUnicode) {
        int i;
        for (i = 1; i != guiScale && i < Minecraft.getMinecraft().getFramebuffer().framebufferWidth && i < Minecraft.getMinecraft().getFramebuffer().framebufferHeight && Minecraft.getMinecraft().getFramebuffer().framebufferWidth / (i + 1) >= 320 && Minecraft.getMinecraft().getFramebuffer().framebufferHeight / (i + 1) >= 240; ++i) {
        }

        if (forceUnicode && i % 2 != 0) {
            ++i;
        }

        return i;
    }

}
