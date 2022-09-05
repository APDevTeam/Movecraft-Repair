package net.countercraft.movecraft.repair.types;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InventoryRepair extends RepairTask {
    @NotNull
    private Location location;
    @NotNull
    private Material item;
    private int count;

    public InventoryRepair(Location location, Material item, int count) {
        this.location = location;
        this.item = item;
        this.count = count;
    }

    @Override
    public void execute() {
        Block block = location.getBlock();
        if (!(block.getState() instanceof BlockInventoryHolder)) {
            done = true;
            return;
        }

        BlockInventoryHolder inventoryHolder = (BlockInventoryHolder) block.getState();
        inventoryHolder.getInventory().addItem(new ItemStack(item, count));
        done = true;
    }
}
