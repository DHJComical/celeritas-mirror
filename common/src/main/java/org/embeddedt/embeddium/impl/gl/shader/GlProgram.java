package org.embeddedt.embeddium.impl.gl.shader;

import org.taumc.celeritas.lwjgl.GL20;
import org.taumc.celeritas.lwjgl.GL30;
import org.taumc.celeritas.lwjgl.GL32;
import org.taumc.celeritas.lwjgl.GL43;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.embeddedt.embeddium.impl.gl.GlObject;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniform;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * An OpenGL shader program.
 */
public class GlProgram<T> extends GlObject implements ShaderBindingContext {
    private static final Logger LOGGER = LogManager.getLogger(GlProgram.class);

    private final T shaderInterface;

    protected GlProgram(int program, Function<ShaderBindingContext, T> interfaceFactory) {
        this.setHandle(program);
        this.shaderInterface = interfaceFactory.apply(this);
    }

    public T getInterface() {
        return this.shaderInterface;
    }

    public static Builder builder(String identifier) {
        return new Builder(identifier);
    }

    public void bind() {
        LWJGL.glUseProgram(this.handle());
    }

    public void unbind() {
        LWJGL.glUseProgram(0);
    }

    @Override
    protected void destroyInternal() {
        LWJGL.glDeleteProgram(this.handle());
    }

    @Override
    public <U extends GlUniform<?>> @Nullable U bindUniformIfPresent(String name, IntFunction<U> factory) {
        int index = LWJGL.glGetUniformLocation(this.handle(), name);

        if (index < 0) {
            return null;
        }

        return factory.apply(index);
    }

    @Override
    public @Nullable GlUniformBlock bindUniformBlockIfPresent(String name, int bindingPoint) {
        int index = LWJGL.glGetUniformBlockIndex(this.handle(), name);

        if (index < 0) {
            return null;
        }

        LWJGL.glUniformBlockBinding(this.handle(), index, bindingPoint);

        return new GlUniformBlock(bindingPoint);
    }

    public static class Builder {
        private final String name;
        private final int program;
        private final List<GlShader.Deferred> deferredShaders = new ArrayList<>();

        public Builder(String name) {
            this.name = name;
            this.program = LWJGL.glCreateProgram();
        }

        public Builder attachShader(GlShader shader) {
            LWJGL.glAttachShader(this.program, shader.handle());

            return this;
        }

        /**
         * Attaches a shader that may still be compiling. It belongs to the program from here on, and is waited for
         * and deleted when the program is linked.
         */
        public Builder attachShader(GlShader.Deferred shader) {
            shader.attachTo(this.program);
            this.deferredShaders.add(shader);

            return this;
        }

        /**
         * Links the attached shaders to this program and returns a user-defined container which wraps the shader
         * program. This container can, for example, provide methods for updating the specific uniforms of that shader
         * set.
         *
         * @param factory The factory which will create the shader program's interface
         * @param <U> The interface type for the shader program
         * @return An instantiated shader container as provided by the factory
         */
        public <U> GlProgram<U> link(Function<ShaderBindingContext, U> factory) {
            return this.linkDeferred().finish(factory);
        }

        public Deferred linkDeferred() {
            LWJGL.glLinkProgram(this.program);

            GLDebug.nameObject(GL43.GL_PROGRAM, this.program, this.name);

            return new Deferred(this.name, this.program, List.copyOf(this.deferredShaders));
        }

        public Builder bindAttribute(String name, int index) {
            LWJGL.glBindAttribLocation(this.program, index, name);

            return this;
        }

        public Builder bindAttributes(GlVertexFormat format, int firstAttributeIndex) {
            int index = firstAttributeIndex;

            for (var attribute : format.getAttributes()) {
                this.bindAttribute(attribute.getName(), index++);
            }

            return this;
        }

        public Builder bindFragmentData(String name, int index) {
            LWJGL.glBindFragDataLocation(this.program, index, name);

            return this;
        }
    }

    /**
     * A program whose shaders may still be compiling and linking. The program itself is only handed out by
     * {@link #finish}, once it is known to have linked.
     */
    public static final class Deferred {
        private final String name;
        private final int program;
        private final List<GlShader.Deferred> shaders;

        private Deferred(String name, int program, List<GlShader.Deferred> shaders) {
            this.name = name;
            this.program = program;
            this.shaders = shaders;
        }

        /**
         * Waits for the shaders to compile and the program to link, then wraps it like {@link Builder#link}. On
         * failure, the shaders and program are deleted before the exception is rethrown.
         */
        public <U> GlProgram<U> finish(Function<ShaderBindingContext, U> factory) {
            try {
                GlShader.Deferred.awaitAll(this.shaders).forEach(GlShader::delete);
            } catch (GlShader.CompileException e) {
                LWJGL.glDeleteProgram(this.program);
                throw e;
            }

            String log = LWJGL.glGetProgramInfoLog(this.program, 4096);

            if (!log.isEmpty()) {
                LOGGER.warn("Program link log for " + this.name + ": " + log);
            }

            if (LWJGL.glGetProgrami(this.program, GL20.GL_LINK_STATUS) != GL20.GL_TRUE) {
                LWJGL.glDeleteProgram(this.program);
                throw new RuntimeException("Shader program linking failed, see log for details");
            }

            return new GlProgram<>(this.program, factory);
        }

        /**
         * Deletes a program that is no longer needed, without waiting for it to finish linking.
         */
        public void delete() {
            this.shaders.forEach(GlShader.Deferred::delete);
            LWJGL.glDeleteProgram(this.program);
        }
    }
}
