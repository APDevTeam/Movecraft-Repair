package net.countercraft.movecraft.repair;

import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.util.TopicPaginator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class RepairListCommand implements CommandExecutor {
    private int ENTRIES_PER_PAGE = 10;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(I18nSupport.getInternationalisedString(MOVECRAFT_COMMAND_PREFIX + "RepairList - This command can only be run by a player"));
        }
        Player player = (Player) sender;

        if (!player.hasPermission("movecraft.repair.repairlist")) {
            player.sendMessage(I18nSupport.getInternationalisedString(MOVECRAFT_COMMAND_PREFIX + "You don't have permission to use this command."));
            return true;
        }

        File repairDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(repairDirectory, player.getUniqueId().toString());
        if (!playerDirectory.exists()) {
            player.sendMessage(I18nSupport.getInternationalisedString(MOVECRAFT_COMMAND_PREFIX + "Repair States - None Found"));
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

        int totalPages = (int) Math.ceil((double) schemList.length / ENTRIES_PER_PAGE);

        if (page < 1 || page > totalPages) {
            player.sendMessage(I18nSupport.getInternationalisedString("Error - Page number out of range."));
            return true;
        }

        int startIndex = (page - 1) * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, schemList.length);

        for (int i = startIndex; i < endIndex; i++) {
            pageinator.addLine(schemList[i].getName().replace(".schem", ""));
        }

        if (pageinator.isEmpty()) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Repair States - None Found"));
            return true;
        }

        if(!pageinator.isInBounds(page)){
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Paginator - Invalid page") + "\"" + page + "\"");
            return true;
        }

        for(String line :pageinator.getPage(page))
            player.sendMessage(line);
        return true;

    }
}
