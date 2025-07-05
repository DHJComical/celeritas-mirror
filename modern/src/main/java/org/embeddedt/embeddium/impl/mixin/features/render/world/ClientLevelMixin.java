package org.embeddedt.embeddium.impl.mixin.features.render.world;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.18.2
import net.minecraft.core.Holder;
//? if >=1.16
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
//? if >=1.16
import net.minecraft.resources.ResourceKey;
//$ rng_import
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
//? if >=1.16
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
//? if <1.16
/*import net.minecraft.world.level.dimension.Dimension;*/
import net.minecraft.world.level.dimension.DimensionType;
//? if >=1.18 {
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
//?}
import net.minecraft.world.level.material.FluidState;
//? if >=1.16
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.level.storage.LevelData;
import org.embeddedt.embeddium.impl.util.rand.XoRoShiRoRandom;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Use a very low priority so most injects into doAnimateTick will still work
@Mixin(value = ClientLevel.class, priority = 500)
public class ClientLevelMixin {

    private BlockPos.MutableBlockPos embeddium$particlePos;

    //$ doAnimateTickBiomeLambda
    @Shadow private void lambda$doAnimateTick$8(BlockPos.MutableBlockPos pos, AmbientParticleSettings settings) {throw new AssertionError();} private final Consumer<AmbientParticleSettings> embeddium$particleSettingsConsumer = settings -> lambda$doAnimateTick$8(embeddium$particlePos, settings);

    //? if >=1.19 {
    @Redirect(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
    private RandomSource createLocal() {
        return new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
    }
    //?} else {
    /*@Redirect(method = "animateTick", at = @At(value = "NEW", target = "()Ljava/util/Random;"))
    private Random createLocal() {
        return new XoRoShiRoRandom();
    }
    *///?}

    /**
     * @author embeddedt
     * @reason Avoid INVOKEDYNAMIC
     */
    @ModifyExpressionValue(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;getAmbientParticle()Ljava/util/Optional;"))
    private Optional<AmbientParticleSettings> celeritas$setupForAnimateTickLambdaReplacement(Optional<AmbientParticleSettings> settingsOpt, @Local(ordinal = 0, argsOnly = true) BlockPos.MutableBlockPos pos) {
        // Save the position so it can be accessed inside the capture
        embeddium$particlePos = pos;
        return settingsOpt;
    }
}
