// This file is based on code from Sodium by JellySquid, licensed under the LGPLv3 license.

package net.irisshaders.iris.gl.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.IrisRenderSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.KHRDebug;

import java.util.List;

public class ProgramCreator {
	private static final Logger LOGGER = LogManager.getLogger(ProgramCreator.class);

	/**
	 * Compiles and links a program without waiting for either to finish, so that the driver can work on it while
	 * further programs are submitted. The program must not be used before {@link Submitted#finish()} returns.
	 */
	public static Submitted submit(String name, List<GlShader.Deferred> shaders) {
		RenderSystem.assertOnRenderThread();

		int program = GlStateManager.glCreateProgram();

		GlStateManager._glBindAttribLocation(program, 11, "iris_Entity");
		GlStateManager._glBindAttribLocation(program, 11, "mc_Entity");
		GlStateManager._glBindAttribLocation(program, 12, "mc_midTexCoord");
		GlStateManager._glBindAttribLocation(program, 13, "at_tangent");
		GlStateManager._glBindAttribLocation(program, 14, "at_midBlock");

		GlStateManager._glBindAttribLocation(program, 0, "Position");
		GlStateManager._glBindAttribLocation(program, 1, "UV0");

		for (GlShader.Deferred shader : shaders) {
			shader.attachTo(program);
		}

		GlStateManager.glLinkProgram(program);

		GLDebug.nameObject(KHRDebug.GL_PROGRAM, program, name);

		return new Submitted(name, program, shaders);
	}

	public static final class Submitted {
		private final String name;
		private final int program;
		private final List<GlShader.Deferred> shaders;
		private boolean done;

		private Submitted(String name, int program, List<GlShader.Deferred> shaders) {
			this.name = name;
			this.program = program;
			this.shaders = shaders;
		}

		public String getName() {
			return name;
		}

		/**
		 * Waits for the program's shaders to compile and the program to link, then returns its handle. On failure,
		 * everything is deleted and a {@link ShaderCompileException} thrown.
		 */
		public int finish() {
			RenderSystem.assertOnRenderThread();
			checkNotDone();
			done = true;

			List<GlShader> compiled;

			try {
				compiled = GlShader.Deferred.awaitAll(shaders);
			} catch (GlShader.CompileException e) {
				GlStateManager.glDeleteProgram(program);
				throw new ShaderCompileException(e.getShaderName(), e.getLog());
			}

			String log = IrisRenderSystem.getProgramInfoLog(program);

			if (!log.isEmpty()) {
				LOGGER.warn("Program link log for " + name + ": " + log);
			}

			boolean linked = GlStateManager.glGetProgrami(program, GL20C.GL_LINK_STATUS) == GL20C.GL_TRUE;

			//Always detach shaders according to https://www.khronos.org/opengl/wiki/Shader_Compilation#Cleanup
			for (GlShader shader : compiled) {
				IrisRenderSystem.detachShader(program, shader.handle());
				shader.delete();
			}

			if (!linked) {
				GlStateManager.glDeleteProgram(program);
				throw new ShaderCompileException(name, log);
			}

			return program;
		}

		/**
		 * Deletes a program that is no longer needed, without waiting for it to finish compiling.
		 */
		public void delete() {
			checkNotDone();
			done = true;

			shaders.forEach(GlShader.Deferred::delete);
			GlStateManager.glDeleteProgram(program);
		}

		private void checkNotDone() {
			if (done) {
				throw new IllegalStateException("Program " + name + " was already finished or deleted");
			}
		}
	}
}
