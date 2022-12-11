package net.countercraft.movecraft.repair.tasks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public class SignRepair extends RepairTask {
    @NotNull
    private Location location;
    @NotNull
    String[] lines;

    public SignRepair(Location location, String[] lines) {
        this.location = location;
        this.lines = lines;
    }

    @Override
    public void execute() {
        Block block = location.getBlock();
        done = true;
    }

    @Override
    public int getPriority() {
        return 1000;
    }
}
