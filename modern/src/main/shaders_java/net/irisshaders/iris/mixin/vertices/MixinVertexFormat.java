package net.irisshaders.iris.mixin.vertices;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.ImmediateState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.embeddedt.embeddium.compat.mc.MCVertexFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * Ensures that the correct state for the extended vertex format is set up when needed.
 */
@Mixin(VertexFormat.class)
public abstract class MixinVertexFormat implements MCVertexFormat {
    @Shadow
    @Final
    //? if <1.21 {
    private ImmutableMap<String, ?> elementMapping;
    //?} else
    /*private java.util.List<String> names;*/

    @Inject(method = "setupBufferState", at = @At("HEAD"), cancellable = true)
	private void iris$onSetupBufferState(CallbackInfo ci) {
		if (WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat() && ImmediateState.renderWithExtendedVertexFormat) {
			if ((Object) this == DefaultVertexFormat.BLOCK) {
				IrisVertexFormats.TERRAIN.setupBufferState();

				ci.cancel();
			} else if ((Object) this == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP) {
				IrisVertexFormats.GLYPH.setupBufferState();

				ci.cancel();
			} else if ((Object) this == DefaultVertexFormat.NEW_ENTITY) {
				IrisVertexFormats.ENTITY.setupBufferState();

				ci.cancel();
			}
		}
	}

	@Inject(method = "clearBufferState", at = @At("HEAD"), cancellable = true)
	private void iris$onClearBufferState(CallbackInfo ci) {
		if (WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat() && ImmediateState.renderWithExtendedVertexFormat) {
			if ((Object) this == DefaultVertexFormat.BLOCK) {
				IrisVertexFormats.TERRAIN.clearBufferState();

				ci.cancel();
			} else if ((Object) this == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP) {
				IrisVertexFormats.GLYPH.clearBufferState();

				ci.cancel();
			} else if ((Object) this == DefaultVertexFormat.NEW_ENTITY) {
				IrisVertexFormats.ENTITY.clearBufferState();

				ci.cancel();
			}
		}
	}

    @Override
    public Collection<String> celeritas$getElementAttributeNames() {
        //? if <1.21 {
        return this.elementMapping.keySet();
        //?} else
        /*return this.names;*/
    }
}
