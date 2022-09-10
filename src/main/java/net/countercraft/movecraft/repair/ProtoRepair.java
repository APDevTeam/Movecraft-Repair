package net.countercraft.movecraft.repair;

import java.util.UUID;

import org.bukkit.Material;

import net.countercraft.movecraft.repair.types.RepairQueue;
import net.countercraft.movecraft.util.Counter;

public class ProtoRepair {
    private UUID uuid;
    private RepairQueue queue;
    private Counter<Material> materials;
    private long calculationTime;

    public ProtoRepair(UUID uuid, RepairQueue queue, Counter<Material> materials) {
        this.uuid = uuid;
        this.queue = queue;
        this.materials = materials;
        this.calculationTime = System.nanoTime();
    }

    public boolean isExpired() {
        return System.nanoTime() - calculationTime > 1000000000L;
    }
}
