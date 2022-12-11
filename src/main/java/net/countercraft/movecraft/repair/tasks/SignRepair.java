package net.countercraft.movecraft.repair.tasks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.repair.MovecraftRepair;

public class SignRepair extends RepairTask {
    @NotNull
    String[] lines;

    public SignRepair(Location location, String[] lines) {
        super(location);
        this.lines = lines;
    }

    @Override
    public void execute() {
        Block block = location.getBlock();
        if (!(block.getState() instanceof Sign)) {
            done = true;
            MovecraftRepair.getInstance().getLogger().info("Block at <" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + "> is not a sign");
            return;
        }

        Sign sign = (Sign) block.getState();
        if (sign == null) {
            done = true;
            MovecraftRepair.getInstance().getLogger().info("Block at <" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + "> is not a sign state");
            return;
        }
        MovecraftRepair.getInstance().getLogger().info("Repairing sign at <" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + "> with " + lines);
        sign.setLine(0, lines[0]);
        sign.setLine(1, lines[1]);
        sign.setLine(2, lines[2]);
        sign.setLine(3, lines[3]);
        sign.update();

        done = true;
        MovecraftRepair.getInstance().getLogger().info("Done");
    }

    @Override
    public int getPriority() {
        return 1000;
    }
}
