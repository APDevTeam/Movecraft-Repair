package net.countercraft.movecraft.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import net.countercraft.movecraft.craft.type.TypeData.InvalidValueException;
import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.util.Tags;
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
        Config.Debug = getConfig().getBoolean("Debug", false);

        // TODO other languages
        String[] languages = {"en"};
        for (String s : languages) {
            if (!new File(getDataFolder()  + "/localisation/mc-repairlang_"+ s +".properties").exists()) {
                this.saveResource("localisation/mc-repairlang_"+ s +".properties", false);
            }
        }
        Config.Locale = getConfig().getString("Locale", "en");
        I18nSupport.init();

        // Load up WorldEdit if it's present
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            getLogger().log(Level.SEVERE, I18nSupport.getInternationalisedString("Startup - WE Not Found"));
            return;
        }
        getLogger().log(Level.INFO, I18nSupport.getInternationalisedString("Startup - WE Found"));

        Config.RepairTicksPerBlock = getConfig().getInt("RepairTicksPerBlock", 0);
        Config.RepairMaxPercent = getConfig().getDouble("RepairMaxPercent", 50);
        Config.RepairMoneyPerBlock = getConfig().getDouble("RepairMoneyPerBlock", 0.0);
        Config.RepairBlobs = new HashSet<>();
        Object entry = getConfig().get("RepairBlobs");
        if (!(entry instanceof ArrayList)) {
            throw new InvalidValueException("RepairBlobs must be a list.");
        }
        for (Object object : (ArrayList) entry) {
            EnumSet<Material> result = EnumSet.noneOf(Material.class);
            if (object instanceof ArrayList) {
                // Handle an array of materials and/or tags
                for (Object o : (ArrayList) object) {
                    if(!(o instanceof String)) {
                        throw new InvalidValueException("RepairBlobs array entries must be strings.");
                    }
                    result.addAll(Tags.parseMaterials((String) o));
                }
            }
            else if (object instanceof String) {
                // Handle a single material or tag
                result.addAll(Tags.parseMaterials((String) object));
            }
            else {
                throw new InvalidValueException("RepairBlobs entries must be a list or material name.");
            }
            Config.RepairBlobs.add(result);
        }

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
