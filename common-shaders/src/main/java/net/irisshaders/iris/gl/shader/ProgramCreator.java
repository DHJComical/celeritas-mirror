// This file is based on code from Sodium by JellySquid, licensed under the LGPLv3 license.

package net.irisshaders.iris.gl.shader;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;

import net.irisshaders.iris.gl.IrisRenderSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.KHRDebug;

public class ProgramCreator {
	private static final Logger LOGGER = LogManager.getLogger(ProgramCreator.class);

	public static int create(String name, GlShader... shaders) {
		int program = GL_STATE_MANAGER.glCreateProgram();

		GL_STATE_MANAGER.glBindAttribLocation(program, 11, "iris_Entity");
		GL_STATE_MANAGER.glBindAttribLocation(program, 11, "mc_Entity");
		GL_STATE_MANAGER.glBindAttribLocation(program, 12, "mc_midTexCoord");
		GL_STATE_MANAGER.glBindAttribLocation(program, 13, "at_tangent");
		GL_STATE_MANAGER.glBindAttribLocation(program, 14, "at_midBlock");

		GL_STATE_MANAGER.glBindAttribLocation(program, 0, "Position");
		GL_STATE_MANAGER.glBindAttribLocation(program, 1, "UV0");

		for (GlShader shader : shaders) {
			GLDebug.nameObject(KHRDebug.GL_SHADER, shader.handle(), shader.getName());

			GL_STATE_MANAGER.glAttachShader(program, shader.handle());
		}

        GL_STATE_MANAGER.glLinkProgram(program);

//		GLDebug.nameObject(KHRDebug.GL_PROGRAM, program, name);

		//Always detach shaders according to https://www.khronos.org/opengl/wiki/Shader_Compilation#Cleanup
		for (GlShader shader : shaders) {
			IrisRenderSystem.detachShader(program, shader.handle());
		}

		String log = IrisRenderSystem.getProgramInfoLog(program);

		if (!log.isEmpty()) {
			LOGGER.warn("Program link log for " + name + ": " + log);
		}

		int result = GL_STATE_MANAGER.glGetProgrami(program, GL20C.GL_LINK_STATUS);

		if (result != GL20C.GL_TRUE) {
			throw new ShaderCompileException(name, log);
		}

		return program;
	}
}
