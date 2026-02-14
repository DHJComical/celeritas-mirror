package org.embeddedt.embeddium.impl.gl.shader.uniform;

import org.taumc.celeritas.lwjgl.GL30;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.joml.Matrix4fc;
import org.taumc.celeritas.lwjgl.MemoryStack;

import java.nio.FloatBuffer;

public class GlUniformMatrix4f extends GlUniform<Matrix4fc>  {
    public GlUniformMatrix4f(int index) {
        super(index);
    }

    @Override
    public void set(Matrix4fc value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buf = stack.callocFloat(16);
            value.get(buf);

            LWJGL.glUniformMatrix4fv(this.index, false, buf);
        }
    }
}
