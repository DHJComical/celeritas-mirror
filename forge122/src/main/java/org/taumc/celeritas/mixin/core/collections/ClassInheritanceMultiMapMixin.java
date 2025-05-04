package org.taumc.celeritas.mixin.core.collections;

import net.minecraft.util.ClassInheritanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.AbstractSet;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ClassInheritanceMultiMap.class)
public abstract class ClassInheritanceMultiMapMixin<T> extends AbstractSet<T> {
    @Shadow
    @Final
    private List<T> values;


    /**
     * @author embeddedt
     * @reason avoid iterator allocation when forEach is called
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        this.values.forEach(action);
    }
}
