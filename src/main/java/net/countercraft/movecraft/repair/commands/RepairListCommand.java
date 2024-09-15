package net.countercraft.movecraft.repair.commands;

import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.repair.util.WEUtils;
import net.countercraft.movecraft.util.ChatUtils;
import net.countercraft.movecraft.util.ComponentPaginator;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class RepairListCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Repair - Must Be Player")));
            return true;
        }

        if (!player.hasPermission("movecraft.repair.repairlist")) {
            player.sendMessage(ChatUtils.commandPrefix().append(net.countercraft.movecraft.localisation.I18nSupport.getInternationalisedComponent("Insufficient Permissions")));
            return true;
        }

        File repairDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(repairDirectory, player.getUniqueId().toString());
        if (!playerDirectory.exists()) {
            player.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Repair - Empty Directory")));
            return true;
        }

        File[] schemList = playerDirectory.listFiles();
        ComponentPaginator paginator = new ComponentPaginator(
                I18nSupport.getInternationalisedComponent("Repair - Saved States"),
                pageNumber -> "/repairlist " + pageNumber);

        int page = 1; // Default page
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.empty()
                        .append(ChatUtils.commandPrefix())
                        .append(net.countercraft.movecraft.localisation.I18nSupport.getInternationalisedComponent("Paginator - Invalid page"))
                        .append(Component.text(" \""))
                        .append(Component.text(args[0]))
                        .append(Component.text("\"")));
                return true;
            }
        }

        if (schemList != null) {
            String primaryExtension = "." + WEUtils.SCHEMATIC_FORMATS.getFirst().getPrimaryFileExtension();
            for (File schemFile : schemList) {
                String name = schemFile.getName();
                if (name.endsWith(primaryExtension))
                    name = name.replace(primaryExtension, "");

                paginator.addLine(Component.text(name));
            }
        }
        if (paginator.isEmpty()) {
            player.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Repair - Empty Directory")));
            return true;
        }
        if (!paginator.isInBounds(page)) {
            sender.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(net.countercraft.movecraft.localisation.I18nSupport.getInternationalisedComponent("Paginator - Invalid page"))
                    .append(Component.text(" \""))
                    .append(Component.text(args[0]))
                    .append(Component.text("\"")));
            return true;
        }
        for (Component line : paginator.getPage(page)) {
            player.sendMessage(line);
        }
        return true;
    }
}
