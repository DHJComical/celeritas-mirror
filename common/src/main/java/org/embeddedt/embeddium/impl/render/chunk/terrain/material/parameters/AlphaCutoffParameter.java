package org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public enum AlphaCutoffParameter {
    // Must stay in step with ALPHA_CUTOFF in chunk_material.glsl, which is indexed by ordinal.
    ZERO(0.0f),
    ONE_HUNDREDTH(0.01f),
    ONE_TENTH(0.1f),
    HALF(0.5f);

    private final float cutoff;

    public static AlphaCutoffParameter valueOf(float val) {
        for (var param : AlphaCutoffParameter.values()) {
            if (Math.abs(param.cutoff() - val) < 0.001f) {
                return param;
            }
        }
        throw new IllegalArgumentException("No alpha cutoff option found for value: " + val);
    }
}
