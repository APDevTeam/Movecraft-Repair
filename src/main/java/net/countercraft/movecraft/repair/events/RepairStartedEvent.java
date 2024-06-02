package net.countercraft.movecraft.repair.events;

import net.countercraft.movecraft.repair.types.Repair;

public class RepairStartedEvent extends RepairEvent {
    public RepairStartedEvent(Repair repair) {
        super(repair);
    }
}
