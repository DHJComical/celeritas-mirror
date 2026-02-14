package org.embeddedt.embeddium.impl.gl.shader;

import org.taumc.celeritas.lwjgl.GL20;
import org.taumc.celeritas.lwjgl.GL32;
import org.taumc.celeritas.lwjgl.GL42;
import org.taumc.celeritas.lwjgl.GL43;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * An enumeration over the supported OpenGL shader types.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ShaderType {
    VERTEX(GL20.GL_VERTEX_SHADER, "vsh"),
    FRAGMENT(GL20.GL_FRAGMENT_SHADER, "fsh"),
    GEOM(GL32.GL_GEOMETRY_SHADER, "gsh"),
    TESS_CTRL(GL42.GL_TESS_CONTROL_SHADER, "tcs"),
    TESS_EVALUATE(GL42.GL_TESS_EVALUATION_SHADER, "tes"),
    COMPUTE(GL43.GL_COMPUTE_SHADER, "csh");

    @Deprecated
    public static final ShaderType TESSELATION_CONTROL = ShaderType.TESS_CTRL;
    @Deprecated
    public static final ShaderType TESSELATION_EVAL = ShaderType.TESS_EVALUATE;
    @Deprecated
    public static final ShaderType GEOMETRY = ShaderType.GEOM;

    public final int id;
    public final String fileExtension;
}
