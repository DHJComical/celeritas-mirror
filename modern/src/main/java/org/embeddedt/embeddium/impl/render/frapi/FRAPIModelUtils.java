package org.embeddedt.embeddium.impl.render.frapi;

//? if ffapi {
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;

public class FRAPIModelUtils {
    public static boolean isFRAPIModel(Object model) {
        if(!FRAPIRenderHandler.INDIGO_PRESENT) {
            return false;
        }

        return !((FabricBakedModel)model).isVanillaAdapter();
    }
}
//?} else {
/*public class FRAPIModelUtils {
    public static boolean isFRAPIModel(Object model) {
        return false;
    }
}
*///?}