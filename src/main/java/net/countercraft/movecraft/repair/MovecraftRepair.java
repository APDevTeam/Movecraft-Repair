package net.countercraft.movecraft.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
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
        String[] languages = { "en" };
        for (String s : languages) {
            if (!new File(getDataFolder() + "/localisation/mc-repairlang_" + s + ".properties").exists()) {
                this.saveResource("localisation/mc-repairlang_" + s + ".properties", false);
            }
        }
        Config.Locale = getConfig().getString("Locale", "en");
        I18nSupport.init();

        // Load up WorldEdit if it's present
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (!(plugin instanceof WorldEditPlugin)) {
            getLogger().log(Level.SEVERE, I18nSupport.getInternationalisedString("Startup - WE Not Found"));
            return;
        }
        getLogger().log(Level.INFO, I18nSupport.getInternationalisedString("Startup - WE Found"));

        Config.RepairTicksPerBlock = getConfig().getInt("RepairTicksPerBlock", 0);
        Config.RepairMaxTickTime = getConfig().getLong("RepairMaxTickTime", 5000000);
        Config.RepairMaxBlocksPerTick = getConfig().getInt("RepairMaxBlocksPerTick", 2);
        Config.RepairMoneyPerBlock = getConfig().getDouble("RepairMoneyPerBlock", 0.0);
        Config.RepairMaxPercent = getConfig().getDouble("RepairMaxPercent", 50);
        Object entry = getConfig().get("RepairBlobs");
        if (!(entry instanceof ArrayList)) {
            throw new InvalidValueException("RepairBlobs must be a list.");
        }
        for (Object object : (ArrayList<?>) entry) {
            EnumSet<Material> result = EnumSet.noneOf(Material.class);
            if (object instanceof ArrayList) {
                // Handle an array of materials and/or tags
                for (Object o : (ArrayList<?>) object) {
                    if (!(o instanceof String)) {
                        throw new InvalidValueException("RepairBlobs array entries must be strings.");
                    }
                    result.addAll(Tags.parseMaterials((String) o));
                }
            } else if (object instanceof String) {
                // Handle a single material or tag
                result.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairBlobs entries must be a list or material name.");
            }
            Config.RepairBlobs.add(result);
        }
        entry = getConfig().get("RepairDispenserItems");
        if (!(entry instanceof ArrayList)) {
            throw new InvalidValueException("RepairDispenserItems must be a list.");
        }
        for (Object object : (ArrayList<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairDispenserItems.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairDispenserItems entries must be a material name.");
            }
        }
        entry = getConfig().get("RepairFurnaceItems");
        if (!(entry instanceof ArrayList)) {
            throw new InvalidValueException("RepairFurnaceItems must be a list.");
        }
        for (Object object : (ArrayList<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairFurnaceItems.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairFurnaceItems entries must be a material name.");
            }
        }
        entry = getConfig().get("RepairDropperItems");
        if (!(entry instanceof ArrayList)) {
            throw new InvalidValueException("RepairDropperItems must be a list.");
        }
        for (Object object : (ArrayList<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairDropperItems.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairDropperItems entries must be a material name.");
            }
        }
        entry = getConfig().get("RepairFirstPass");
        if (!(entry instanceof ArrayList)) {
            throw new InvalidValueException("RepairFirstPass must be a list.");
        }
        for (Object object : (ArrayList<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairFirstPass.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairFirstPass entries must be a material name.");
            }
        }
        entry = getConfig().get("RepairLastPass");
        if (!(entry instanceof ArrayList)) {
            throw new InvalidValueException("RepairLastPass must be a list.");
        }
        for (Object object : (ArrayList<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairLastPass.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairLastPass entries must be a material name.");
            }
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
