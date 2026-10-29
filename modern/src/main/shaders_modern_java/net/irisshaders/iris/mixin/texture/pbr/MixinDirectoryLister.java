package net.irisshaders.iris.mixin.texture.pbr;

import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(DirectoryLister.class)
public class MixinDirectoryLister {
    @ModifyExpressionValue(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"))
    private Map<ResourceLocation, Resource> iris$hidePbrOverrides(Map<ResourceLocation, Resource> matches) {
        Predicate<ResourceLocation> siblingExistencePredicate = matches.keySet()::contains;
        return Maps.filterKeys(matches, location -> {
            String basePath = PBRType.removeSuffix(location.getPath());
            if (basePath == null) {
                // Doesn't end in a recognized PBR suffix at all.
                return true;
            }
            if (PBRType.hasDirectionalSiblings(location, siblingExistencePredicate)) {
                // Looks like a cardinal-direction texture set (e.g. "_n"/"_s"/"_e"/"_w" for block
                // faces) rather than an actual PBR map. Keep it as its own sprite.
                return true;
            }
            // Only treat it as a PBR override (and hide it as its own sprite) if the base texture it
            // would be overriding actually exists.
            return !siblingExistencePredicate.test(location.withPath(basePath));
        });
    }
}
