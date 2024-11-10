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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

//? if >=1.18 {
@Mixin(SimpleBitStorage.class)
//?} else
/*@Mixin(BitStorage.class)*/
public class PackedIntegerArrayMixin implements PaletteStorageExtended {
    @Shadow
    @Final
    private long[] data;

    //? if >=1.16 {
    @Shadow
    @Final
    private int valuesPerLong;
    //?}

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
        //? if >=1.16 {
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
        //?} else {
        /*int i = this.data.length;
        if (i != 0) {
            int j = 0;
            long l = this.data[0];
            long m = i > 1 ? this.data[1] : 0L;

            for(int idx = 0; idx < this.size; ++idx) {
                int n = idx * this.bits;
                int o = n >> 6;
                int p = (idx + 1) * this.bits - 1 >> 6;
                int q = n ^ o << 6;
                if (o != j) {
                    l = m;
                    m = o + 1 < i ? this.data[o + 1] : 0L;
                    j = o;
                }

                int id;
                if (o == p) {
                    id = (int)(l >>> q & this.mask);
                } else {
                    int r = 64 - q;
                    id = (int)((l >>> q | m << r) & this.mask);
                }
                var value = palette.valueFor(id);
                if (value == null) {
                    if(defaultValue != null) {
                        value = defaultValue;
                    } else {
                        throw new NullPointerException("Palette does not contain entry for value in storage");
                    }
                }
                out[idx] = value;
            }
        }
        *///?}
    }
}
