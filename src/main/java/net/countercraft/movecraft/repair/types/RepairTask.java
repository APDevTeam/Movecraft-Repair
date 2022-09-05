package net.countercraft.movecraft.repair.types;

import org.jetbrains.annotations.Nullable;

public class RepairTask {
    protected boolean done = false;
    @Nullable
    private RepairTask dependency;

    public boolean isDone() {
        return done;
    }

    @Nullable
    public RepairTask getDependency() {
        return dependency;
    }

    public void execute() {
        // Do nothing
    }
}
