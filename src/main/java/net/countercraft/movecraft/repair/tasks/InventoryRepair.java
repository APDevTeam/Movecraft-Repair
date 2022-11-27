package net.countercraft.movecraft.repair.tasks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.repair.MovecraftRepair;

public class InventoryRepair extends RepairTask {
    @NotNull
    private Location location;
    @NotNull
    private ItemStack item;

    public InventoryRepair(Location location, ItemStack item) {
        this.location = location;
        this.item = item;
    }

    @Override
    public void execute() {
        MovecraftRepair.getInstance().getLogger().info("Repairing inventory at " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + " with " + item.getAmount() + "x " + item.getType());

        Block block = location.getBlock();
        BlockState state = block.getState();
        if (!(state instanceof Container)) {
            MovecraftRepair.getInstance().getLogger().info("Not a container");
            done = true;
            return;
        }

        Container container = (Container) state;
        addInventory(container.getInventory(), item);
        done = true;
        MovecraftRepair.getInstance().getLogger().info("Done");
    }

    private void addInventory(Inventory inventory, ItemStack item) {
        int remainingCount = item.getAmount();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) {
                // Empty stack, set to the max size
                item.setAmount(item.getType().getMaxStackSize());
                remainingCount -= item.getType().getMaxStackSize();
                MovecraftRepair.getInstance().getLogger().info("Setting" + i + " to " + item.getType().getMaxStackSize());
            }
            else {
                if (stack.getType() != item.getType())
                    continue; // Wrong type

                // Fill stack up to the max
                int currentCount = stack.getAmount();
                int toSetCount = Math.min(currentCount + remainingCount, item.getType().getMaxStackSize());
                item.setAmount(toSetCount);
                remainingCount -= (toSetCount - currentCount);
                MovecraftRepair.getInstance().getLogger().info("Changing " + i + " from " + currentCount + " to " + toSetCount);
            }

            // Update inventory
            inventory.setItem(i, item);

            // Return if we are done
            if (remainingCount == 0)
                return;
        }
    }
}
