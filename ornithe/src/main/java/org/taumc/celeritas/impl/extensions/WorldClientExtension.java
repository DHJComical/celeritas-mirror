package org.taumc.celeritas.impl.extensions;

import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTracker;

public interface WorldClientExtension {
    ChunkTracker celeritas$getTracker();
}
