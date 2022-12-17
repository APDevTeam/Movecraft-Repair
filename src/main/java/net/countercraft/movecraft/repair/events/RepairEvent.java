package net.countercraft.movecraft.repair.events;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.repair.types.Repair;

public abstract class RepairEvent extends Event {
    @NotNull protected final Repair repair;

    protected RepairEvent(Repair repair) {
        this.repair = repair;
    }

    @NotNull
    public Repair getRepair() {
        return repair;
    }
}
