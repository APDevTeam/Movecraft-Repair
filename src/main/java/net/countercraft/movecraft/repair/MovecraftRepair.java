package net.countercraft.movecraft.repair;

import java.io.File;
import java.util.List;
import java.util.EnumSet;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

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

    private WorldEditPlugin worldEditPlugin = null;
    private Economy economy = null;
    private RepairManager repairManager = null;
    private ProtoRepairCache protoRepairCache = null;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        String[] languages = { "en" };
        for (String s : languages) {
            if (!new File(getDataFolder() + "/localisation/mc-repairlang_" + s + ".properties").exists()) {
                saveResource("localisation/mc-repairlang_" + s + ".properties", false);
            }
        }

        // Load up Vault and WorldEdit if they are present
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                getLogger().info("Found a compatible Vault plugin. Enabling Vault integration.");
            } else {
                getLogger().severe("Movecraft-Repair did not find a compatible Vault plugin. Disabling Vault integration.");
                economy = null;
                return;
            }
        } else {
            getLogger().severe("Movecraft-Repair did not find a compatible Vault plugin. Disabling Vault integration.");
            economy = null;
            return;
        }
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (!(plugin instanceof WorldEditPlugin)) {
            getLogger().severe(
                    "Movecraft-Repair did not find a compatible version of WorldEdit. Disabling WorldEdit integration.");
            return;
        }
        getLogger().info("Found a compatible version of WorldEdit. Enabling WorldEdit integration.");
        worldEditPlugin = (WorldEditPlugin) plugin;

        loadConfig(getConfig());

        // Startup repair manager (every tick)
        repairManager = new RepairManager();
        repairManager.runTaskTimer(this, 0, 1);

        // Startup proto repair (every 10 seconds)
        protoRepairCache = new ProtoRepairCache();
        protoRepairCache.runTaskTimerAsynchronously(this, 10, 200);

        getServer().getPluginManager().registerEvents(new RepairSign(), this);
    }

    private static void loadConfig(FileConfiguration config) {
        // TODO: Simplify loading
        Config.Debug = config.getBoolean("Debug", false);
        Config.Locale = config.getString("Locale", "en");
        I18nSupport.init();
        Config.RepairTicksPerBlock = config.getInt("RepairTicksPerBlock", 0);
        Config.RepairMaxTickTime = config.getLong("RepairMaxTickTime", 5000000);
        Config.RepairMaxBlocksPerTick = config.getInt("RepairMaxBlocksPerTick", 2);
        Config.RepairMoneyPerBlock = config.getDouble("RepairMoneyPerBlock", 0.0);
        Config.RepairMaxPercent = config.getDouble("RepairMaxPercent", 50);
        Config.RepairTool = Material.valueOf(config.getString("RepairTool", "firework_rocket").toUpperCase());
        Object entry = config.get("RepairBlobs");
        if (!(entry instanceof List)) {
            throw new InvalidValueException("RepairBlobs must be a list.");
        }
        for (Object object : (List<?>) entry) {
            EnumSet<Material> result = EnumSet.noneOf(Material.class);
            if (object instanceof List) {
                // Handle an array of materials and/or tags
                for (Object o : (List<?>) object) {
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
        entry = config.get("RepairDispenserItems");
        if (!(entry instanceof List)) {
            throw new InvalidValueException("RepairDispenserItems must be a list.");
        }
        for (Object object : (List<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairDispenserItems.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairDispenserItems entries must be a material name.");
            }
        }
        entry = config.get("RepairFurnaceItems");
        if (!(entry instanceof List)) {
            throw new InvalidValueException("RepairFurnaceItems must be a list.");
        }
        for (Object object : (List<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairFurnaceItems.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairFurnaceItems entries must be a material name.");
            }
        }
        entry = config.get("RepairDropperItems");
        if (!(entry instanceof List)) {
            throw new InvalidValueException("RepairDropperItems must be a list.");
        }
        for (Object object : (List<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairDropperItems.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairDropperItems entries must be a material name.");
            }
        }
        entry = config.get("RepairFirstPass");
        if (!(entry instanceof List)) {
            throw new InvalidValueException("RepairFirstPass must be a list.");
        }
        for (Object object : (List<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairFirstPass.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairFirstPass entries must be a material name.");
            }
        }
        entry = config.get("RepairLastPass");
        if (!(entry instanceof List)) {
            throw new InvalidValueException("RepairLastPass must be a list.");
        }
        for (Object object : (List<?>) entry) {
            if (object instanceof String) {
                // Handle a single material or tag
                Config.RepairLastPass.addAll(Tags.parseMaterials((String) object));
            } else {
                throw new InvalidValueException("RepairLastPass entries must be a material name.");
            }
        }
    }

    public WorldEditPlugin getWorldEditPlugin() {
        return worldEditPlugin;
    }

    public Economy getEconomy() {
        return economy;
    }

    @Nullable
    public RepairManager getRepairManager() {
        return repairManager;
    }

    @Nullable
    public ProtoRepairCache getProtoRepairCache() {
        return protoRepairCache;
    }
}
