package net.countercraft.movecraft.repair.events;

import org.bukkit.event.Cancellable;

import net.countercraft.movecraft.repair.types.Repair;

public class RepairStartedEvent extends RepairEvent implements Cancellable {
    private boolean isCancelled = false;

    public RepairStartedEvent(Repair repair) {
        super(repair);
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }
}
