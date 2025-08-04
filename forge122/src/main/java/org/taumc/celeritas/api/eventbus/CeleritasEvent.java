package org.taumc.celeritas.api.eventbus;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * The base class which all Celeritas-posted events are derived from.
 * <p>
 * On Forge (1.12.2), this class extends FML's Event class.
 */
public abstract class CeleritasEvent extends Event {
    @Override
    public boolean isCancelable() {
        return false;
    }

}