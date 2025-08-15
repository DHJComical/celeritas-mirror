package org.embeddedt.embeddium.impl.gl.shader.uniform;

import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class GlUniformFloatArray extends GlUniform<float[]> {
    public GlUniformFloatArray(int index) {
        super(index);
    }

    @Override
    public void set(float[] value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buf = stack.callocFloat(value.length);
            buf.put(value);

            GL30C.glUniform1fv(this.index, buf);
        }
    }

    public void set(FloatBuffer value) {
        GL30C.glUniform1fv(this.index, value);
    }
}
