package net.countercraft.movecraft.repair.types;

import java.util.UUID;

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.tasks.RepairTask;

public class Repair {
    private UUID playerUUID;
    private String name;
    private RepairQueue queue;
    private int size;
    private long lastExecution;

    public Repair(UUID playerUUID, String name, RepairQueue queue) {
        this.playerUUID = playerUUID;
        this.name = name;
        this.queue = queue;
        this.size = queue.size();
        lastExecution = System.nanoTime();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getName() {
        return name;
    }

    public boolean isDone() {
        return queue.isEmpty();
    }

    public int size() {
        return size;
    }

    public int remaining() {
        return queue.size();
    }

    public boolean run() {
        double elapsedTicks = (System.nanoTime() - lastExecution) * 20.0 / 1000000000;
        int placedBlocks = 0;

        while (elapsedTicks > Config.RepairTicksPerBlock && placedBlocks <= Config.RepairMaxBlocksPerTick) {
            RepairTask task = queue.poll();
            if (task == null)
                break;

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
