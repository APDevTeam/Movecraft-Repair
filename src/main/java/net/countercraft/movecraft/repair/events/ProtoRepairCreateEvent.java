package net.countercraft.movecraft.repair.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.countercraft.movecraft.repair.types.ProtoRepair;

public class ProtoRepairCreateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled = false;
    private final ProtoRepair protoRepair;

    public ProtoRepairCreateEvent(ProtoRepair protoRepair) {
        this.protoRepair = protoRepair;
    }

    public ProtoRepair getProtoRepair() {
        return protoRepair;
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
