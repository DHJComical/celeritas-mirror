package net.irisshaders.iris.mixin;

import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.compat.mc.MCDumpable;
import org.embeddedt.embeddium.compat.mc.MCResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.nio.file.Path;

@Mixin(Dumpable.class)
public interface MixinDumpable extends MCDumpable {
    @Shadow
    void dumpContents(ResourceLocation resourceLocation, Path path) throws IOException;

    @Override
    default void dumpContents(MCResourceLocation resourceLocation, Path path) throws IOException {
        this.dumpContents((ResourceLocation) (Object)resourceLocation, path);
    }
}
