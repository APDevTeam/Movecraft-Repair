package net.countercraft.movecraft.repair.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.repair.bar.config.PlayerManager;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class RepairBarCommand implements CommandExecutor {
    @NotNull
    private final PlayerManager manager;

    public RepairBarCommand(@NotNull PlayerManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(
                    MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Repair - Must Be Player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("movecraft.repair.repairbar")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + net.countercraft.movecraft.localisation.I18nSupport
                    .getInternationalisedString("Insufficient Permissions"));
            return true;
        }

        manager.toggleBarSetting(player);
        player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Command - Bar set") + ": " + manager.getBarSetting(player));
        return true;
    }
}
