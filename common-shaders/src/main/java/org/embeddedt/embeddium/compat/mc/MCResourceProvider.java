package org.embeddedt.embeddium.compat.mc;

import java.util.Optional;

public interface MCResourceProvider {
    Optional<MCResource> getResource(MCResourceLocation location);
}
