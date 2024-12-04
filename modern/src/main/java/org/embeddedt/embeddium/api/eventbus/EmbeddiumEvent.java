package org.embeddedt.embeddium.api.eventbus;

//? if forge
import net.minecraftforge.eventbus.api.Event;
//? if neoforge {
/*import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
*///?}

/**
 * The base class which all Embeddium-posted events are derived from.
 * <p></p>
 * On (Neo)Forge, this class will extend their native event class, to allow firing the event to the event bus.
 * <p></p>
 * On Fabric, it extends nothing.
 */
public abstract class EmbeddiumEvent /*? if forgelike {*/ extends Event /*?}*/ /*? if neoforge {*/ /*implements ICancellableEvent *//*?}*/ {
    /**
     * Subclasses must override and return true if they want the event to be canceled.
     */
    //? if forge
    @Override
    public boolean isCancelable() {
        return false;
    }

    //? if forgelike {
    @Override
    public boolean isCanceled() {
        return
                //? if neoforge
                /*ICancellableEvent.*/
                super.isCanceled();
    }

    @Override
    public void setCanceled(boolean cancel) {
        //? if neoforge
        /*ICancellableEvent.*/
        super.setCanceled(cancel);
    }
    //?} else {
    /*private boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean cancel) {
        canceled = cancel;
    }
    *///?}
}
