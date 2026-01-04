package net.countercraft.movecraft.repair;

import java.util.*;
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
        List<Repair> executed = new ArrayList<>(repairs.size());
        List<Repair> waiting = new ArrayList<>(repairs.size());
        long time = System.currentTimeMillis();
        while (time - start < Config.RepairMaxTickTime) {
            Repair repair = repairs.poll();
            if (repair == null)
                break; // No repairs, jump out

            if (repair.run(time)) {
                // Repair placed at least a block, put it back at the end
                executed.add(repair);
            } else {
                // Put back at the top of the queue
                waiting.add(repair);
            }

            if (repair.isDone()) {
                completed.add(repair);
            }

            time = System.currentTimeMillis();
        }
        repairs.addAll(waiting);
        repairs.addAll(executed);

        for (Repair repair : completed) {
            end(repair);
        }
    }

    public void shutdown() {
        long start = System.currentTimeMillis();
        MovecraftRepair.getInstance().getLogger().info(() -> String.format("Completing %d repairs", repairs.size()));

        while (!repairs.isEmpty()) {
            Repair repair = repairs.poll();
            repair.run();

            if (repair.isDone()) {
                end(repair);
            }
            else {
                repairs.add(repair);
            }

            if (System.currentTimeMillis() - start > 5000) {
                MovecraftRepair.getInstance().getLogger().info(() -> String.format("Repair time overrun, %d skipped:", repairs.size()));
                for (Repair skipped : repairs) {
                    MovecraftRepair.getInstance().getLogger().info(() -> String.format("- %s's repair %s with the cost of %.2f", skipped.getPlayerUUID(), skipped.getName(), skipped.getCost()));
                }
                return;
            }
        }

        MovecraftRepair.getInstance().getLogger().info("Repairs completed");
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
