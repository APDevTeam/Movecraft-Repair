package net.countercraft.movecraft.repair.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.repair.types.Repair;
import net.countercraft.movecraft.util.TopicPaginator;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class RepairReportCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
            @NotNull String[] args) {
        if (!sender.hasPermission("movecraft.repair.repairreport")) {
            sender.sendMessage(MOVECRAFT_COMMAND_PREFIX + net.countercraft.movecraft.localisation.I18nSupport
                    .getInternationalisedString("Insufficient Permissions"));
            return true;
        }

        TopicPaginator paginator = new TopicPaginator(
                I18nSupport.getInternationalisedString("Repair - Ongoing Repairs"));

        int page = 1; // Default page
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(MOVECRAFT_COMMAND_PREFIX + net.countercraft.movecraft.localisation.I18nSupport
                        .getInternationalisedString("Paginator - Invalid Page") + " \"" + args[0] + "\"");
                return true;
            }
        }

        for (Repair repair : MovecraftRepair.getInstance().getRepairManager().get()) {
            Player p = Bukkit.getPlayer(repair.getPlayerUUID());
            paginator.addLine(repair.getName() + " " + (p == null ? "None" : p.getName()) + " @ " + repair.remaining()
                    + ", " + (repair.size() - repair.remaining()) + " / " + repair.size());
        }

        if (paginator.isEmpty()) {
            sender.sendMessage(
                    MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Repair - No Repairs"));
            return true;
        }

        if (!paginator.isInBounds(page)) {
            sender.sendMessage(MOVECRAFT_COMMAND_PREFIX +
                    net.countercraft.movecraft.localisation.I18nSupport
                            .getInternationalisedString("Paginator - Page Number")
                    + " " + page + " " +
                    net.countercraft.movecraft.localisation.I18nSupport
                            .getInternationalisedString("Paginator - Exceeds Bounds"));
            return true;
        }

        for (String line : paginator.getPage(page)) {
            sender.sendMessage(line);
        }

        return true;
    }
}
