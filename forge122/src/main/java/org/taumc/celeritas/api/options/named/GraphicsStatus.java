package org.taumc.celeritas.api.options.named;

public enum GraphicsStatus {
    FAST(false, "options.graphics.fast"),
    FANCY(true, "options.graphics.fancy");

    private final boolean value;
    private final String translationKey;

    GraphicsStatus(boolean value, String translationKey) {
        this.value = value;
        this.translationKey = translationKey;
    }

    public boolean getValue() {
        return this.value;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static GraphicsStatus fromBoolean(boolean value) {
        return value ? FANCY : FAST;
    }
}