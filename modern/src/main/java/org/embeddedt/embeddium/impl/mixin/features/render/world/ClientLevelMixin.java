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
public abstract class ClientLevelMixin extends Level {

    private BlockPos.MutableBlockPos embeddium$particlePos;

    //? if >=1.20 <1.21.2 {
    protected ClientLevelMixin(WritableLevelData p_270739_, ResourceKey<Level> p_270683_, RegistryAccess p_270200_, Holder<DimensionType> p_270240_, Supplier<ProfilerFiller> p_270692_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_) {
        super(p_270739_, p_270683_, p_270200_, p_270240_, p_270692_, p_270904_, p_270470_, p_270248_, p_270466_);
    }
    //?} else if >=1.21.2 {
    /*protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }
    *///?} else if >=1.19.2 {
    /*protected ClientLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }
    *///?} else if >=1.18 {
    /*protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l) {
        super(writableLevelData, resourceKey, holder, supplier, bl, bl2, l);
    }
    *///?} else if >=1.16.2 {
    /*protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, DimensionType dimensionType, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l) {
        super(writableLevelData, resourceKey, dimensionType, supplier, bl, bl2, l);
    }
    *///?} else if >=1.16 {
    /*protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, ResourceKey<DimensionType> resourceKey2, DimensionType dimensionType, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l) {
        super(writableLevelData, resourceKey, resourceKey2, dimensionType, supplier, bl, bl2, l);
    }
    *///?} else {
    /*protected ClientLevelMixin(LevelData levelData, DimensionType dimensionType, BiFunction<Level, Dimension, ChunkSource> biFunction, ProfilerFiller profilerFiller, boolean bl) {
        super(levelData, dimensionType, biFunction, profilerFiller, bl);
    }
    *///?}

    //? if >=1.18 {
    @Shadow
    private void lambda$doAnimateTick$8(BlockPos.MutableBlockPos pos, AmbientParticleSettings settings) {throw new AssertionError();}

    private final Consumer<AmbientParticleSettings> embeddium$particleSettingsConsumer = settings -> lambda$doAnimateTick$8(embeddium$particlePos, settings);
    //?} else if >=1.17 {
    /*@Shadow
    private void lambda$doAnimateTick$5(BlockPos.MutableBlockPos pos, AmbientParticleSettings settings) {throw new AssertionError();}

    private final Consumer<AmbientParticleSettings> embeddium$particleSettingsConsumer = settings -> lambda$doAnimateTick$5(embeddium$particlePos, settings);
    *///?} else if >=1.16 {
    /*@Shadow
    private void lambda$doAnimateTick$4(BlockPos.MutableBlockPos pos, AmbientParticleSettings settings) {throw new AssertionError();}

    private final Consumer<AmbientParticleSettings> embeddium$particleSettingsConsumer = settings -> lambda$doAnimateTick$4(embeddium$particlePos, settings);
    *///?}


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
