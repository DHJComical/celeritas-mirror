package org.taumc.celeritas.api.options.named;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Comparator;

public enum CloudStatus {
    OFF(0, "options.off"),
    FAST(1, "options.clouds.fast"),
    FANCY(2, "options.clouds.fancy");

    private static final CloudStatus[] VALUES = Arrays.stream(values())
            .sorted(Comparator.comparingInt(CloudStatus::getId))
            .toArray(CloudStatus[]::new);

    private final int id;
    private final String translationKey;

    CloudStatus(int id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public int getId() {
        return this.id;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static CloudStatus byId(int id) {
        return VALUES[(int) MathHelper.positiveModulo(id, VALUES.length)];
    }
}