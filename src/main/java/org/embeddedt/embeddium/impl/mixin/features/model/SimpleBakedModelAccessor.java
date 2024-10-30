package org.embeddedt.embeddium.impl.mixin.features.model;

//? if forge
import net.minecraftforge.client.ChunkRenderTypeSet;
//? if neoforge
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;*/

//? if forgelike {
import net.minecraft.client.resources.model.SimpleBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleBakedModel.class)
public interface SimpleBakedModelAccessor {
    @Accessor(remap = false)
    ChunkRenderTypeSet getBlockRenderTypes();
}
//?}