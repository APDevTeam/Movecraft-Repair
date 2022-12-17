package net.countercraft.movecraft.repair.events;

import org.bukkit.event.HandlerList;

import net.countercraft.movecraft.repair.types.Repair;

public class RepairFinishedEvent extends RepairEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    protected RepairFinishedEvent(Repair repair) {
        super(repair);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
