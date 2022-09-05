package net.countercraft.movecraft.repair.types;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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
        Block block = location.getBlock();
        BlockState state = block.getState();
        if (!(state instanceof Container)) {
            done = true;
            return;
        }

        Container container = (Container) state;
        container.getInventory().addItem(item);
        container.update(true, false);
        done = true;
    }
}
