package net.countercraft.movecraft.repair.events;

import net.countercraft.movecraft.repair.types.Repair;

public class RepairCancelledEvent extends RepairEvent {
    public RepairCancelledEvent(Repair repair) {
        super(repair);
    }
}
