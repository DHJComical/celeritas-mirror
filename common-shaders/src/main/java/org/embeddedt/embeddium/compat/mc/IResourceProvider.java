package org.embeddedt.embeddium.compat.mc;

import java.util.Optional;

public interface IResourceProvider {
    Optional<IResource> getResource(IResourceLocation location);
}
