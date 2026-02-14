package org.embeddedt.embeddium.impl.gl.shader.uniform;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.joml.Matrix3f;
import org.taumc.celeritas.lwjgl.MemoryStack;

import java.nio.FloatBuffer;

public class GlUniformMatrix3f extends GlUniform<Matrix3f> {
    public GlUniformMatrix3f(int index) {
        super(index);
    }

    @Override
    public void set(Matrix3f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buf = stack.callocFloat(12);
            value.get(buf);

            LWJGL.glUniformMatrix3fv(this.index, false, buf);
        }
    }
}
