package net.irisshaders.iris.mixin.texture.pbr;

import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.BiConsumer;

@Mixin(DirectoryLister.class)
public class MixinDirectoryLister {

    @ModifyArg(method = "run", at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private BiConsumer<? super ResourceLocation, ? super Resource> alsoReadPbr(BiConsumer<? super ResourceLocation, ? super Resource> action, @Local(ordinal = 0, argsOnly = true) ResourceManager manager) {
        return (location, resource) -> {
            String basePath = PBRType.removeSuffix(location.getPath());
            if (basePath != null) {
                ResourceLocation baseLocation = location.withPath(basePath);
                if (manager.getResource(baseLocation).isPresent()) {
                    return;
                }
            }
            action.accept(location, resource);
        };
    }
}
