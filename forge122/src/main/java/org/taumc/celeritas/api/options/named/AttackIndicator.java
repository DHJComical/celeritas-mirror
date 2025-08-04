package org.taumc.celeritas.api.options.named;

import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.util.math.MathHelper;

public enum AttackIndicator {
    OFF(0, "options.off"),
    CROSSHAIR(1, "options.attack.crosshair"),
    HOTBAR(2, "options.attack.hotbar");

    private static final AttackIndicator[] VALUES = Arrays.stream(values()).sorted(Comparator.comparingInt(AttackIndicator::getId)).toArray(AttackIndicator[]::new);
    private final int id;
    private final String translationKey;

    AttackIndicator(int id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public int getId() {
        return this.id;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static AttackIndicator byId(int id) {
        return VALUES[(int) MathHelper.positiveModulo(id, VALUES.length)];
    }
}
