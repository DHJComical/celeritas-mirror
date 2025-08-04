package org.taumc.celeritas.api.options.named;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Comparator;

public enum ParticleMode {
    ALL(0, "options.particles.all"),
    DECREASED(1, "options.particles.decreased"),
    MINIMAL(2, "options.particles.minimal");

    private static final ParticleMode[] VALUES = Arrays.stream(values())
            .sorted(Comparator.comparingInt(ParticleMode::getId))
            .toArray(ParticleMode[]::new);

    private final int id;
    private final String translationKey;

    ParticleMode(int id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public int getId() {
        return this.id;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static ParticleMode byId(int id) {
        return VALUES[(int) MathHelper.positiveModulo(id, VALUES.length)];
    }
}
