package net.irisshaders.iris.compat.flywheel.gl;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

import java.util.Optional;

public enum ClrwlShaderType {
    VERTEX("vertex", "VERTEX_SHADER", "vert", GL20.GL_VERTEX_SHADER),
    FRAGMENT("fragment", "FRAGMENT_SHADER", "frag", GL20.GL_FRAGMENT_SHADER),
    GEOMETRY("geometry", "GEOMETRY_SHADER", "geom", GL43.GL_GEOMETRY_SHADER),
    COMPUTE("compute", "COMPUTE_SHADER", "glsl", GL43.GL_COMPUTE_SHADER),
    ;

    public final String name;
    public final String define;
    public final String extension;
    public final int glEnum;

    ClrwlShaderType(String name, String define, String extension, int glEnum) {
        this.name = name;
        this.define = define;
        this.extension = extension;
        this.glEnum = glEnum;
    }

    public Optional<dev.engine_room.flywheel.backend.gl.shader.ShaderType> toFlw()
    {
        switch (this)
        {
            case VERTEX ->
            {
                return Optional.of(dev.engine_room.flywheel.backend.gl.shader.ShaderType.VERTEX);
            }
            case FRAGMENT ->
            {
                return Optional.of(dev.engine_room.flywheel.backend.gl.shader.ShaderType.FRAGMENT);
            }
            case GEOMETRY ->
            {
                return Optional.empty();
            }
            case COMPUTE ->
            {
                return Optional.of(dev.engine_room.flywheel.backend.gl.shader.ShaderType.COMPUTE);
            }
        }

        throw new RuntimeException("Unknown ShaderType: " + this);
    }
}
