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
        MovecraftRepair.getInstance().getLogger().info("SignRepair: <" + location + ">: ['" + String.join("'', '", lines) + "']");
        this.lines = lines;
    }

    @Override
    public void execute() {
        MovecraftRepair.getInstance().getLogger().info("Running: <" + location + ">");
        Block block = location.getBlock();
        if (!(block.getState() instanceof Sign)) {
            MovecraftRepair.getInstance().getLogger().info("Not a sign");
            done = true;
            return;
        }

        Sign sign = (Sign) block.getState();
        MovecraftRepair.getInstance().getLogger().info("Before: ['" + String.join("'', '", sign.getLines()) + "']");
        sign.setLine(0, lines[0]);
        sign.setLine(1, lines[1]);
        sign.setLine(2, lines[2]);
        sign.setLine(3, lines[3]);
        sign.update(false, false);
        MovecraftRepair.getInstance().getLogger().info("Mid: ['" + String.join("'', '", sign.getLines()) + "']");
        sign = (Sign) (location.getBlock().getState());
        MovecraftRepair.getInstance().getLogger().info("After: ['" + String.join("'', '", sign.getLines()) + "']");
        done = true;
    }

    @Override
    public int getPriority() {
        return 1000;
    }
}
