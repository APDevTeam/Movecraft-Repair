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
            Repair repair = repairs.poll();
            if (repair == null) {
                MovecraftRepair.getInstance().getLogger().info("End of queue");
                break; // No repairs, jump out
            }

            MovecraftRepair.getInstance().getLogger().info("Running repair for " + repair.getPlayerUUID());
            if (repair.run()) {
                // Repair placed at least a block, return to back of queue
                executed.add(repair);
                MovecraftRepair.getInstance().getLogger().info("Repair executed");
            }
            // Else leave at top of queue

            if (repair.isDone()) {
                MovecraftRepair.getInstance().getLogger().info("Repair completed");
                completed.add(repair);
            }
        }
        repairs.addAll(executed);
        repairs.removeAll(completed);
    }

    public void add(Repair repair) {
        repairs.add(repair);
    }

    public Set<Repair> get() {
        return new HashSet<>(repairs);
    }
}
