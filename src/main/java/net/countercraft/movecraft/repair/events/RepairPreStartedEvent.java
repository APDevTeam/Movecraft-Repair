package net.countercraft.movecraft.repair.events;

import org.bukkit.event.Cancellable;

import net.countercraft.movecraft.repair.types.Repair;

public class RepairPreStartedEvent extends RepairEvent implements Cancellable {
    private boolean cancelled = false;

    public RepairPreStartedEvent(Repair repair) {
        super(repair);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}
