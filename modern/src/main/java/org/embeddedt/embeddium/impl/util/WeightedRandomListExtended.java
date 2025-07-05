package org.embeddedt.embeddium.impl.util.collections;

//$ rng_import
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public interface WeightedRandomListExtended<E> {
    /**
     * Like getRandomItem, but avoids allocating an Optional.
     */
    @Nullable E embeddium$getRandomItem(/*$ rng >>*/ RandomSource random);
}