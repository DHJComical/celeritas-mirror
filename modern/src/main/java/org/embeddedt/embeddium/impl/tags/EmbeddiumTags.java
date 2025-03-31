package org.embeddedt.embeddium.impl.tags;

//? if >=1.18 {
import org.embeddedt.embeddium.impl.Celeritas;
//? if >=1.20 {
import net.minecraft.core.registries.Registries;
//?} else
/*import net.minecraft.core.Registry;*/
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class EmbeddiumTags {
    public static final TagKey<Fluid> RENDERS_WITH_VANILLA = TagKey.create(
            //? if <1.20 {
            /*Registry.FLUID_REGISTRY,
            *///?} else
            Registries.FLUID,
            ResourceLocationUtil.make(Celeritas.MODID, "is_vanilla_rendered_fluid"));
}
//?}