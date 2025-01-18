package org.embeddedt.embeddium.impl.mixin.features.options.render_layers;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemBlockRenderTypes.class)
public interface ItemBlockRenderTypesAccessor {
    @Accessor("renderCutout")
    static boolean celeritas$areLeavesFancy() {
        throw new AssertionError();
    }
}
