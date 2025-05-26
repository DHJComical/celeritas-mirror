package org.embeddedt.embeddium.compat.mc;

import java.util.Optional;

public interface MCResourceManager {
    Optional<MCResource> getResource(MCResourceLocation location);
}
