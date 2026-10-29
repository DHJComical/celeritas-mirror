package org.embeddedt.embeddium.impl.gl.shader;

import org.taumc.celeritas.lwjgl.GL20;
import org.taumc.celeritas.lwjgl.GL43;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.embeddedt.embeddium.impl.gl.GlObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;

import java.util.ArrayList;
import java.util.List;

/**
 * A compiled OpenGL shader object.
 */
public class GlShader extends GlObject {
    private static final Logger LOGGER = LogManager.getLogger(GlShader.class);

    private final String name;

    public GlShader(ShaderType type, String name, String src) {
        this(name, compile(type, name, src));

        try {
            this.checkCompileStatus();
        } catch (CompileException e) {
            this.delete();
            throw e;
        }
    }

    private GlShader(String name, int handle) {
        this.name = name;
        this.setHandle(handle);
    }

    public static Deferred compileDeferred(ShaderType type, String name, String src) {
        return new Deferred(new GlShader(name, compile(type, name, src)));
    }

    private static int compile(ShaderType type, String name, String src) {
        int handle = LWJGL.glCreateShader(type.id);
        LWJGL.glShaderSourceSafe(handle, src);
        LWJGL.glCompileShader(handle);

        GLDebug.nameObject(GL43.GL_SHADER, handle, name);

        return handle;
    }

    private void checkCompileStatus() {
        int handle = this.handle();
        String log = LWJGL.glGetShaderInfoLog(handle, 4096);

        if (!log.isEmpty()) {
            LOGGER.warn("Shader compilation log for " + this.name + ": " + log);
        }

        if (LWJGL.glGetShaderi(handle, GL20.GL_COMPILE_STATUS) != GL20.GL_TRUE) {
            throw new CompileException(this.name, log);
        }
    }

    public String getName() {
        return this.name;
    }

    @Override
    protected void destroyInternal() {
        LWJGL.glDeleteShader(this.handle());
    }

    public static final class Deferred {
        private final GlShader shader;

        private Deferred(GlShader shader) {
            this.shader = shader;
        }

        public String getName() {
            return this.shader.name;
        }

        public void attachTo(int program) {
            LWJGL.glAttachShader(program, this.shader.handle());
        }

        /**
         * Waits for the compile to finish. If it failed, the shader is deleted and a {@link CompileException} thrown.
         */
        public GlShader await() {
            try {
                this.shader.checkCompileStatus();
            } catch (CompileException e) {
                this.shader.delete();
                throw e;
            }

            return this.shader;
        }

        /**
         * Waits for every shader in turn. If one failed, all of them are deleted and its {@link CompileException}
         * thrown.
         */
        public static List<GlShader> awaitAll(List<Deferred> shaders) {
            List<GlShader> compiled = new ArrayList<>(shaders.size());

            try {
                for (Deferred shader : shaders) {
                    compiled.add(shader.await());
                }
            } catch (CompileException e) {
                // The shader that failed has already deleted itself.
                for (int i = compiled.size() + 1; i < shaders.size(); i++) {
                    shaders.get(i).delete();
                }
                compiled.forEach(GlShader::delete);
                throw e;
            }

            return compiled;
        }

        public void delete() {
            this.shader.delete();
        }
    }

    public static final class CompileException extends RuntimeException {
        private final String shaderName;
        private final String log;

        public CompileException(String shaderName, String log) {
            super("Shader compilation failed for " + shaderName + ", see log for details");
            this.shaderName = shaderName;
            this.log = log;
        }

        public String getShaderName() {
            return this.shaderName;
        }

        public String getLog() {
            return this.log;
        }
    }
}
