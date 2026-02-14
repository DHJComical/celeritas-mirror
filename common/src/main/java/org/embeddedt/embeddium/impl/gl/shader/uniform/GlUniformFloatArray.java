package org.embeddedt.embeddium.impl.gl.shader.uniform;

import org.taumc.celeritas.lwjgl.MemoryStack;
import org.taumc.celeritas.lwjgl.GL30;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;


import java.nio.FloatBuffer;

public class GlUniformFloatArray extends GlUniform<float[]> {
    public GlUniformFloatArray(int index) {
        super(index);
    }

    @Override
    public void set(float[] value) {
        try (MemoryStack stack = LWJGL.stackPush()) {
            FloatBuffer buf = stack.callocFloat(value.length);
            buf.put(value);

            LWJGL.glUniform1fv(this.index, buf);
        }
    }

    public void set(FloatBuffer value) {
        LWJGL.glUniform1fv(this.index, value);
    }
}
