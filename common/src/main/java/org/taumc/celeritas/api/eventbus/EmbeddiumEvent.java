package org.taumc.celeritas.api.eventbus;

/**
 * The base class which all Embeddium-posted events are derived from.
 * <p></p>
 * On (Neo)Forge, this class will extend their native event class, to allow firing the event to the event bus.
 * <p></p>
 * On Fabric, it extends nothing.
 */
public abstract class EmbeddiumEvent {
    public boolean isCancelable() {
        return false;
    }

    private boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean cancel) {
        canceled = cancel;
    }
}
