package net.irisshaders.iris.compat.sodium.mixin.shadow_map;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.embeddedt.embeddium.impl.render.chunk.PositionedViewport;
import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSectionManager.class)
public class MixinRenderSectionManager {
	@Unique
	private @NotNull SortedRenderLists shadowRenderLists = SortedRenderLists.empty();

    @ModifyExpressionValue(method = "getRenderLists", at = @At(value = "FIELD", target = "Lorg/embeddedt/embeddium/impl/render/chunk/RenderSectionManager;renderLists:Lorg/embeddedt/embeddium/impl/render/chunk/lists/SortedRenderLists;"))
    private SortedRenderLists useShadowRenderListsGet(SortedRenderLists lists) {
        return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? shadowRenderLists : lists;
    }

    @WrapOperation(method = "setRenderLists", at = @At(value = "FIELD", target = "Lorg/embeddedt/embeddium/impl/render/chunk/RenderSectionManager;renderLists:Lorg/embeddedt/embeddium/impl/render/chunk/lists/SortedRenderLists;"))
    private void useShadowRenderListsSet(RenderSectionManager instance, SortedRenderLists value, Operation<Void> original) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) shadowRenderLists = value;
        else original.call(instance, value);
    }

	@Inject(method = "update", at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/impl/render/chunk/RenderSectionManager;createTerrainRenderList(Lorg/embeddedt/embeddium/impl/render/chunk/PositionedViewport;IZ)V", shift = At.Shift.AFTER), cancellable = true)
	private void cancelIfShadow(PositionedViewport positionedViewport, int frame, boolean spectator, CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) ci.cancel();
	}
}
