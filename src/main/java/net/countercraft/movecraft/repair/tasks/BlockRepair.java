package net.countercraft.movecraft.repair.tasks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.Movecraft;

public class BlockRepair extends RepairTask {
    @NotNull
    private BlockData data;

    public BlockRepair(Location location, BlockData data) {
        super(location);
        this.data = data;
    }

    public Material getMaterial() {
        return data.getMaterial();
    }

    @Override
    public void execute() {
        Movecraft.getInstance().getWorldHandler().setBlockFast(location, data);
        done = true;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
