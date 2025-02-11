package net.countercraft.movecraft.repair.types;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.RepairBlobManager;
import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.events.RepairPreStartedEvent;
import net.countercraft.movecraft.repair.types.blobs.RepairBlob;
import net.countercraft.movecraft.repair.util.RotationUtils;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;

public class ProtoRepair {
    private final UUID playerUUID;
    private final String name;
    private final RepairQueue queue;
    private final RepairCounter materials;
    private final int damagedBlockCount;
    private final MovecraftLocation origin;
    private final BlockFace rotation;
    private final World world;
    private final HitBox hitBox;
    private final long calculationTime;
    private boolean valid;

    public ProtoRepair(UUID playerUUID, String name, RepairQueue queue, RepairCounter materials, int damagedBlockCount,
            MovecraftLocation origin, BlockFace rotation, World world, HitBox hitBox) {
        this.playerUUID = playerUUID;
        this.name = name;
        this.queue = queue;
        this.materials = materials;
        this.origin = origin;
        this.rotation = rotation;
        this.world = world;
        this.hitBox = hitBox;
        this.damagedBlockCount = damagedBlockCount;
        calculationTime = System.currentTimeMillis();
        valid = true;
    }

    public UUID playerUUID() {
        return playerUUID;
    }

    public RepairQueue getQueue() {
        return queue;
    }

    public RepairCounter getMaterials() {
        return materials;
    }

    public int getDamagedBlockCount() {
        return damagedBlockCount;
    }

    public MovecraftLocation getOrigin() {
        return origin;
    }

    public BlockFace getRotation() {
        return rotation;
    }

    public World getWorld() {
        return world;
    }

    public HitBox getHitBox() {
        return hitBox;
    }

    public boolean isInvalid() {
        return (System.currentTimeMillis() - calculationTime > 5000) || !valid; // 5 seconds
    }

    @NotNull
    public Repair execute(@NotNull Craft craft, Sign sign)
            throws ProtoRepairExpiredException, ProtoRepairLocationException, ItemRemovalException,
            NotEnoughItemsException, NotEnoughMoneyException {
        if (isInvalid())
            throw new ProtoRepairExpiredException(); // Check for expired
        if (!origin.equals(MathUtils.bukkit2MovecraftLoc(sign.getLocation())))
            throw new ProtoRepairLocationException(); // Check for origin
        if (!rotation.equals(RotationUtils.getRotation(sign.getBlockData())))
            throw new ProtoRepairLocationException(); // Check for rotation

        // Check for balance
        double cost = 0;
        if (MovecraftRepair.getInstance().getEconomy() != null && Config.RepairMoneyPerBlock != 0) {
            cost = queue.size() * Config.RepairMoneyPerBlock;
            if (!MovecraftRepair.getInstance().getEconomy().has(Bukkit.getOfflinePlayer(playerUUID), cost))
                throw new NotEnoughMoneyException();
        }

        // Check materials
        Pair<RepairCounter, Map<MovecraftLocation, Counter<Material>>> pair = checkMaterials(craft);

        // Make sure we have enough
        RepairCounter remaining = pair.getLeft();
        if (!remaining.isEmpty())
            throw new NotEnoughItemsException(remaining);

        Repair repair = new Repair(playerUUID, name, cost, queue);
        RepairPreStartedEvent event = new RepairPreStartedEvent(repair);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            throw new CancelledException();

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

        // Take money
        if (MovecraftRepair.getInstance().getEconomy() != null && cost != 0)
            MovecraftRepair.getInstance().getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(playerUUID), cost);

        valid = false;
        return repair;
    }

    public Pair<RepairCounter, Map<MovecraftLocation, Counter<Material>>> checkMaterials(@NotNull Craft craft) {
        RepairCounter remaining = new RepairCounter(materials);
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
                RepairBlob blob = RepairBlobManager.get(m);
                if (!remaining.getKeySet().contains(blob))
                    continue;

                double remainingCount = remaining.get(blob);
                int contentsCount = contents.get(m);
                if (contentsCount >= remainingCount) {
                    // Enough items found, clear the material from remaining
                    remaining.clear(blob);
                    toRemove.add(m, (int) Math.ceil(remainingCount));
                } else {
                    // Not enough items found, subtract what we have
                    remaining.set(blob, remainingCount - contentsCount);
                    toRemove.add(m, contentsCount);
                }
            }
            itemsToRemove.put(location, toRemove);
        }
        return new Pair<>(remaining, itemsToRemove);
    }

    @NotNull
    private Counter<Material> sumInventory(Inventory inventory) {
        Counter<Material> result = new Counter<>();
        if (inventory instanceof DoubleChestInventory)
            return result; // Don't take from double chests
        for (ItemStack item : inventory.getContents()) {
            if (item == null)
                continue;

            result.add(item.getType(), item.getAmount());
        }
        return result;
    }

    private void removeInventory(@NotNull Inventory inventory, Counter<Material> remaining) throws ItemRemovalException {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null)
                continue;

            Material m = stack.getType();
            if (!remaining.getKeySet().contains(m))
                continue;

            int remainingCount = remaining.get(m);
            int currentCount = stack.getAmount();
            if (currentCount >= remainingCount) {
                // Enough items found, clear the material from remaining
                remaining.clear(m);
                stack.setAmount(currentCount - remainingCount);
                inventory.setItem(i, stack);
            } else {
                // Not enough items found, subtract what we have
                remaining.set(m, remainingCount - currentCount);
                inventory.setItem(i, null);
            }
        }
        if (!remaining.isEmpty())
            throw new ItemRemovalException();
    }

    public static class NotEnoughItemsException extends IllegalStateException {
        private final transient RepairCounter remaining;

        public NotEnoughItemsException(RepairCounter remaining) {
            this.remaining = remaining;
        }

        public RepairCounter getRemaining() {
            return remaining;
        }
    }

    public static class NotEnoughMoneyException extends IllegalStateException {
    }

    public static class ItemRemovalException extends IllegalStateException {
    }

    public static class ProtoRepairExpiredException extends IllegalStateException {
    }

    public static class ProtoRepairLocationException extends IllegalStateException {
    }

    public static class CancelledException extends IllegalStateException {
    }
}
