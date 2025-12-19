package org.taumc.celeritas.mixin.core;

//? if >=1.8 {

/*import net.minecraft.util.TypeInstanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.AbstractSet;
import java.util.List;
import java.util.function.Consumer;

@Mixin(TypeInstanceMultiMap.class)
public abstract class TypeInstanceMultimapMixin<T> extends AbstractSet<T> {
    @Shadow
    @Final
    private List<T> instances;


    /^*
     * @author embeddedt
     * @reason avoid iterator allocation when forEach is called
     ^/
    @Override
    public void forEach(Consumer<? super T> action) {
        this.instances.forEach(action);
    }
}

*///?}