package org.taumc.celeritas.core;

import com.gtnewhorizons.retrofuturabootstrap.SharedConfig;

public class CeleritasLwjgl3ifyCompat {
    public static void apply() {
        // Hack for now
        var handle = SharedConfig.getRfbTransformers().stream().filter(transformer -> transformer.id().equals("lwjgl3ify:redirect")).findFirst().orElseThrow();
        handle.exclusions().add("org.embeddedt.embeddium");
        handle.exclusions().add("org.taumc.celeritas");
    }
}
