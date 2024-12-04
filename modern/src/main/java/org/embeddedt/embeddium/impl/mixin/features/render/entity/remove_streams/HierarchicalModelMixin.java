package org.embeddedt.embeddium.impl.mixin.features.render.entity.remove_streams;

//? if >=1.18 <1.21.2
import net.minecraft.client.model.HierarchicalModel;
//? if >=1.21.2
/*import net.minecraft.client.model.Model;*/
import net.minecraft.client.model.geom.ModelPart;
import org.embeddedt.embeddium.impl.render.entity.ModelPartExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

//? if >=1.18 <1.21.2
@Mixin(HierarchicalModel.class)
//? if >=1.21.2
/*@Mixin(Model.class)*/
public abstract class HierarchicalModelMixin {
    //? if >=1.18
    @Shadow
    public abstract ModelPart root();

    //? if >=1.19 {
    @Overwrite
    public Optional<ModelPart> getAnyDescendantWithName(String pName) {
        var extendedRoot = ModelPartExtended.of(this.root());
        if(pName.equals("root")) {
            return extendedRoot.embeddium$asOptional();
        } else {
            var part = extendedRoot.embeddium$getDescendantsByName().get(pName);
            return part != null ? ModelPartExtended.of(part).embeddium$asOptional() : Optional.empty();
        }
    }
    //?}
}
