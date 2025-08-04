package org.taumc.celeritas.impl.loader.common;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.taumc.celeritas.CeleritasVintage;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ModLogoUtil {
    private static final Set<String> erroredLogos = new HashSet<>();

    public static ResourceLocation registerLogo(String modId) {
        if (erroredLogos.contains(modId)) return null;

        ModContainer container = Loader.instance().getIndexedModList().get(modId);
        if (container == null) return null;

        String logoPath;
        if (modId.equals("celeritas")) {
            logoPath = "textures/gui/icon.png";
        } else {
            logoPath = "icon.png";
        }

        ResourceLocation location = new ResourceLocation(modId, logoPath);

        try {
            Minecraft.getMinecraft().getResourceManager().getResource(location);
            return location;
        } catch (IOException e) {
            erroredLogos.add(modId);
            CeleritasVintage.logger().error("Exception reading logo for " + modId, e);
            return null;
        }
    }
}
