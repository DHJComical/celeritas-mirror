package org.embeddedt.embeddium.impl.loader.common;

import com.mojang.blaze3d.platform.NativeImage;
//? if fabric
/*import net.fabricmc.loader.api.FabricLoader;*/
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
//? if forge {
import net.minecraftforge.fml.ModList;
//?}
//? if neoforge
/*import net.neoforged.fml.ModList;*/
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ModLogoUtil {
    private static final Set<String> erroredLogos = new HashSet<>();

    //? if fabric {
    /*public static ResourceLocation registerLogo(String modId) {
        Optional<Path> logoPath = FabricLoader.getInstance().getModContainer(modId).flatMap(m -> m.getMetadata().getIconPath(32).flatMap(m::findPath));
        ResourceLocation texture = null;
        if (logoPath.isPresent()) {
            try {
                texture = handleIoSupplier(modId, Files.newInputStream(logoPath.get()));
            } catch(IOException e) {
                erroredLogos.add(modId);
                Celeritas.logger().error("Exception reading logo for " + modId, e);
            }
        }
        return texture;
    }
    *///?} else if (forge && >=1.18) || neoforge {
    public static ResourceLocation registerLogo(String modId) {
        Optional<String> logoFile = erroredLogos.contains(modId) ? Optional.empty() : ModList.get().getModContainerById(modId).flatMap(c -> c.getModInfo().getLogoFile());
        ResourceLocation texture = null;
        if(logoFile.isPresent()) {
            Path logoPath = ModList.get().getModFileById(modId).getFile().findResource(logoFile.get());
            try {
                if(logoPath != null) {
                    texture = handleIoSupplier(modId, Files.newInputStream(logoPath));
                }
            } catch(IOException e) {
                erroredLogos.add(modId);
                Celeritas.logger().error("Exception reading logo for " + modId, e);
            }
        }
        return texture;
    }
    //?} else {
    /*public static ResourceLocation registerLogo(String modId) {
        return null;
    }
    *///?}

    private static ResourceLocation handleIoSupplier(String modId, InputStream logoResource) throws IOException {
        if (logoResource != null) {
            NativeImage logo = NativeImage.read(logoResource);
            if(logo.getWidth() != logo.getHeight()) {
                logo.close();
                throw new IOException("Logo for " + modId + " is not square");
            }
            ResourceLocation texture = ResourceLocationUtil.make(Celeritas.MODID, "logo/" + modId);
            Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(
                    //? if >=1.21.5-alpha.25.7.a
                    /*() -> modId + " logo",*/
                    logo));
            return texture;
        } else {
            return null;
        }
    }
}
