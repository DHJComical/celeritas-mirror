package org.embeddedt.embeddium.impl;

import org.embeddedt.embeddium.impl.compatibility.checks.EarlyDriverScanner;
import org.embeddedt.embeddium.impl.compatibility.environment.probe.GraphicsAdapterProbe;
import org.embeddedt.embeddium.impl.compatibility.workarounds.Workarounds;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;

public class SodiumPreLaunch {
    public static void onPreLaunch() {
        if(EarlyLoaderServices.INSTANCE.getDistribution().isClient()) {
            GraphicsAdapterProbe.findAdapters();
            EarlyDriverScanner.scanDrivers();
            Workarounds.init();
        }
    }
}