package org.embeddedt.embeddium.impl.mixin.features.render.immediate;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@Mixin(RenderType.class)
public abstract class RenderTypeMixin {
    @Shadow
    public static RenderType entityCutoutNoCull(ResourceLocation location, boolean outline) {
        throw new AssertionError();
    }

    @Unique
    private static final ConcurrentHashMap<ResourceLocation, RenderType> CELERITAS$ENTITY_CUTOUT_NO_CULL_WITH_OUTLINE = new ConcurrentHashMap<>();

    /**
     * @author embeddedt
     * @reason avoid allocation-intensive bifunction variant of Util.memoize
     */
    @Overwrite
    public static RenderType entityCutoutNoCull(ResourceLocation location) {
        var type = CELERITAS$ENTITY_CUTOUT_NO_CULL_WITH_OUTLINE.get(location);

        if (type != null) {
            return type;
        }

        type = entityCutoutNoCull(location, true);

        CELERITAS$ENTITY_CUTOUT_NO_CULL_WITH_OUTLINE.put(location, type);

        return type;
    }
}
