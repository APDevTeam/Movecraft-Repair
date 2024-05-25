package net.countercraft.movecraft.repair.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.repair.types.Repair;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class RepairCancelCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(
                    MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Repair - Must Be Player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("movecraft.repair.repaircancel")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + net.countercraft.movecraft.localisation.I18nSupport
                    .getInternationalisedString("Insufficient Permissions"));
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
            player.sendMessage(
                    MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Repair - No Repairs"));
            return true;
        }

        // Cancel
        MovecraftRepair.getInstance().getRepairManager().cancel(newest);
        player.sendMessage(I18nSupport.getInternationalisedString("Repair - Repair cancelled"));

        return true;
    }
}
