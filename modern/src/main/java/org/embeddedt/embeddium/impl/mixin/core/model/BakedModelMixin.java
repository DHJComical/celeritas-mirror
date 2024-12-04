//? if >=1.15 {
package org.embeddedt.embeddium.impl.mixin.core.model;

import net.minecraft.client.resources.model.BakedModel;
import org.embeddedt.embeddium.api.model.EmbeddiumBakedModelExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BakedModel.class)
public interface BakedModelMixin extends EmbeddiumBakedModelExtension {
}
//?}