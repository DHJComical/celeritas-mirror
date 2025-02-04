package org.embeddedt.embeddium.impl.mixin.core.render;

//? if <1.21.4 {
import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.impl.render.matrix_stack.CachingPoseStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PoseStack.Pose.class)
public class PoseMixin implements CachingPoseStack.Pose {
    private boolean celeritas$hasEscaped;

    @Override
    public boolean celeritas$hasEscaped() {
        return celeritas$hasEscaped;
    }

    @Override
    public void celeritas$setEscaped() {
        celeritas$hasEscaped = true;
    }
}
//?}