package org.embeddedt.embeddium.impl.render.frame;

import org.taumc.celeritas.lwjgl.GL32;
import org.taumc.celeritas.lwjgl.GL32;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;

public class RenderAheadManager {
    private final LongArrayFIFOQueue fences = new LongArrayFIFOQueue();

    public void startFrame(int renderAheadLimit) {
        while (this.fences.size() > renderAheadLimit) {
            var fence = this.fences.dequeueLong();
            // We do a ClientWaitSync here instead of a WaitSync to not allow the CPU to get too far ahead of the GPU.
            // This is also needed to make sure that our persistently-mapped staging buffers function correctly, rather
            // than being overwritten by data meant for future frames before the current one has finished rendering on
            // the GPU.
            //
            // Because we use GL_SYNC_FLUSH_COMMANDS_BIT, a flush will be inserted at some point in the command stream
            // (the stream of commands the GPU and/or driver (aka. the "server") is processing).
            // In OpenGL 4.4 contexts and below, the flush will be inserted *right before* the call to ClientWaitSync.
            // In OpenGL 4.5 contexts and above, the flush will be inserted *right after* the call to FenceSync (the
            // creation of the fence).
            // The flush, when the server reaches it in the command stream and processes it, tells the server that it
            // must *finish execution* of all the commands that have already been processed in the command stream,
            // and only after everything before the flush is done is it allowed to start processing and executing
            // commands after the flush.
            // Because we are also waiting on the client for the FenceSync to finish, the flush is effectively treated
            // like a Finish command, where we know that once ClientWaitSync returns, it's likely that everything
            // before it has been completed by the GPU.
            LWJGL.glClientWaitSync(fence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, Long.MAX_VALUE);
            LWJGL.glDeleteSync(fence);
        }
    }

    public void endFrame() {
        var fence = LWJGL.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

        if (fence == 0) {
            throw new RuntimeException("Failed to create fence object");
        }

        this.fences.enqueue(fence);
    }
}
