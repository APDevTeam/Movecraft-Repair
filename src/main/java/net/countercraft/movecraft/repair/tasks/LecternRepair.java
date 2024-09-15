package net.countercraft.movecraft.repair.tasks;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Location;

public class LecternRepair extends RepairTask {
    private final BaseBlock block;

    public LecternRepair(Location location, BaseBlock block) {
        super(location);
        this.block = block;
    }

    @Override
    public void execute() {
        try {
            BukkitAdapter.adapt(location.getWorld()).setBlock(BukkitAdapter.asBlockVector(location), block, SideEffectSet.none());
        } catch (WorldEditException e) {
            throw new RuntimeException("Unable to set block", e);
        }
        done = true;
    }

    @Override
    public int getPriority() {
        return -10000;
    }
}
