package net.countercraft.movecraft.repair.tasks;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SignRepair extends RepairTask {
    @Nullable
    private final Component[] frontLines;
    @Nullable
    private final Component[] backLines;

    public SignRepair(Location location, @Nullable Component[] frontLines, @Nullable Component[] backLines) {
        super(location);
        this.frontLines = frontLines;
        this.backLines = backLines;
    }

    @Override
    public void execute() {
        Block block = location.getBlock();
        if (!(block.getState() instanceof Sign sign)) {
            done = true;
            return;
        }

        if (frontLines != null) {
            for (int i = 0; i < frontLines.length; i++) {
                sign.getSide(Side.FRONT).line(i, frontLines[i]);
            }
        }
        if (backLines != null) {
            for (int i = 0; i < backLines.length; i++) {
                sign.getSide(Side.BACK).line(i, backLines[i]);
            }
        }
        sign.update(false, false);
        done = true;
    }

    @Override
    public int getPriority() {
        return 1000;
    }
}
