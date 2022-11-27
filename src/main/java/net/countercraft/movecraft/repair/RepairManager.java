package net.countercraft.movecraft.repair;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.scheduler.BukkitRunnable;
import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.types.Repair;

public class RepairManager extends BukkitRunnable {
    private final Queue<Repair> repairs = new ConcurrentLinkedQueue<>();

    @Override
    public void run() {
        long start = System.nanoTime();

        Set<Repair> completed = new HashSet<>();
        Set<Repair> executed = new HashSet<>();
        while (System.nanoTime() - start < Config.RepairMaxTickTime) {
            Repair repair = repairs.peek();
            if (repair.run()) {
                // Repair placed at least a block, return to back of queue
                executed.add(repairs.poll());
            }
            // Else leave at top of queue

            if (repair.isDone())
                completed.add(repair);
        }
        repairs.removeAll(completed);
    }

    public void add(Repair repair) {
        repairs.add(repair);
    }

    public Set<Repair> get() {
        return new HashSet<>(repairs);
    }
}