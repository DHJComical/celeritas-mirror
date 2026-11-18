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
    VERTEX(GL20.GL_VERTEX_SHADER, "vsh", "VERTEX_SHADER"),
    FRAGMENT(GL20.GL_FRAGMENT_SHADER, "fsh", "FRAGMENT_SHADER"),
    GEOM(GL32.GL_GEOMETRY_SHADER, "gsh", "GEOMETRY_SHADER"),
    TESS_CTRL(GL42.GL_TESS_CONTROL_SHADER, "tcs", "TESS_CONTROL_SHADER"),
    TESS_EVALUATE(GL42.GL_TESS_EVALUATION_SHADER, "tes", "TESS_EVALUATION_SHADER"),
    COMPUTE(GL43.GL_COMPUTE_SHADER, "csh", "COMPUTE_SHADER");

    @Deprecated
    public static final ShaderType TESSELATION_CONTROL = ShaderType.TESS_CTRL;
    @Deprecated
    public static final ShaderType TESSELATION_EVAL = ShaderType.TESS_EVALUATE;
    @Deprecated
    public static final ShaderType GEOMETRY = ShaderType.GEOM;

    public final int id;
    public final String fileExtension;
    public final String stageDefine;
}
