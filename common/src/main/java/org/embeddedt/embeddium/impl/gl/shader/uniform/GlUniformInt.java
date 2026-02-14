package org.embeddedt.embeddium.impl.gl.shader.uniform;

import org.taumc.celeritas.lwjgl.GL30;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;


public class GlUniformInt extends GlUniform<Integer> {
    public GlUniformInt(int index) {
        super(index);
    }

    @Override
    public void set(Integer value) {
        this.setInt(value);
    }

    public void setInt(int value) {
        LWJGL.glUniform1i(this.index, value);
    }
}
