package net.countercraft.movecraft.repair.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.repair.types.Repair;

public class RepairEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull protected final Repair repair;

    protected RepairEvent(Repair repair) {
        this.repair = repair;
    }

    @NotNull
    public Repair getRepair() {
        return repair;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
