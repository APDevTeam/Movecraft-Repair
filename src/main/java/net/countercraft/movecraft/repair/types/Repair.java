package net.countercraft.movecraft.repair.types;

import java.util.UUID;

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.tasks.RepairTask;

public class Repair {
    private UUID playerUUID;
    private String name;
    private double cost;
    private RepairQueue queue;
    private int size;
    private long lastExecution;
    private long start;

    public Repair(UUID playerUUID, String name, double cost, RepairQueue queue) {
        this.playerUUID = playerUUID;
        this.name = name;
        this.cost = cost;
        this.queue = queue;
        this.size = queue.size();
        lastExecution = System.currentTimeMillis();
        start = System.currentTimeMillis();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getName() {
        return name;
    }

    public double getCost() {
        return cost;
    }

    public long getStart() {
        return start;
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

    public boolean run(long time) {
        double elapsedTicks = (time - lastExecution) * 20.0 / 1000;
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
            lastExecution = time;
            return true;
        }

        return false;
    }
}
