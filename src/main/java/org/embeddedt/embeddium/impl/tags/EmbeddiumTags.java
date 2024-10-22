package org.embeddedt.embeddium.impl.tags;

import org.embeddedt.embeddium.impl.SodiumClientMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class EmbeddiumTags {
    public static final TagKey<Fluid> RENDERS_WITH_VANILLA = TagKey.create(Registries.FLUID, new ResourceLocation(SodiumClientMod.MODID, "is_vanilla_rendered_fluid"));
}
