package org.embeddedt.embeddium.impl;

//? if >=1.18 {
import org.embeddedt.embeddium.impl.compatibility.checks.EarlyDriverScanner;
import org.embeddedt.embeddium.impl.compatibility.environment.probe.GraphicsAdapterProbe;
import org.embeddedt.embeddium.impl.compatibility.workarounds.Workarounds;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;

public class SodiumPreLaunch {
    public static void onPreLaunch() {
        if(EarlyLoaderServices.INSTANCE.getDistribution().isClient()) {
            //? if >=1.18
            GraphicsAdapterProbe.findAdapters();
            EarlyDriverScanner.scanDrivers();
            Workarounds.init();
        }
    }
}
//?} else {
/*public class SodiumPreLaunch {
    public static void onPreLaunch() {

    }
}
*///?}