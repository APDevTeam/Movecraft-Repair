package net.countercraft.movecraft.repair;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import net.countercraft.movecraft.repair.types.ProtoRepair;

public class ProtoRepairCache extends BukkitRunnable {
    private final Map<UUID, ProtoRepair> protoRepairs = new ConcurrentHashMap<>();

    public void add(ProtoRepair protoRepair) {
        protoRepairs.put(protoRepair.playerUUID(), protoRepair);
    }

    @Nullable
    public ProtoRepair get(UUID player) {
        ProtoRepair protoRepair = protoRepairs.get(player);
        if (protoRepair == null)
            return null;

        if (protoRepair.isInvalid()) {
            // Remove expired ones, but return anyways
            protoRepairs.remove(player);
        }

        return protoRepair;
    }

    @Override
    public void run() {
        // Otherwise, remove expired repairs every so often
        Set<UUID> toRemove = new HashSet<>();
        for (Map.Entry<UUID, ProtoRepair> entry : protoRepairs.entrySet()) {
            if (entry.getValue().isInvalid()) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID uuid : toRemove) {
            protoRepairs.remove(uuid);
        }
    }
}
