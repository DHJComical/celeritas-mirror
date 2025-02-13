package org.embeddedt.embeddium.impl.mixin.core.model;

//? if <1.21.5-alpha.25.7.a {
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.resources.model.SimpleBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SimpleBakedModel.Builder.class)
public class SimpleBakedModelBuilderMixin {
    @ModifyArg(method = { "addCulledFace", "addUnculledFace" }, at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false), require = 0)
    private Object setVanillaShadingFlag(Object quad) {
        BakedQuadView view = (BakedQuadView)quad;
        view.addFlags(ModelQuadFlags.IS_VANILLA_SHADED);
        return quad;
    }
}
//?} else {

/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.resources.model.QuadCollection;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SimpleUnbakedGeometry.class)
public class SimpleBakedModelBuilderMixin {
    @ModifyReturnValue(method = "bake(Ljava/util/List;Lnet/minecraft/client/renderer/block/model/TextureSlots;Lnet/minecraft/client/resources/model/SpriteGetter;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/QuadCollection;", at = @At("RETURN"))
    private static QuadCollection setVanillaShadingFlag(QuadCollection collection) {
        for (var quad : collection.getAll()) {
            BakedQuadView view = (BakedQuadView)(Object)quad;
            view.addFlags(ModelQuadFlags.IS_VANILLA_SHADED);
        }
        return collection;
    }
}
*///?}