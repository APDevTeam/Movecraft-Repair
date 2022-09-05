package net.countercraft.movecraft.repair;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.scheduler.BukkitRunnable;

import net.countercraft.movecraft.repair.config.Config;

public class RepairManager extends BukkitRunnable {
    private final Queue<Repair> repairs = new ConcurrentLinkedQueue()<>();

    @Override
    public void run() {
        long start = System.nanoTime();

        Set<Repair> completed = new HashSet<>();
        for (Repair repair : repairs) {
            repair.run();

            if (repair.isDone())
                completed.add(repair);

            if (System.nanoTime() - start > Config.RepairMaxTickTime)
                break; // Limit repair time, preferring older repairs
        }
        repairs.removeAll(completed);
    }

    public void add(Repair repair) {
        repairs.add(repair);
    }

    public Collection<Repair> get() {
        return repairs;
    }
}
