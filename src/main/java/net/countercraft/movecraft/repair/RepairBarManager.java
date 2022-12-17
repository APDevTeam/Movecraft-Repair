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

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.events.RepairFinishedEvent;
import net.countercraft.movecraft.repair.events.RepairStartedEvent;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
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

            bossBar.setProgress(100.0 * (repair.size() - repair.remaining()) / repair.size());
            int remainingSeconds = (int) Math.ceil(repair.remaining() * Config.RepairTicksPerBlock / 20.0);
            int totalSeconds = (int) Math.ceil(repair.size() * Config.RepairTicksPerBlock / 20.0);
            bossBar.setTitle(String.format("%s: %d / %d", repair.getName(), remainingSeconds, totalSeconds));
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
            player.sendMessage(I18nSupport.getInternationalisedString("Repair - Repairs underway"));
        }
    }

    @EventHandler
    public void onRepairFinished(@NotNull RepairFinishedEvent event) {
        Repair repair = event.getRepair();
        BossBar bossBar = bossBars.get(repair);

        bossBars.remove(repair);
        bossBar.setVisible(false);

        Player player = Bukkit.getPlayer(repair.getPlayerUUID());
        if (player == null)
            return;

        player.sendMessage(I18nSupport.getInternationalisedString("Repair - Repairs complete"));
    }
}
