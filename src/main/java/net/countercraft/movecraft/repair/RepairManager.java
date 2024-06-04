package net.countercraft.movecraft.repair;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.events.RepairCancelledEvent;
import net.countercraft.movecraft.repair.events.RepairFinishedEvent;
import net.countercraft.movecraft.repair.events.RepairStartedEvent;
import net.countercraft.movecraft.repair.types.Repair;

public class RepairManager extends BukkitRunnable {
    private final Queue<Repair> repairs = new ConcurrentLinkedQueue<>();

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        Set<Repair> completed = new HashSet<>();
        Set<Repair> executed = new HashSet<>();
        while (System.currentTimeMillis() - start < Config.RepairMaxTickTime) {
            Repair repair = repairs.peek();
            if (repair == null)
                break; // No repairs, jump out

            if (repair.run()) {
                // Repair placed at least a block, return to back of queue
                executed.add(repairs.poll());
            } // Else leave at top of queue

            if (repair.isDone())
                completed.add(repair);
        }
        repairs.addAll(executed);

        for (Repair repair : completed) {
            end(repair);
        }
    }

    public Set<Repair> get() {
        return new HashSet<>(repairs);
    }

    public void start(Repair repair) {
        Bukkit.getPluginManager().callEvent(new RepairStartedEvent(repair));

        MovecraftRepair.getInstance().getLogger().info(() -> String.format("%s has begun repair %s with the cost of %.2f", repair.getPlayerUUID(), repair.getName(), repair.getCost()));
        repairs.add(repair);
    }

    public void cancel(Repair repair) {
        Bukkit.getPluginManager().callEvent(new RepairCancelledEvent(repair));

        MovecraftRepair.getInstance().getLogger().info(() -> String.format("%s has cancelled repair %s with the cost of %.2f", repair.getPlayerUUID(), repair.getName(), repair.getCost()));
        repairs.remove(repair);
    }

    private void end(Repair repair) {
        Bukkit.getPluginManager().callEvent(new RepairFinishedEvent(repair));

        MovecraftRepair.getInstance().getLogger().info(() -> String.format("%s has completed repair %s with the cost of %.2f", repair.getPlayerUUID(), repair.getName(), repair.getCost()));
        repairs.remove(repair);
    }
}
