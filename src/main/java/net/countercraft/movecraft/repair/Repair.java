package net.countercraft.movecraft.repair;

import java.util.UUID;

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.types.RepairQueue;
import net.countercraft.movecraft.repair.types.RepairTask;

public class Repair {
    private UUID uuid;
    private RepairQueue queue;
    private long lastExecution;

    public Repair(UUID uuid, RepairQueue queue) {
        this.uuid = uuid;
        this.queue = queue;
    }

    public boolean isDone() {
        return queue.isEmpty();
    }

    public boolean run() {
        double elapsedTicks = (System.nanoTime() - lastExecution) * 20.0 / 1000000000;
        int placedBlocks = 0;

        while (elapsedTicks > Config.RepairTicksPerBlock && placedBlocks <= Config.RepairMaxBlocksPerTick) {
            RepairTask task = queue.poll();
            task.execute();
            elapsedTicks -= Config.RepairTicksPerBlock;
            placedBlocks++;
        }

        if (placedBlocks > 0) {
            lastExecution = System.nanoTime();
            return true;
        }

        return false;
    }
}
