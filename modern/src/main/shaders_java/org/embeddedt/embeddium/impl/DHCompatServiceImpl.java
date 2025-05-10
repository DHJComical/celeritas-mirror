package org.embeddedt.embeddium.impl;

import net.irisshaders.iris.compat.dh.DHCompat;
import org.embeddedt.embeddium.compat.dh.DHCompatService;

public class DHCompatServiceImpl implements DHCompatService {
    @Override
    public boolean hasRenderingEnabled() {
        return DHCompat.hasRenderingEnabled();
    }
}
