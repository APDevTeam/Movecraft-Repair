package net.countercraft.movecraft.repair.types;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;

public class ProtoRepair {
    private UUID uuid;
    private RepairQueue queue;
    private Counter<Material> materials;
    private int damagedBlockCount;
    private MovecraftLocation origin;
    private long calculationTime;

    public ProtoRepair(UUID uuid, RepairQueue queue, Counter<Material> materials, int damagedBlockCount, MovecraftLocation origin) {
        this.uuid = uuid;
        this.queue = queue;
        this.materials = materials;
        this.origin = origin;
        this.damagedBlockCount = damagedBlockCount;
        this.calculationTime = System.nanoTime();
    }

    public UUID playerUUID() {
        return uuid;
    }

    public RepairQueue getQueue() {
        return queue;
    }

    public Counter<Material> getMaterials() {
        return materials;
    }

    public int getDamagedBlockCount() {
        return damagedBlockCount;
    }

    public MovecraftLocation getOrigin() {
        return origin;
    }

    public boolean isExpired() {
        return System.nanoTime() - calculationTime > 5000000000L; // 5 seconds
    }

    @Nullable
    public Repair execute(@NotNull Craft craft, Sign sign)
            throws ProtoRepairExpiredException, ProtoRepairLocationException, ItemRemovalException,
            NotEnoughItemsException {
        if (isExpired())
            throw new ProtoRepairExpiredException(); // Check for expired
        if (!origin.equals(MathUtils.bukkit2MovecraftLoc(sign.getLocation())))
            throw new ProtoRepairLocationException(); // Check for origin

        // Check materials
        Pair<Counter<Material>, Map<MovecraftLocation, Counter<Material>>> pair = checkMaterials(craft);

        // Make sure we have enough
        Counter<Material> remaining = pair.getLeft();
        if (remaining.size() > 0)
            throw new NotEnoughItemsException(remaining);

        // Remove materials
        Map<MovecraftLocation, Counter<Material>> itemsToRemove = pair.getRight();
        World world = craft.getWorld();
        for (Map.Entry<MovecraftLocation, Counter<Material>> entry : itemsToRemove.entrySet()) {
            MovecraftLocation location = entry.getKey();
            Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
            if (!Tags.CHESTS.contains(block.getType()))
                throw new ItemRemovalException();

            BlockState state = block.getState();
            if (!(state instanceof Container))
                throw new ItemRemovalException();

            removeInventory(((Container) state).getInventory(), entry.getValue());
        }

        // Start repair
        return new Repair(uuid, queue);
    }

    public Pair<Counter<Material>, Map<MovecraftLocation, Counter<Material>>> checkMaterials(Craft craft) {
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
            Counter<Material> toRemove = new Counter<>();
            for (Material m : contents.getKeySet()) {
                if (!remaining.getKeySet().contains(m))
                    continue;

                int remainingCount = remaining.get(m);
                int currentCount = contents.get(m);
                if (remainingCount >= currentCount) {
                    // Enough items found, clear the material from remaining
                    remaining.clear(m);
                } else {
                    // Not enough items found, subtract what we have
                    remaining.set(m, remainingCount - currentCount);
                }
                toRemove.add(m, remainingCount - currentCount);
            }
            itemsToRemove.put(location, toRemove);
        }
        return new Pair<>(remaining, itemsToRemove);
    }

    private Counter<Material> sumInventory(Inventory inventory) {
        Counter<Material> result = new Counter<>();
        for (ItemStack item : inventory.getContents()) {
            if (item == null)
                continue;

            result.add(item.getType(), item.getAmount());
        }
        return result;
    }

    private void removeInventory(Inventory inventory, Counter<Material> remaining) throws ItemRemovalException {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            Material m = stack.getType();
            if (!remaining.getKeySet().contains(m))
                continue;

            int remainingCount = remaining.get(m);
            int currentCount = stack.getAmount();
            if (remainingCount >= currentCount) {
                // Enough items found, clear the material from remaining
                remaining.clear(m);
                inventory.setItem(i, null);
            } else {
                // Not enough items found, subtract what we have
                remaining.set(m, remainingCount - currentCount);
                stack.setAmount(remainingCount - currentCount);
                inventory.setItem(i, stack);
            }
        }
        if (remaining.size() > 0)
            throw new ItemRemovalException();
    }

    public class NotEnoughItemsException extends IllegalStateException {
        private final transient Counter<Material> remaining;

        public NotEnoughItemsException(Counter<Material> remaining) {
            this.remaining = remaining;
        }

        public Counter<Material> getRemaining() {
            return remaining;
        }
    }

    public class ItemRemovalException extends IllegalStateException {
    }

    public class ProtoRepairExpiredException extends IllegalStateException {
    }

    public class ProtoRepairLocationException extends IllegalStateException {
    }
}
