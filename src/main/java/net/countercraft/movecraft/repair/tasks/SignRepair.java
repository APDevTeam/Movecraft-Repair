package net.countercraft.movecraft.repair.tasks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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
        if (!(block.getState() instanceof Sign)) {
            done = true;
            return;
        }

        Sign sign = (Sign) block.getState();
        sign.setLine(0, lines[0]);
        sign.setLine(1, lines[1]);
        sign.setLine(2, lines[2]);
        sign.setLine(3, lines[3]);
        sign.update();

        done = true;
    }

    @Override
    public int getPriority() {
        return 1000;
    }
}
