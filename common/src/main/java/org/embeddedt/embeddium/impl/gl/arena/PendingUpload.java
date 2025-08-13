package org.embeddedt.embeddium.impl.gl.arena;

import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import org.jetbrains.annotations.Nullable;

public class PendingUpload {
    private final NativeBuffer data;
    private GlBufferSegment result;

    private PendingUpload(NativeBuffer data) {
        this.data = data;
    }

    public static @Nullable PendingUpload of(@Nullable NativeBuffer data) {
        if (data == null) {
            return null;
        }
        return new PendingUpload(data);
    }

    public NativeBuffer getDataBuffer() {
        return this.data;
    }

    protected void setResult(GlBufferSegment result) {
        if (this.result != null) {
            throw new IllegalStateException("Result already provided");
        }

        this.result = result;
    }

    public GlBufferSegment getResult() {
        if (this.result == null) {
            throw new IllegalStateException("Result not computed");
        }

        return this.result;
    }

    public int getLength() {
        return this.data.getLength();
    }
}
