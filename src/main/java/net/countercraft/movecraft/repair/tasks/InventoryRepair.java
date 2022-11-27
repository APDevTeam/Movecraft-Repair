package net.countercraft.movecraft.repair.tasks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
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
        MovecraftRepair.getInstance().getLogger().info("Repairing inventory at " + location);
        MovecraftRepair.getInstance().getLogger().info("with " + item);
        Block block = location.getBlock();
        BlockState state = block.getState();
        if (!(state instanceof Container)) {
            MovecraftRepair.getInstance().getLogger().info("Not a container");
            done = true;
            return;
        }

        Container container = (Container) state;
        container.getInventory().addItem(item);
        container.update(true, false);
        done = true;
        MovecraftRepair.getInstance().getLogger().info("Done");
    }
}
