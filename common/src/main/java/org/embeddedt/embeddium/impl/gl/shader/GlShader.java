package org.embeddedt.embeddium.impl.gl.shader;

import org.taumc.celeritas.lwjgl.GL20;
import org.taumc.celeritas.lwjgl.GL43;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.embeddedt.embeddium.impl.gl.GlObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;

/**
 * A compiled OpenGL shader object.
 */
public class GlShader extends GlObject {
    private static final Logger LOGGER = LogManager.getLogger(GlShader.class);

    private final String name;

    public GlShader(ShaderType type, String name, String src) {
        this.name = name;

        int handle = LWJGL.glCreateShader(type.id);
        LWJGL.glShaderSourceSafe(handle, src);
        LWJGL.glCompileShader(handle);

        String log = LWJGL.glGetShaderInfoLog(handle, 4096);

        if (!log.isEmpty()) {
            LOGGER.warn("Shader compilation log for " + this.name + ": " + log);
        }

        int result = LWJGL.glGetShaderi(handle, GL20.GL_COMPILE_STATUS);

        if (result != GL20.GL_TRUE) {
            throw new RuntimeException("Shader compilation failed, see log for details");
        }

        this.setHandle(handle);

        GLDebug.nameObject(GL43.GL_SHADER, handle, name);
    }

    public String getName() {
        return this.name;
    }

    @Override
    protected void destroyInternal() {
        LWJGL.glDeleteShader(this.handle());
    }
}
