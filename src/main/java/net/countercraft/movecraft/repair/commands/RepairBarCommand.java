package net.countercraft.movecraft.repair.commands;

import net.countercraft.movecraft.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.repair.bar.config.PlayerManager;

public class RepairBarCommand implements CommandExecutor {
    @NotNull
    private final PlayerManager manager;

    public RepairBarCommand(@NotNull PlayerManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Repair - Must Be Player")));
            return true;
        }

        if (!player.hasPermission("movecraft.repair.repairbar")) {
            player.sendMessage(ChatUtils.commandPrefix()
                    .append(net.countercraft.movecraft.localisation.I18nSupport.getInternationalisedComponent("Insufficient Permissions")));
            return true;
        }

        manager.toggleBarSetting(player);
        player.sendMessage(ChatUtils.commandPrefix()
                .append(I18nSupport.getInternationalisedComponent("Command - Bar set"))
                .append(Component.text(": "))
                .append(Component.text(manager.getBarSetting(player))));
        return true;
    }
}
