package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.gl.device.GLRenderDevice;
import org.lwjgl.egl.EGL;
import org.lwjgl.egl.EGL10;
import org.lwjgl.egl.EGL12;
import org.lwjgl.egl.EGL14;
import org.lwjgl.egl.EGL15;
import org.lwjgl.egl.EGLCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * Headless EGL context: a real OpenGL context with no window or display server. Uses EGL rather than GLFW
 * so it works on headless machines (CI, SSH). Falls back to Mesa's llvmpipe when no GPU is available.
 *
 * <p>Created once per JVM, left current on the creating thread. Multi-threaded benchmarks would need a
 * context per thread, which is not implemented.
 */
public final class HeadlessGl {
    /** {@code EGL_PLATFORM_SURFACELESS_MESA}, from {@code EGL_MESA_platform_surfaceless}. */
    private static final int EGL_PLATFORM_SURFACELESS_MESA = 0x31DD;

    private static boolean initialized;

    private HeadlessGl() {
    }

    /** Creates the context if this JVM does not have one yet, and makes it current on the calling thread. */
    public static synchronized void ensureContext() {
        if (initialized) {
            return;
        }

        long display = openDisplay();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer major = stack.mallocInt(1);
            IntBuffer minor = stack.mallocInt(1);

            if (!EGL10.eglInitialize(display, major, minor)) {
                throw new IllegalStateException("eglInitialize failed: " + eglError());
            }

            EGLCapabilities caps = EGL.createDisplayCapabilities(display, major.get(0), minor.get(0));

            if (!caps.EGL_KHR_surfaceless_context) {
                throw new IllegalStateException(
                        "EGL_KHR_surfaceless_context is unsupported; cannot create a context without a window");
            }

            if (!EGL12.eglBindAPI(EGL14.EGL_OPENGL_API)) {
                throw new IllegalStateException("eglBindAPI(EGL_OPENGL_API) failed: " + eglError());
            }

            long config = chooseConfig(display, stack);
            long context = createContext(display, config, stack);

            if (!EGL10.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, context)) {
                throw new IllegalStateException("eglMakeCurrent failed: " + eglError());
            }
        }

        GL.createCapabilities();

        // GLRenderDevice requires this; the benchmarks never touch vanilla GL state.
        GLRenderDevice.VANILLA_STATE_RESETTER = () -> {
        };

        initialized = true;
    }

    /** {@return a one-line description of the GL implementation backing the context} */
    public static String describe() {
        ensureContext();

        return GL11C.glGetString(GL11C.GL_RENDERER) + " | " + GL11C.glGetString(GL11C.GL_VERSION);
    }

    private static long openDisplay() {
        // Resolved by hand: LWJGL's EGL.createClientCapabilities() never parses client extensions into its
        // flag set, so all 158 extension flags read false and the LWJGL bindings for platform-display functions
        // fail their null check. Bug present on LWJGL 3.3.1, 3.3.6 and 3.4.0; do not delete this workaround.
        long getPlatformDisplay = EGL.getFunctionProvider().getFunctionAddress("eglGetPlatformDisplayEXT");

        if (getPlatformDisplay != 0L) {
            long display = JNI.invokePPP(EGL_PLATFORM_SURFACELESS_MESA, EGL14.EGL_DEFAULT_DISPLAY, 0L,
                    getPlatformDisplay);

            if (display != EGL10.EGL_NO_DISPLAY) {
                return display;
            }
        }

        // Falls back to the default display (works when a display server is present).
        long display = EGL10.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        if (display == EGL10.EGL_NO_DISPLAY) {
            throw new IllegalStateException("no EGL display available (tried surfaceless and default): " + eglError());
        }

        return display;
    }

    private static long chooseConfig(long display, MemoryStack stack) {
        IntBuffer attribs = stack.ints(
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                EGL12.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_BIT,
                EGL10.EGL_NONE
        );

        var configs = stack.mallocPointer(1);
        IntBuffer count = stack.mallocInt(1);

        if (!EGL10.eglChooseConfig(display, attribs, configs, count) || count.get(0) == 0) {
            throw new IllegalStateException("no EGL config supports an OpenGL context: " + eglError());
        }

        return configs.get(0);
    }

    private static long createContext(long display, long config, MemoryStack stack) {
        // Request 4.6 core; MappedStagingBuffer needs ≥4.4. Falls back to any version below.
        IntBuffer attribs = stack.ints(
                EGL15.EGL_CONTEXT_MAJOR_VERSION, 4,
                EGL15.EGL_CONTEXT_MINOR_VERSION, 6,
                EGL15.EGL_CONTEXT_OPENGL_PROFILE_MASK, EGL15.EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT,
                EGL10.EGL_NONE
        );

        long context = EGL10.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribs);

        if (context == EGL10.EGL_NO_CONTEXT) {
            context = EGL10.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, stack.ints(EGL10.EGL_NONE));
        }

        if (context == EGL10.EGL_NO_CONTEXT) {
            throw new IllegalStateException("eglCreateContext failed: " + eglError());
        }

        return context;
    }

    private static String eglError() {
        return "0x" + Integer.toHexString(EGL10.eglGetError());
    }
}
