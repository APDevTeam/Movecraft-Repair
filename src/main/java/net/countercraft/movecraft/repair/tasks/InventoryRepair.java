package net.countercraft.movecraft.repair.tasks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InventoryRepair extends RepairTask {
    @NotNull
    private ItemStack item;

    public InventoryRepair(Location location, ItemStack item) {
        super(location);
        this.item = item;
    }

    @Override
    public void execute() {
        Block block = location.getBlock();
        BlockState state = block.getState();
        if (!(state instanceof Container)) {
            done = true;
            return;
        }

        Container container = (Container) state;
        addInventory(container.getInventory(), item);
        done = true;
    }

    private void addInventory(Inventory inventory, ItemStack item) {
        int remainingCount = item.getAmount();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) {
                // Empty stack, set to the max size
                item.setAmount(item.getType().getMaxStackSize());
                remainingCount -= item.getType().getMaxStackSize();
            }
            else {
                if (stack.getType() != item.getType())
                    continue; // Wrong type

                // Fill stack up to the max
                int currentCount = stack.getAmount();
                int toSetCount = Math.min(currentCount + remainingCount, item.getType().getMaxStackSize());
                item.setAmount(toSetCount);
                remainingCount -= (toSetCount - currentCount);
            }

            // Update inventory
            inventory.setItem(i, item);

            // Return if we are done
            if (remainingCount == 0)
                return;
        }
    }

    @Override
    public int getPriority() {
        return -1000;
    }
}
