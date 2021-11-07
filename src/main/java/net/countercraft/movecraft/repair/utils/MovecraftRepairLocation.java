package net.countercraft.movecraft.repair.utils;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.util.Pair;
import org.jetbrains.annotations.NotNull;

public class MovecraftRepairLocation extends Pair<MovecraftLocation, MovecraftLocation> {

    public MovecraftRepairLocation(@NotNull MovecraftLocation offset, @NotNull MovecraftLocation origin) {
        super(offset, origin);
    }

    @NotNull
    public MovecraftLocation getOffset() {
        return getLeft();
    }

    @NotNull
    public MovecraftLocation getOrigin() {
        return getRight();
    }
}
