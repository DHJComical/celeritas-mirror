package org.embeddedt.embeddium.impl.model.quad.properties;

import lombok.Getter;
import org.embeddedt.embeddium.api.util.NormI8;

public enum ModelQuadFacing {
    POS_X(NormI8.pack(1, 0, 0)),
    POS_Y(NormI8.pack(0, 1, 0)),
    POS_Z(NormI8.pack(0, 0, 1)),
    NEG_X(NormI8.pack(-1, 0, 0)),
    NEG_Y(NormI8.pack(0, -1, 0)),
    NEG_Z(NormI8.pack(0, 0, -1)),
    UNASSIGNED(0);

    public static final ModelQuadFacing[] VALUES = ModelQuadFacing.values();

    public static final int COUNT = VALUES.length;

    public static final int NONE = 0;
    public static final int ALL = (1 << COUNT) - 1;

    @Getter
    private final int packedNormal;

    ModelQuadFacing(int packedNormal) {
        this.packedNormal = packedNormal;
    }

    public ModelQuadFacing getOpposite() {
        return switch (this) {
            case POS_Y -> NEG_Y;
            case NEG_Y -> POS_Y;
            case POS_X -> NEG_X;
            case NEG_X -> POS_X;
            case POS_Z -> NEG_Z;
            case NEG_Z -> POS_Z;
            default -> UNASSIGNED;
        };
    }
}
