package net.countercraft.movecraft.repair.commands;

import java.util.LinkedList;
import java.util.List;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.repair.RepairBlobManager;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.repair.types.RepairCounter;
import net.countercraft.movecraft.repair.types.blobs.RepairBlob;
import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.TopicPaginator;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class RepairInventoryCommand implements CommandExecutor {
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

        PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null) {
            player.sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return true;
        }

        int page = 1; // Default page
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX + net.countercraft.movecraft.localisation.I18nSupport
                        .getInternationalisedString("Paginator - Invalid Page") + " \"" + args[0] + "\"");
                return true;
            }
        }

        RepairCounter inventory = sumInventory(craft);
        List<RepairBlob> keys = new LinkedList<>(inventory.getKeySet());
        keys.sort((key1, key2) -> ((int) (inventory.get(key2) - inventory.get(key1))));

        TopicPaginator paginator = new TopicPaginator(
                I18nSupport.getInternationalisedString("Inventory - Inventory Header"), false);
        for (RepairBlob key : keys) {
            paginator.addLine(buildLine(key, inventory.get(key)));
        }

        if (paginator.isEmpty()) {
            player.sendMessage(
                    MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Inventory - Empty Craft"));
            return true;
        }

        if (!paginator.isInBounds(page)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + net.countercraft.movecraft.localisation.I18nSupport
                            .getInternationalisedString("Paginator - Page Number")
                    + " " + page + " " + net.countercraft.movecraft.localisation.I18nSupport
                            .getInternationalisedString("Paginator - Exceeds Bounds"));
            return true;
        }

        for (String line : paginator.getPage(page)) {
            player.sendMessage(line);
        }
        return true;
    }

    private String buildLine(RepairBlob key, double count) {
        return key.getName() + ": " + String.format("%,.0f", count);
    }

    private RepairCounter sumInventory(Craft craft) {
        RepairCounter result = new RepairCounter();
        World world = craft.getWorld();
        for (MovecraftLocation location : craft.getHitBox()) {
            Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
            if (!Tags.CHESTS.contains(block.getType()))
                continue;

            BlockState state = block.getState();
            if (!(state instanceof Container))
                continue;

            Inventory inventory = ((Container) state).getInventory();
            if (inventory instanceof DoubleChestInventory)
                continue; // Don't take from double chests
            for (ItemStack item : inventory.getContents()) {
                if (item == null)
                    continue;

                result.add(RepairBlobManager.get(item.getType()), item.getAmount());
            }
        }
        return result;
    }
}
