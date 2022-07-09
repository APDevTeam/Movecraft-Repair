package net.countercraft.movecraft.repair;

import java.util.logging.Level;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import net.milkbowl.vault.economy.Economy;

public final class MovecraftRepair extends JavaPlugin {
    private static MovecraftRepair instance;


    public static MovecraftRepair getInstance() {
        return instance;
    }

    private WorldEditPlugin worldEditPlugin;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Load up WorldEdit if it's present
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            getLogger().log(Level.SEVERE, I18nSupport.getInternationalisedString("Startup - WE Not Found"));
            return;
        }
        getLogger().log(Level.INFO, I18nSupport.getInternationalisedString("Startup - WE Found"));
        worldEditPlugin = (WorldEditPlugin) plugin;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public WorldEditPlugin getWorldEditPlugin() {
        return worldEditPlugin;
    }

    public Economy getEconomy() {
        return economy;
    }
}
