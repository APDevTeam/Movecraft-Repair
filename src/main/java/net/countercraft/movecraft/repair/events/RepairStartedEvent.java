package net.countercraft.movecraft.repair.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import net.countercraft.movecraft.repair.types.Repair;

public class RepairStartedEvent extends RepairEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled = false;

    protected RepairStartedEvent(Repair repair) {
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

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
