package net.countercraft.movecraft.repair.commands;

import net.countercraft.movecraft.util.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.repair.types.Repair;

public class RepairCancelCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Repair - Must Be Player")));
            return true;
        }

        if (!player.hasPermission("movecraft.repair.repaircancel")) {
            player.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Insufficient Permissions")));
            return true;
        }

        // Find player's newest repair
        Repair newest = null;
        for (Repair repair : MovecraftRepair.getInstance().getRepairManager().get()) {
            if (repair.getPlayerUUID() != player.getUniqueId())
                continue;

            if (newest == null || repair.getStart() < newest.getStart()) {
                newest = repair;
            }
        }

        // No repairs
        if (newest == null) {
            player.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Repair - No Repairs")));
            return true;
        }

        // Cancel
        MovecraftRepair.getInstance().getRepairManager().cancel(newest);
        player.sendMessage(I18nSupport.getInternationalisedComponent("Repair - Repair cancelled"));

        return true;
    }
}
