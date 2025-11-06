package org.embeddedt.embeddium.impl.gl.util;

import org.embeddedt.embeddium.impl.gl.array.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.NativeType;

import java.nio.IntBuffer;

/**
 * Provides an abstraction over various VAO implementations.
 */
public class VAOUtil {
	public static final boolean coreSupported = GL.getCapabilities().OpenGL32;
	public static final boolean arbSupported = GL.getCapabilities().GL_ARB_vertex_array_object;
	public static final boolean appleSupported = APPLEVertexArrayObject.supported;

	public static final boolean supported = coreSupported || arbSupported || appleSupported;

	public static void glBindVertexArray(@NativeType("GLuint") int array) {
		if (coreSupported) {
			GL30C.glBindVertexArray(array);
		} else if (arbSupported) {
			ARBVertexArrayObject.glBindVertexArray(array);
		} else if (appleSupported) {
			APPLEVertexArrayObject.glBindVertexArrayAPPLE(array);
		} else {
			throw new UnsupportedOperationException("VAO not supported");
		}
	}

	public static void nglDeleteVertexArrays(int n, long arrays) {
		if (coreSupported) {
			GL30C.nglDeleteVertexArrays(n, arrays);
		} else if (arbSupported) {
			ARBVertexArrayObject.nglDeleteVertexArrays(n, arrays);
		} else if (appleSupported) {
			APPLEVertexArrayObject.nglDeleteVertexArraysAPPLE(n, arrays);
		} else {
			throw new UnsupportedOperationException("VAO not supported");
		}
	}

	public static void glDeleteVertexArrays(@NativeType("GLuint const *") IntBuffer arrays) {
		if (coreSupported) {
			GL30C.glDeleteVertexArrays(arrays);
		} else if (arbSupported) {
			ARBVertexArrayObject.glDeleteVertexArrays(arrays);
		} else if (appleSupported) {
			APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(arrays);
		} else {
			throw new UnsupportedOperationException("VAO not supported");
		}
	}

	public static void glDeleteVertexArrays(@NativeType("GLuint const *") int array) {
		if (coreSupported) {
			GL30C.glDeleteVertexArrays(array);
		} else if (arbSupported) {
			ARBVertexArrayObject.glDeleteVertexArrays(array);
		} else if (appleSupported) {
			APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(array);
		} else {
			throw new UnsupportedOperationException("VAO not supported");
		}
	}

	public static void nglGenVertexArrays(int n, long arrays) {
		if (coreSupported) {
			GL30C.nglGenVertexArrays(n, arrays);
		} else if (arbSupported) {
			ARBVertexArrayObject.nglGenVertexArrays(n, arrays);
		} else if (appleSupported) {
			APPLEVertexArrayObject.nglGenVertexArraysAPPLE(n, arrays);
		} else {
			throw new UnsupportedOperationException("VAO not supported");
		}
	}

	public static void glGenVertexArrays(@NativeType("GLuint *") IntBuffer arrays) {
		if (coreSupported) {
			GL30C.glGenVertexArrays(arrays);
		} else if (arbSupported) {
			ARBVertexArrayObject.glGenVertexArrays(arrays);
		} else if (appleSupported) {
			APPLEVertexArrayObject.glGenVertexArraysAPPLE(arrays);
		} else {
			throw new UnsupportedOperationException("VAO not supported");
		}
	}

	public static int glGenVertexArrays() {
		if (coreSupported) {
			return GL30C.glGenVertexArrays();
		} else if (arbSupported) {
			return ARBVertexArrayObject.glGenVertexArrays();
		} else if (appleSupported) {
			return APPLEVertexArrayObject.glGenVertexArraysAPPLE();
		} else {
			throw new UnsupportedOperationException("VAO not supported");
		}
	}

	public static boolean glIsVertexArray(@NativeType("GLuint") int array) {
		if (coreSupported) {
			return GL30C.glIsVertexArray(array);
		} else if (arbSupported) {
			return ARBVertexArrayObject.glIsVertexArray(array);
		} else if (appleSupported) {
			return APPLEVertexArrayObject.glIsVertexArrayAPPLE(array);
		} else {
			throw new UnsupportedOperationException("VAO not supported");
		}
	}
}
