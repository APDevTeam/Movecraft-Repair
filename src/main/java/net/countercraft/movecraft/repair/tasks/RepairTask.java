package net.countercraft.movecraft.repair.tasks;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RepairTask {
    protected boolean done = false;
    @NotNull
    protected Location location;
    @Nullable
    private RepairTask dependency = null;

    public RepairTask(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isDone() {
        return done;
    }

    public void setDependency(@Nullable RepairTask dependency) {
        this.dependency = dependency;
    }

    @Nullable
    public RepairTask getDependency() {
        return dependency;
    }

    public void execute() {
        // Do nothing
    }

    public int getPriority() {
        return Integer.MIN_VALUE;
    }
}
