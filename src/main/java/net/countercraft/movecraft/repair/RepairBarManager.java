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

import net.countercraft.movecraft.repair.events.RepairCancelledEvent;
import net.countercraft.movecraft.repair.events.RepairFinishedEvent;
import net.countercraft.movecraft.repair.events.RepairStartedEvent;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.repair.types.Repair;

public class RepairBarManager extends BukkitRunnable implements Listener {
    private Map<Repair, BossBar> bossBars = new HashMap<>();

    @Override
    public void run() {
        if (bossBars.get(null) != null) {
            // If we have a null key, remove the bossbar
            BossBar bossBar = bossBars.get(null);
            bossBars.remove(null);
            bossBar.setVisible(false);
            bossBar.removeAll();
        }

        for (Map.Entry<Repair, BossBar> entry : bossBars.entrySet()) {
            Repair repair = entry.getKey();
            BossBar bossBar = entry.getValue();

            Player player = Bukkit.getPlayer(repair.getPlayerUUID());
            if (player != null) {
                bossBar.addPlayer(player);
            }

            int remaining = repair.remaining();
            int total = repair.size();
            bossBar.setTitle(String.format("%s: %d / %d", repair.getName(), total - remaining, total));
            double progress = (double) (total - remaining) / total;
            progress = Math.min(Math.max(progress, 0.0), 1.0);
            bossBar.setProgress(progress);
        }
    }

    @EventHandler
    public void onRepairStart(@NotNull RepairStartedEvent event) {
        Repair repair = event.getRepair();

        BossBar bossBar = Bukkit.createBossBar(repair.getName(), BarColor.WHITE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBars.put(repair, bossBar);

        Player player = Bukkit.getPlayer(repair.getPlayerUUID());
        if (player != null) {
            bossBar.addPlayer(player);
            player.sendMessage(I18nSupport.getInternationalisedString("Repair - Repairs underway"));
        }
    }

    @EventHandler
    public void onRepairFinished(@NotNull RepairFinishedEvent event) {
        remove(event.getRepair());

        Player player = Bukkit.getPlayer(event.getRepair().getPlayerUUID());
        if (player == null)
            return;

        player.sendMessage(I18nSupport.getInternationalisedString("Repair - Repairs complete"));
    }

    @EventHandler
    public void onRepairCancelled(@NotNull RepairCancelledEvent event) {
        remove(event.getRepair());
    }

    private void remove(Repair repair) {
        BossBar bossBar = bossBars.get(repair);

        bossBars.remove(repair);
        bossBar.setVisible(false);
        bossBar.removeAll();
    }
}
