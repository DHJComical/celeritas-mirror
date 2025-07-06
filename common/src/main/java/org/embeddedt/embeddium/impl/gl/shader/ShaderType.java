package org.embeddedt.embeddium.impl.gl.shader;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL43C;

/**
 * An enumeration over the supported OpenGL shader types.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ShaderType {
    VERTEX(GL20C.GL_VERTEX_SHADER, "vsh"),
    FRAGMENT(GL20C.GL_FRAGMENT_SHADER, "fsh"),
    GEOM(GL32C.GL_GEOMETRY_SHADER, "gsh"),
    TESS_CTRL(GL42C.GL_TESS_CONTROL_SHADER, "tcs"),
    TESS_EVALUATE(GL42C.GL_TESS_EVALUATION_SHADER, "tes"),
    COMPUTE(GL43C.GL_COMPUTE_SHADER, "csh");

    @Deprecated
    public static final ShaderType TESSELATION_CONTROL = ShaderType.TESS_CTRL;
    @Deprecated
    public static final ShaderType TESSELATION_EVAL = ShaderType.TESS_EVALUATE;
    @Deprecated
    public static final ShaderType GEOMETRY = ShaderType.GEOM;

    public final int id;
    public final String fileExtension;
}
