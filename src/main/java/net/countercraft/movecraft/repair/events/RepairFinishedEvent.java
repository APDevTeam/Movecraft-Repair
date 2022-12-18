package net.countercraft.movecraft.repair.events;

import net.countercraft.movecraft.repair.types.Repair;

public class RepairFinishedEvent extends RepairEvent {
    public RepairFinishedEvent(Repair repair) {
        super(repair);
    }
}
