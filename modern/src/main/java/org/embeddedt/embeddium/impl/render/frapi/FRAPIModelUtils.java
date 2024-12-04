package org.embeddedt.embeddium.impl.render.frapi;


import net.minecraft.client.resources.model.BakedModel;

//? if ffapi {
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;

public class FRAPIModelUtils {
    public static boolean isFRAPIModel(BakedModel model) {
        if(!FRAPIRenderHandler.INDIGO_PRESENT) {
            return false;
        }

        return !((FabricBakedModel)model).isVanillaAdapter();
    }
}
//?} else {
/*public class FRAPIModelUtils {
    public static boolean isFRAPIModel(BakedModel model) {
        return false;
    }
}
*///?}