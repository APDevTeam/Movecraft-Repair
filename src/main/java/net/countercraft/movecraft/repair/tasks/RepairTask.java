package net.countercraft.movecraft.repair.tasks;

import org.jetbrains.annotations.Nullable;

public class RepairTask {
    protected boolean done = false;
    @Nullable
    private RepairTask dependency = null;

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
