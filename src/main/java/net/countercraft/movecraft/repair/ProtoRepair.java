package net.countercraft.movecraft.repair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.repair.types.RepairQueue;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;

public class ProtoRepair {
    private UUID uuid;
    private RepairQueue queue;
    private Counter<Material> materials;
    private long calculationTime;

    public ProtoRepair(UUID uuid, RepairQueue queue, Counter<Material> materials) {
        this.uuid = uuid;
        this.queue = queue;
        this.materials = materials;
        this.calculationTime = System.nanoTime();
    }

    public boolean isExpired() {
        return System.nanoTime() - calculationTime > 1000000000L;
    }

    @Nullable
    public Repair execute(@NotNull Craft craft) {
        

        return new Repair(uuid, queue);
    }

    private Pair<Counter<Material>, Map<MovecraftLocation, Counter<Material>>> checkMaterials(Craft craft) {
        Counter<Material> remaining = new Counter<>(materials);
        Map<MovecraftLocation, Counter<Material>> itemsToRemove = new HashMap<>();

        World world = craft.getWorld();
        for (MovecraftLocation location : craft.getHitBox()) {
            Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
            if (!Tags.CHESTS.contains(block.getType()))
                continue;

            BlockState state = block.getState();
            if (!(state instanceof Container))
                continue;

            Counter<Material> contents = sumInventory(((Container) state).getInventory());
        }
    }

    private Counter<Material> sumInventory(Inventory inventory) {
        Counter<Material> result = new Counter<>();
        for (ItemStack items : inventory.getContents()) {
            result.add(items.getType(), items.getAmount());
        }
        return result;
    }
}
