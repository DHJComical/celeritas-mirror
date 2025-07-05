package org.embeddedt.embeddium.impl.mixin.core.model;

// TODO enable this, once we find a way to handle the fact that a cutout texture shouldn't render as solid with mips
//? if (forge && !forge) && >=1.19 {

/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
//? if forge
import net.minecraftforge.client.ChunkRenderTypeSet;
//? if neoforge
/^import net.neoforged.neoforge.client.ChunkRenderTypeSet;^/
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(SimpleBakedModel.class)
public class SimpleBakedModelMixin {
    @Shadow
    @Final
    @Mutable
    protected ChunkRenderTypeSet blockRenderTypes;

    @Shadow
    @Final
    protected Map<Direction, List<BakedQuad>> culledFaces;

    @Shadow
    @Final
    protected List<BakedQuad> unculledFaces;

    private static final ChunkRenderTypeSet CELERITAS$SOLID = ChunkRenderTypeSet.of(RenderType.solid());

    /^*
     * @author embeddedt
     * @reason Try to assign a strict render type to the model based on its textures,
     * to avoid the hash map lookup where possible.
     ^/
    @Inject(method = "/<init>/", at = @At(value = "RETURN"))
    private void celeritas$clampRenderType(CallbackInfo ci) {
        if (this.blockRenderTypes == null) {
            SpriteTransparencyLevel worstLevel = findWorstLevel(unculledFaces);
            if (worstLevel == null) {
                return;
            }
            for (var list : culledFaces.values()) {
                var newWorst = findWorstLevel(list);
                if (newWorst == null) {
                    return;
                }
                worstLevel = worstLevel.chooseNextLevel(newWorst);
            }
            if (worstLevel == SpriteTransparencyLevel.OPAQUE) {
                this.blockRenderTypes = CELERITAS$SOLID;
            }
        }
    }

    private static SpriteTransparencyLevel findWorstLevel(List<BakedQuad> quads) {
        SpriteTransparencyLevel worstLevel = SpriteTransparencyLevel.OPAQUE;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < quads.size(); i++) {
            var quad = quads.get(i);
            var level = ((BakedQuadView)quad).getTransparencyLevel();
            if (level == null) {
                return null;
            }
            worstLevel = worstLevel.chooseNextLevel(level);
        }
        return worstLevel;
    }

}

*///?}