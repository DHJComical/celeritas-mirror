package org.embeddedt.embeddium.impl.loader.common;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.resource.ResourcePackLoader;
import org.embeddedt.embeddium.impl.SodiumClientMod;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ModLogoUtil {
    private static final Set<String> erroredLogos = new HashSet<>();

    public static ResourceLocation registerLogo(String modId) {
        Optional<String> logoFile = erroredLogos.contains(modId) ? Optional.empty() : ModList.get().getModContainerById(modId).flatMap(c -> c.getModInfo().getLogoFile());
        ResourceLocation texture = null;
        if(logoFile.isPresent()) {
            final PathPackResources resourcePack = ResourcePackLoader.getPackFor(modId)
                    .orElse(ResourcePackLoader.getPackFor("forge").
                            orElseThrow(()->new RuntimeException("Can't find forge, WHAT!")));
            try {
                IoSupplier<InputStream> logoResource = resourcePack.getRootResource(logoFile.get());
                if (logoResource != null) {
                    NativeImage logo = NativeImage.read(logoResource.get());
                    if(logo.getWidth() != logo.getHeight()) {
                        logo.close();
                        throw new IOException("Logo " + logoFile.get() + " for " + modId + " is not square");
                    }
                    texture = new ResourceLocation(SodiumClientMod.MODID, "logo/" + modId);
                    Minecraft.getInstance().textureManager.register(texture, new DynamicTexture(logo));
                }
            } catch(IOException e) {
                erroredLogos.add(modId);
                SodiumClientMod.logger().error("Exception reading logo for " + modId, e);
            }
        }
        return texture;
    }
}
