package org.embeddedt.embeddium.impl.mixin.core.world.chunk;

import org.embeddedt.embeddium.impl.world.PaletteStorageExtended;
//? if >=1.18 {
import net.minecraft.util.SimpleBitStorage;
//?} else
/*import net.minecraft.util.BitStorage;*/
import net.minecraft.world.level.chunk.Palette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

//? if >=1.18 {
@Mixin(SimpleBitStorage.class)
//?} else
/*@Mixin(BitStorage.class)*/
public class PackedIntegerArrayMixin implements PaletteStorageExtended {
    @Shadow
    @Final
    private long[] data;

    @Shadow
    @Final
    private int valuesPerLong;

    @Shadow
    @Final
    private long mask;

    @Shadow
    @Final
    private int bits;

    @Shadow
    @Final
    private int size;

    @Override
    public <T> void sodium$unpack(T[] out, Palette<T> palette, T defaultValue) {
        int idx = 0;

        for (long word : this.data) {
            long l = word;

            for (int j = 0; j < this.valuesPerLong; ++j) {
                var value = palette.valueFor((int) (l & this.mask));
                if (value == null) {
                    if(defaultValue != null) {
                        value = defaultValue;
                    } else {
                        throw new NullPointerException("Palette does not contain entry for value in storage");
                    }
                }
                out[idx] = value;
                l >>= this.bits;

                if (++idx >= this.size) {
                    return;
                }
            }
        }
    }
}
