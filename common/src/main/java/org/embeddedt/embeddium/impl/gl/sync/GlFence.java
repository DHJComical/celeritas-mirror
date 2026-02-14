package org.embeddedt.embeddium.impl.gl.sync;

import org.taumc.celeritas.lwjgl.GL32;
import org.taumc.celeritas.lwjgl.MemoryStack;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;


import java.nio.IntBuffer;

public class GlFence {
    private final long id;
    private boolean disposed;

    public GlFence(long id) {
        this.id = id;
    }

    public boolean isCompleted() {
        this.checkDisposed();

        int result;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.callocInt(1);
            result = LWJGL.glGetSynci(this.id, GL32.GL_SYNC_STATUS, count);

            if (count.get(0) != 1) {
                throw new RuntimeException("glGetSync returned more than one value");
            }
        }

        return result == GL32.GL_SIGNALED;
    }

    public void sync() {
        this.checkDisposed();
        this.sync(Long.MAX_VALUE);
    }

    public void sync(long timeout) {
        this.checkDisposed();
        LWJGL.glWaitSync(this.id, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, timeout);
    }

    public void delete() {
        LWJGL.glDeleteSync(this.id);
        this.disposed = true;
    }

    private void checkDisposed() {
        if (this.disposed) {
            throw new IllegalStateException("Fence object has been disposed");
        }
    }
}
