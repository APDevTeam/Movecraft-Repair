package net.countercraft.movecraft.repair;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.repair.repair.RepairManager;
import net.countercraft.movecraft.repair.sign.RepairSign;
import net.countercraft.movecraft.repair.utils.WE6Utils;
import net.countercraft.movecraft.repair.utils.WEUtils;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class MovecraftRepair extends JavaPlugin {
    private static MovecraftRepair instance;


    public static MovecraftRepair getInstance() {
        return instance;
    }

    private static WorldEditPlugin worldEditPlugin;

    private WEUtils weUtils;
    private RepairManager repairManager;

    @Override
    public void onEnable() {
        instance = this;

        //load up WorldEdit if it's present
        Plugin wEPlugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (wEPlugin == null || !(wEPlugin instanceof WorldEditPlugin)) {
            getLogger().log(Level.SEVERE, I18nSupport.getInternationalisedString("Startup - WE Not Found"));
            return;
        }

        getLogger().log(Level.INFO, I18nSupport.getInternationalisedString("Startup - WE Found"));
        Settings.RepairTicksPerBlock = getConfig().getInt("RepairTicksPerBlock", 0);
        Settings.RepairMaxPercent = getConfig().getDouble("RepairMaxPercent", 50);
        worldEditPlugin = (WorldEditPlugin) wEPlugin;

        weUtils = new WE6Utils();

        repairManager = new RepairManager();
        repairManager.runTaskTimerAsynchronously(this, 0, 1);
        repairManager.convertOldCraftRepairStates();

        getServer().getPluginManager().registerEvents(new RepairSign(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public WorldEditPlugin getWorldEditPlugin() {
        return worldEditPlugin;
    }

    public RepairManager getRepairManager() {
        return repairManager;
    }

    public WEUtils getWEUtils() {
        return weUtils;
    }
}
