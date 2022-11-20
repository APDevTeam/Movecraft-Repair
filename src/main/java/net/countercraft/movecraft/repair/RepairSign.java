package net.countercraft.movecraft.repair;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.sk89q.worldedit.WorldEditException;

import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.repair.types.ProtoRepair;
import net.countercraft.movecraft.repair.types.Repair;
import net.countercraft.movecraft.repair.types.RepairState;
import net.countercraft.movecraft.repair.util.WEUtils;

public class RepairSign implements Listener {
    private final String HEADER = "Repair:";
    private final Map<UUID, Long> leftClickCache = new WeakHashMap<>();

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase(HEADER))
            return;

        // Clear the repair sign if second line is empty
        if (event.getLine(1).isEmpty()) {
            event.getPlayer().sendMessage("You must specify a repair state name on second line");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        if (Config.RepairTicksPerBlock == 0) {
            player.sendMessage(I18nSupport.getInternationalisedString("Repair functionality is disabled or WorldEdit was not detected"));
            return;
        }

        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign))
            return;

        Sign sign = (Sign) event.getClickedBlock().getState();
        String signText = ChatColor.stripColor(sign.getLine(0));
        if (signText == null || !signText.equalsIgnoreCase(HEADER))
            return;

        PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null) {
            player.sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return;
        }
    
        if (!player.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".repair")) {
            player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }
    
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            onRightClick(sign, player, craft);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            onLeftClick(sign, player, craft, event.getHand());
        }
    }

    public void onRightClick(Sign sign, Player player, PlayerCraft craft) {
        UUID uuid = player.getUniqueId();

        ProtoRepair protoRepair = MovecraftRepair.getInstance().getProtoRepairCache().get(uuid);
        if (protoRepair == null) {
            // No cached repair (or expired)
            String stateName = ChatColor.stripColor(sign.getLine(1));

            // Get the repair state from file
            RepairState state;
            try {
                state = new RepairState(uuid, stateName);
            }
            catch (IOException e) {
                player.sendMessage(I18nSupport.getInternationalisedString("Repair - State not found"));;
                return;
            }

            // Convert to a proto repair
            try {
                protoRepair = state.execute(sign);
            }
            catch (WorldEditException e) {
                // Failed to transform
            }

            // Add to cache
            MovecraftRepair.getInstance().getProtoRepairCache().add(protoRepair);
            // TODO: Add the list of required materials?
        }
        else {
            // Cached and non-expired repair, start running
            Repair repair = protoRepair.execute(craft, sign);
            MovecraftRepair.getInstance().getRepairManager().add(repair);
            // TODO: Tell the player?
        }
    }

    public void onLeftClick(Sign sign, Player player, PlayerCraft craft, EquipmentSlot hand) {
        if (getItemInHand(player, hand) != Config.RepairTool)
            return;

        Long lastLeftClick = leftClickCache.get(player.getUniqueId());
        if (lastLeftClick == null || (System.currentTimeMillis() - lastLeftClick.longValue() > 5000)) {
            // First click, just add to the map
            leftClickCache.put(player.getUniqueId(), System.currentTimeMillis());
            return;
        }

        if (!WEUtils.saveCraftSchematic(craft, sign)) {
            player.sendMessage(I18nSupport.getInternationalisedString("Repair - Could not save file"));
            return;
        }

        player.sendMessage(I18nSupport.getInternationalisedString("Repair - State saved"));
    }

    private Material getItemInHand(Player player, EquipmentSlot slot) {
        switch (slot) {
            case HAND:
                return player.getInventory().getItemInMainHand().getType();
            case OFF_HAND:
                return player.getInventory().getItemInOffHand().getType();
            default:
                return null;
        }
    }
}
