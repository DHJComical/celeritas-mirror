package org.embeddedt.embeddium.impl.mixin.features.dfu;

//? if forge && <1.19 {
/*import net.minecraft.util.datafix.DataFixers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.concurrent.Executor;

@Mixin(DataFixers.class)
public class DataFixersMixin {
    @ModifyArg(method = "createFixerUpper", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/DataFixerBuilder;build(Ljava/util/concurrent/Executor;)Lcom/mojang/datafixers/DataFixer;"), require = 0, index = 0)
    private static Executor buildWithoutOptimizing(Executor par1) {
        return task -> {};
    }
}
*///?}