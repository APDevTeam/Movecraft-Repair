package net.countercraft.movecraft.repair;

import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.util.TopicPaginator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static net.countercraft.movecraft.repair.util.WEUtils.SCHEMATIC_FORMAT;
import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class RepairListCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Repair - Must Be Player");
        }
        Player player = (Player) sender;

        if (!player.hasPermission("movecraft.repair.repairlist")) {
            player.sendMessage(I18nSupport.getInternationalisedString(MOVECRAFT_COMMAND_PREFIX + "Insufficient Permissions"));
            return true;
        }

        File repairDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(repairDirectory, player.getUniqueId().toString());
        if (!playerDirectory.exists()) {
            player.sendMessage(I18nSupport.getInternationalisedString(MOVECRAFT_COMMAND_PREFIX + "Repair - None Found"));
            return true;
        }

        File[] schemList = playerDirectory.listFiles();
        TopicPaginator pageinator = new TopicPaginator(I18nSupport.getInternationalisedString("Repair States"));

        int page = 1; // Default page
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Paginator - Invalid Page") +" \"" + args[0] + "\"");
                return true;
            }
        }

        if (pageinator.isInBounds(page)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Paginator - Invalid page") + "\"" + page + "\"");
            return true;
        }

        for (File schemFile : schemList) {
            pageinator.addLine(schemFile.getName().replace((CharSequence) SCHEMATIC_FORMAT, ""));
        }

        if (pageinator.isEmpty()) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Repair - None Found"));
            return true;
        }

        for(String line :pageinator.getPage(page))
            player.sendMessage(line);
        return true;

    }
}
