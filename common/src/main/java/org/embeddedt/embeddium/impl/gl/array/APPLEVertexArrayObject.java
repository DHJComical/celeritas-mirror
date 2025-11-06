package org.embeddedt.embeddium.impl.gl.array;

import org.lwjgl.opengl.GL;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryUtil.memAddress;

/**
 * Native bindings to the <a href="https://registry.khronos.org/OpenGL/extensions/APPLE/APPLE_vertex_array_object.txt">APPLE_vertex_array_object</a> extension.
 * <p>This extension introduces named vertex array objects which encapsulate vertex array state on the client side.
 * The main purpose of these objects is to keep pointers to static vertex data and provide a name for different sets of static vertex data.</p>
 *
 * <p>By extending vertex array range functionality this extension allows multiple vertex array ranges to exist at one time,
 * including their complete sets of state, in manner analogous to texture objects.</p>
 *
 * <p>GenVertexArraysAPPLE creates a list of n number of vertex array object names. After creating a name,
 * BindVertexArrayAPPLE associates the name with a vertex array object and selects this vertex array and its associated state as current.
 * To get back to the default vertex array and its associated state the client should bind to vertex array named 0.</p>
 *
 * <p>Once a client is done using a vertex array object it can be deleted with DeleteVertexArraysAPPLE.
 * The client is responsible for allocating and deallocating the memory used by the vertex array data,
 * while the DeleteVertexArraysAPPLE command deletes vertex array object names and associated state only.</p>
 */
public class APPLEVertexArrayObject {
	private static final long bindVertexArray = GL.getFunctionProvider().getFunctionAddress("glBindVertexArrayAPPLE");
	private static final long deleteVertexArrays = GL.getFunctionProvider().getFunctionAddress("glDeleteVertexArraysAPPLE");
	private static final long genVertexArrays = GL.getFunctionProvider().getFunctionAddress("glGenVertexArraysAPPLE");
	private static final long isVertexArray = GL.getFunctionProvider().getFunctionAddress("glIsVertexArrayAPPLE");

	public static final boolean supported = bindVertexArray != 0;

	public static void glBindVertexArrayAPPLE(@NativeType("GLuint") int array) {
		JNI.callV(array, bindVertexArray);
	}

	public static void nglDeleteVertexArraysAPPLE(int n, long arrays) {
		JNI.callPV(n, arrays, deleteVertexArrays);
	}

	public static void glDeleteVertexArraysAPPLE(@NativeType("GLuint const *") IntBuffer arrays) {
		nglDeleteVertexArraysAPPLE(arrays.remaining(), memAddress(arrays));
	}

	public static void glDeleteVertexArraysAPPLE(@NativeType("GLuint const *") int array) {
		MemoryStack stack = stackGet(); int stackPointer = stack.getPointer();
		try {
			IntBuffer arrays = stack.callocInt(1);
			arrays.put(0, array);
			nglDeleteVertexArraysAPPLE(1, memAddress(arrays));
		} finally {
			stack.setPointer(stackPointer);
		}
	}

	public static void nglGenVertexArraysAPPLE(int n, long arrays) {
		JNI.callPV(n, arrays, genVertexArrays);
	}

	public static void glGenVertexArraysAPPLE(@NativeType("GLuint *") IntBuffer arrays) {
		nglGenVertexArraysAPPLE(arrays.remaining(), memAddress(arrays));
	}

	public static int glGenVertexArraysAPPLE() {
		MemoryStack stack = stackGet(); int stackPointer = stack.getPointer();
		try {
			IntBuffer arrays = stack.callocInt(1);
			nglGenVertexArraysAPPLE(1, memAddress(arrays));
			return arrays.get(0);
		} finally {
			stack.setPointer(stackPointer);
		}
	}

	public static boolean glIsVertexArrayAPPLE(@NativeType("GLuint") int array) {
		return JNI.callZ(array, isVertexArray);
	}
}
