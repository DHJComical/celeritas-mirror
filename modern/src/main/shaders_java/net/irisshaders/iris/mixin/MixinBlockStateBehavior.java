package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * This Mixin implements support for the ambientOcclusionLevel value. This injection point was chosen because it's
 * called by all rendering mods (so we don't need a ton of injection points), but at the same time, it's very rarely
 * overridden or injected to (because it just dispatches to a method on BlockBehavior).
 *
 * <p>Compatibility considerations:</p>
 *
 * <ul>
 *     <li>Vanilla: Calls the relevant method in its block / fluid renderers</li>
 *     <li>Sodium / FRAPI / FREX / etc: Calls this method in relevant renderers</li>
 *     <li>Better Foliage: Unusually, Better Foliage injects into this method, but it merely redirects the call to
 *         getShadeBrightness. By using a priority of 990, we apply before Better Foliage, allowing its redirect to
 *         succeed.</li>
 *     <li>Content mods: Most content mods override the method in BlockBehavior, which doesn't cause issues with
 *         this method. </li>
 * </ul>
 */
@Mixin(value = BlockBehaviour.BlockStateBase.class, priority = 990)
public abstract class MixinBlockStateBehavior {
	@Shadow
	public abstract Block getBlock();

	/**
	 * @author IMS
	 * @reason ambientOcclusionLevel support. Semantically, we're completely changing the meaning of the method.
	 */
	@ModifyReturnValue(method = "getShadeBrightness", at= @At("RETURN"))
	private float applyAoLevel(float originalValue) {
		float aoLightValue = WorldRenderingSettings.INSTANCE.getAmbientOcclusionLevel();
		return 1.0F - aoLightValue * (1.0F - originalValue);
	}
}
