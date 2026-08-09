package org.embeddedt.embeddium.impl.gl.profiling;

import org.taumc.celeritas.lwjgl.GL32;
import org.taumc.celeritas.lwjgl.GL33;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import lombok.Getter;

import java.io.Closeable;

public class TimerQueryManager implements Closeable {
    private static final int INVALID_ID = -1;
    /**
     * The number of frames the timer query manager should wait before reading the data from the GPU.
     */
    private static final int QUERY_FRAME_LAG_COUNT = 3;

    private record InFlightQuery(int id) {
        long getTimeDelta() {
            return LWJGL.glGetQueryObjectui64(this.id, GL32.GL_QUERY_RESULT);
        }

        void delete() {
            releaseQuery(id);
        }
    }

    private final ObjectArrayFIFOQueue<InFlightQuery> inFlightQueries = new ObjectArrayFIFOQueue<>();
    private int startQueryId = INVALID_ID;

    private static final IntArrayFIFOQueue QUERY_POOL = new IntArrayFIFOQueue();

    @Getter
    private long lastTime;

    private static int allocateQuery() {
        if (!QUERY_POOL.isEmpty()) {
            return QUERY_POOL.dequeueInt();
        } else {
            return LWJGL.glGenQueries();
        }
    }

    private static void releaseQuery(int id) {
        QUERY_POOL.enqueue(id);
    }

    public void startProfiling() {
        if (startQueryId != INVALID_ID) {
            throw new IllegalStateException("Query already started but not ended");
        }
        int id = allocateQuery();
        LWJGL.glBeginQuery(GL33.GL_TIME_ELAPSED, id);
        startQueryId = id;
    }

    public void finishProfiling() {
        if (startQueryId == INVALID_ID) {
            throw new IllegalStateException("Trying to end query that hasn't started yet");
        }
        LWJGL.glEndQuery(GL33.GL_TIME_ELAPSED);
        inFlightQueries.enqueue(new InFlightQuery(startQueryId));
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
            LWJGL.glEndQuery(GL33.GL_TIME_ELAPSED);
            releaseQuery(startQueryId);
            startQueryId = -1;
        }
    }
}
