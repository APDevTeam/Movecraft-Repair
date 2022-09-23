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
        return protoRepairs.get(player);
    }

    @Override
    public void run() {
        Set<UUID> toRemove = new HashSet<>();
        for (Map.Entry<UUID, ProtoRepair> entry : protoRepairs.entrySet()) {
            if (entry.getValue().isExpired()) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID uuid : toRemove) {
            protoRepairs.remove(uuid);
        }
    }
}
