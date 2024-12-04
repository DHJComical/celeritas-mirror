package org.embeddedt.embeddium.impl.mixin.features.model;

//? if forge && >=1.19
import net.minecraftforge.client.ChunkRenderTypeSet;
//? if neoforge
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;*/

//? if forgelike && >=1.19 {
import net.minecraft.client.resources.model.SimpleBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleBakedModel.class)
public interface SimpleBakedModelAccessor {
    @Accessor(remap = false)
    ChunkRenderTypeSet getBlockRenderTypes();
}
//?}