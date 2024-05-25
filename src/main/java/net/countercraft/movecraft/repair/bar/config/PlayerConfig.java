package net.countercraft.movecraft.repair.bar.config;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerConfig {
    @Nullable
    private UUID owner;
    private boolean barSetting = true;

    public PlayerConfig() {
    }

    public PlayerConfig(UUID owner) {
        this.owner = owner;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public boolean getBarSetting() {
        return barSetting;
    }

    public void toggleBarSetting() {
        barSetting = !barSetting;
    }
}
