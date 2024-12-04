package org.embeddedt.embeddium.impl.render.shader;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.gl.shader.ShaderParser;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;

public class ShaderLoader {
    /**
     * Creates an OpenGL shader from GLSL sources. The GLSL source file should be made available on the classpath at the
     * path of `/assets/{namespace}/shaders/{path}`. User defines can be used to declare variables in the shader source
     * after the version header, allowing for conditional compilation with macro code.
     *
     * @param type The type of shader to create
     * @param name The identifier used to locate the shader source file
     * @param constants A list of constants for shader specialization
     * @return An OpenGL shader object compiled with the given user defines
     */
    public static GlShader loadShader(ShaderType type, ResourceLocation name, ShaderConstants constants) {
        return new GlShader(type, name.toString(), ShaderParser.parseShader(getShaderSource(name), ShaderLoader::getShaderSource, constants));
    }

    public static String getShaderSource(String name) {
        String[] splitStr = name.split(":");
        return getShaderSource(ResourceLocationUtil.make(splitStr[0], splitStr[1]));
    }

    public static String getShaderSource(ResourceLocation name) {
        String path = String.format("/assets/%s/shaders/%s", name.getNamespace(), name.getPath());

        try (InputStream in = ShaderLoader.class /*? if forgelike && >=1.17 {*/.getClassLoader()/*?}*/.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Shader not found: " + path);
            }

            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader source for " + path, e);
        }
    }
}
