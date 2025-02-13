//? if >=1.15 {
package org.embeddedt.embeddium.impl.mixin.core.model;

import org.embeddedt.embeddium.api.model.EmbeddiumBakedModelExtension;
import org.spongepowered.asm.mixin.Mixin;

//? if <1.21.5-alpha.25.7.a {
@Mixin(net.minecraft.client.resources.model.BakedModel.class)
//?} else
/*@Mixin(net.minecraft.client.renderer.block.model.BlockStateModel.class)*/
public interface BakedModelMixin extends EmbeddiumBakedModelExtension {
}
//?}