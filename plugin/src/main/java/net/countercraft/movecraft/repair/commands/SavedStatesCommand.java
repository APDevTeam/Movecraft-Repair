package net.countercraft.movecraft.repair.commands;

import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.util.TopicPaginator;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class SavedStatesCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("savedstates")) {
            return false;
        }

        if(!sender.hasPermission("movecraft.repair.command.savedstates")) {
            sender.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }

        OfflinePlayer player = null;
        int page = 1;
        if(args.length > 0) {
            if(args.length == 1 && args[0].matches("-?\\d+")) {
                page = Integer.parseInt(args[0]);
            }
            else if(sender.hasPermission("movecraft.repair.command.savedstates.other")) {
                player = MovecraftRepair.getInstance().getServer().getOfflinePlayer(args[0]);

                if(player == null || !player.hasPlayedBefore()) {
                    sender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("SavedStates - Player Not Found"));
                    return true;
                }
                if (args.length >= 2 && args[1].matches("-?\\d+")) {
                    page = Integer.parseInt(args[1]);
                }
            }
        }

        if(player == null) {
            if(!(sender instanceof Player)) {
                sender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("SavedStates - Must Be Player"));
                return true;
            }

            player = (Player) sender;
        }

        File dataDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(dataDirectory, player.getUniqueId().toString());
        if (!playerDirectory.exists() || playerDirectory.listFiles().length == 0) {
            sender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("SavedStates - No Saved States"));
            return true;
        }


        TopicPaginator paginator = new TopicPaginator(I18nSupport.getInternationalisedString("SavedStates - Saved States"));
        for(File file : playerDirectory.listFiles()) {
            if(file.isFile())
            {
                if(file.getName().endsWith(".schematic")) {
                    paginator.addLine(file.getName().replace(".schematic", ""));
                    continue;
                }
                paginator.addLine(file.getName().replace(".schem", ""));
            }
        }

        if(!paginator.isInBounds(page)){
            sender.sendMessage(MOVECRAFT_COMMAND_PREFIX + net.countercraft.movecraft.localisation.I18nSupport.getInternationalisedString("Paginator - Invalid page") + " \"" + page + "\"");
            return true;
        }
        for(String line : paginator.getPage(page))
            sender.sendMessage(line);

        return true;
    }
}
