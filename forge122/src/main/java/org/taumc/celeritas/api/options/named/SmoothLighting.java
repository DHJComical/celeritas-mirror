package org.taumc.celeritas.api.options.named;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Comparator;

public enum SmoothLighting {
    OFF(0, "options.ao.off"),
    MINIMAL(1, "options.ao.min"),
    MAXIMUM(2, "options.ao.max");

    private static final SmoothLighting[] VALUES = Arrays.stream(values())
            .sorted(Comparator.comparingInt(SmoothLighting::getId))
            .toArray(SmoothLighting[]::new);

    private final int id;
    private final String translationKey;

    SmoothLighting(int id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public int getId() {
        return this.id;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static SmoothLighting byId(int id) {
        return VALUES[(int) MathHelper.positiveModulo(id, VALUES.length)];
    }
}
