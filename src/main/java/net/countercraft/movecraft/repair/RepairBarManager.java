package net.countercraft.movecraft.repair;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.repair.events.RepairFinishedEvent;
import net.countercraft.movecraft.repair.events.RepairStartedEvent;
import net.countercraft.movecraft.repair.types.Repair;

public class RepairBarManager extends BukkitRunnable implements Listener {
    private Map<Repair, BossBar> bossBars = new HashMap<>();

    @Override
    public void run() {
        for (Map.Entry<Repair, BossBar> entry : bossBars.entrySet()) {
            Repair repair = entry.getKey();
            BossBar bossBar = entry.getValue();

            Player player = Bukkit.getPlayer(repair.getPlayerUUID());
            if (player != null) {
                bossBar.addPlayer(player);                
            }

            double percentDone = (100.0 * repair.remaining()) / repair.size();
            bossBar.setProgress(percentDone);
        }
    }

    @EventHandler
    public void onRepairStart(@NotNull RepairStartedEvent event) {
        Repair repair = event.getRepair();

        BossBar bossBar = Bukkit.createBossBar(null, BarColor.WHITE, BarStyle.SOLID);
        bossBars.put(repair, bossBar);

        Player player = Bukkit.getPlayer(repair.getPlayerUUID());
        if (player != null) {
            bossBar.addPlayer(player);                
        }
    }

    @EventHandler
    public void onRepairFinished(@NotNull RepairFinishedEvent event) {
        Repair repair = event.getRepair();
        BossBar bossBar = bossBars.get(repair);

        bossBars.remove(repair);
        bossBar.setVisible(false);

        MovecraftRepair.getInstance().getLogger().info("Repair " + repair.getName() + " for " + repair.getPlayerUUID() + " complete");
    }
}
