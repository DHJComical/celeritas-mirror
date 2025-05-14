package org.embeddedt.embeddium.impl.gl.profiling;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import lombok.Getter;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;

import java.io.Closeable;

public class TimerQueryManager implements Closeable {
    private static final int INVALID_ID = -1;
    /**
     * The number of frames the timer query manager should wait before reading the data from the GPU.
     */
    private static final int QUERY_FRAME_LAG_COUNT = 3;

    private record InFlightQuery(int startTime, int endTime) {
        long getTimeDelta() {
            long startTime = GL33C.glGetQueryObjectui64(this.startTime, GL32C.GL_QUERY_RESULT);
            long endTime = GL33C.glGetQueryObjectui64(this.endTime, GL32C.GL_QUERY_RESULT);
            return endTime - startTime;
        }

        void delete() {
            GL32C.glDeleteQueries(startTime);
            GL32C.glDeleteQueries(endTime);
        }
    }

    private final ObjectArrayFIFOQueue<InFlightQuery> inFlightQueries = new ObjectArrayFIFOQueue<>();
    private int startQueryId = INVALID_ID;

    @Getter
    private long lastTime;

    public void startProfiling() {
        if (startQueryId != INVALID_ID) {
            throw new IllegalStateException("Query already started but not ended");
        }
        int id = GL32C.glGenQueries();
        GL33C.glQueryCounter(id, GL33C.GL_TIMESTAMP);
        startQueryId = id;
    }

    public void finishProfiling() {
        if (startQueryId == INVALID_ID) {
            throw new IllegalStateException("Trying to end query that hasn't started yet");
        }
        int id = GL32C.glGenQueries();
        GL33C.glQueryCounter(id, GL33C.GL_TIMESTAMP);
        inFlightQueries.enqueue(new InFlightQuery(startQueryId, id));
        startQueryId = -1;
    }

    public void updateTime() {
        if (inFlightQueries.size() < QUERY_FRAME_LAG_COUNT) {
            return;
        }
        var query = inFlightQueries.dequeue();
        lastTime = query.getTimeDelta();
        query.delete();
    }

    @Override
    public void close() {
        while (!inFlightQueries.isEmpty()) {
            inFlightQueries.dequeue().delete();
        }
        if (startQueryId != -1) {
            GL32C.glDeleteQueries(startQueryId);
            startQueryId = -1;
        }
    }
}
