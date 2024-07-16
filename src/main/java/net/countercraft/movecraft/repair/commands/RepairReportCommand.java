package net.countercraft.movecraft.repair.commands;

import net.countercraft.movecraft.util.ChatUtils;
import net.countercraft.movecraft.util.ComponentPaginator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.repair.types.Repair;

public class RepairReportCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
            @NotNull String[] args) {
        if (!sender.hasPermission("movecraft.repair.repairreport")) {
            sender.sendMessage(ChatUtils.commandPrefix()
                    .append(net.countercraft.movecraft.localisation.I18nSupport.getInternationalisedComponent("Insufficient Permissions")));
            return true;
        }

        ComponentPaginator paginator = new ComponentPaginator(
                I18nSupport.getInternationalisedComponent("Repair - Ongoing Repairs"),
                pageNumber -> "/repairreport " + pageNumber);

        int page = 1; // Default page
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.empty()
                        .append(ChatUtils.commandPrefix())
                        .append(net.countercraft.movecraft.localisation.I18nSupport.getInternationalisedComponent("Paginator - Invalid Page"))
                        .append(Component.text("\""))
                        .append(Component.text(args[0]))
                        .append(Component.text("\"")));
                return true;
            }
        }

        for (Repair repair : MovecraftRepair.getInstance().getRepairManager().get()) {
            Player p = Bukkit.getPlayer(repair.getPlayerUUID());
            paginator.addLine(Component.empty()
                    .append(Component.text(repair.getName()))
                    .append(Component.text(" "))
                    .append(Component.text(p == null ? "None" : p.getName()))
                    .append(Component.text(" @ "))
                    .append(Component.text(repair.remaining()))
                    .append(Component.text(", "))
                    .append(Component.text(repair.size() - repair.remaining()))
                    .append(Component.text(" / "))
                    .append(Component.text(repair.size()))
            );
        }
        if (paginator.isEmpty()) {
            sender.sendMessage(ChatUtils.commandPrefix().append(net.countercraft.movecraft.localisation.I18nSupport.getInternationalisedComponent("Repair - No Repairs")));
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
            sender.sendMessage(line);
        }
        return true;
    }
}
