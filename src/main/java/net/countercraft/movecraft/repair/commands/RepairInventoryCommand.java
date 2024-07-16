package net.countercraft.movecraft.repair.commands;

import java.util.LinkedList;
import java.util.List;

import net.countercraft.movecraft.util.ChatUtils;
import net.countercraft.movecraft.util.ComponentPaginator;
import net.kyori.adventure.text.Component;
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

public class RepairInventoryCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Repair - Must Be Player")));
            return true;
        }

        if (!player.hasPermission("movecraft.repair.repaircancel")) {
            player.sendMessage(ChatUtils.commandPrefix().append(net.countercraft.movecraft.localisation.I18nSupport.getInternationalisedComponent("Insufficient Permissions")));
            return true;
        }

        PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null) {
            player.sendMessage(I18nSupport.getInternationalisedComponent("You must be piloting a craft"));
            return true;
        }

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

        RepairCounter inventory = sumInventory(craft);
        List<RepairBlob> keys = new LinkedList<>(inventory.getKeySet());
        keys.sort((key1, key2) -> ((int) (inventory.get(key2) - inventory.get(key1))));

        ComponentPaginator paginator = new ComponentPaginator(
                I18nSupport.getInternationalisedComponent("Inventory - Inventory Header"),
                pageNumber -> "/repairinventory " + pageNumber);
        for (RepairBlob key : keys) {
            paginator.addLine(buildLine(key, inventory.get(key)));
        }

        if (paginator.isEmpty()) {
            player.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Inventory - Empty Craft")));
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

    @NotNull
    private Component buildLine(@NotNull RepairBlob key, double count) {
        return Component.empty()
                .append(Component.text(key.getName()))
                .append(Component.text(": "))
                .append(Component.text(String.format("%,.0f", count)));
    }

    @NotNull
    private RepairCounter sumInventory(@NotNull Craft craft) {
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
