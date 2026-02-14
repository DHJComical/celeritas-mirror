package org.taumc.celeritas.lwjgl;

/**
 * Loads LWJGLService via ServiceLoader, picking highest priority.
 */
public final class LWJGLServiceProvider {
    public static final LWJGLService LWJGL = createInstance();
    public static final int POINTER_SIZE = LWJGL.getPointerSize();
    public static final long NULL = 0L;

    private LWJGLServiceProvider() {}

    static LWJGLService constructInstance(String className) {
        try {
            var clz = Class.forName(className);
            var method = clz.getDeclaredMethod("create");
            return (LWJGLService)method.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    static LWJGLService createInstance() {
        try {
            Class.forName("org.lwjgl.opengl.GL11C");
            return constructInstance("org.taumc.celeritas.lwjgl.lwjgl3.LWJGL3Service");
        } catch (ClassNotFoundException e) {
            return constructInstance("org.taumc.celeritas.lwjgl.lwjgl2.LWJGL2Service");
        }
    }
}
