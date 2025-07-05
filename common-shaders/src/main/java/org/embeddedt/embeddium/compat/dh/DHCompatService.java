package org.embeddedt.embeddium.compat.dh;

import java.util.ServiceLoader;

public interface DHCompatService {
    DHCompatService DH_COMPAT = ServiceLoader.load(DHCompatService.class).findFirst().orElseThrow();

    default boolean hasRenderingEnabled() {
        return false;
    }

}
