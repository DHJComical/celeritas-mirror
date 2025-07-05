package org.embeddedt.embeddium.impl.render.matrix_stack;

import com.mojang.blaze3d.vertex.PoseStack;

public interface CachingPoseStack {
    /**
     * Enables or disables caching of matrix entries on the given matrix stack. When enabled, any
     * matrix objects returned become invalid after a call to popPose().
     * @param flag whether caching should be enabled
     */
    void embeddium$setCachingEnabled(boolean flag);

    //? if <1.21.4 {
    /**
     * @return the last pose of the stack without marking it as escaped
     */
    PoseStack.Pose celeritas$last();
    //?} else {
    /*default PoseStack.Pose celeritas$last() {
        return ((PoseStack)this).last();
    }
    *///?}

    interface Pose {
        boolean celeritas$hasEscaped();
        void celeritas$setEscaped();
    }
}
