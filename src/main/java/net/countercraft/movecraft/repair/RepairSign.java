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
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.localisation.I18nSupport;
import net.countercraft.movecraft.repair.types.ProtoRepair;
import net.countercraft.movecraft.repair.types.Repair;
import net.countercraft.movecraft.repair.types.RepairState;
import net.countercraft.movecraft.repair.types.blobs.RepairBlob;
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
            if (getItemInHand(player, event.getHand()) != Config.RepairTool)
                return;

            event.setCancelled(true);
            onLeftClick(sign, player, craft);
        }
    }

    public void onRightClick(Sign sign, Player player, PlayerCraft craft) {
        UUID uuid = player.getUniqueId();

        ProtoRepair protoRepair = MovecraftRepair.getInstance().getProtoRepairCache().get(uuid);
        if (protoRepair == null) {
            // No cached repair (or expired)
            createProtoRepair(sign, uuid, player, craft);
            return;
        }

        // Cached repair, try running the repair
        Repair repair = null;
        try {
            repair = protoRepair.execute(craft, sign);
        }
        catch (ProtoRepair.NotEnoughMoneyException e) {
            // Not enough money, tell the player
            player.sendMessage(I18nSupport.getInternationalisedString("Economy - Not Enough Money"));
        }
        catch (ProtoRepair.NotEnoughItemsException e) {
            // Not enough items, tell the player
            for (RepairBlob blob : e.getRemaining().getKeySet()) {
                player.sendMessage(
                    I18nSupport.getInternationalisedString("Repair - Need more of material")
                    + String.format(
                        ": %s - %d",
                        blob.getName(),
                        (int) Math.ceil(e.getRemaining().get(blob))
                    )
                );
            }
            return;
        }
        catch (ProtoRepair.ProtoRepairExpiredException | ProtoRepair.ProtoRepairLocationException e) {
            // Expired or wrong location, go back to first click
            // ItemRemovalException shouldn't happen, but go back to first click regardless
            createProtoRepair(sign, uuid, player, craft);
            return;
        }
        catch (Exception e) {
            // Something weird went wrong, let it fail silently
            e.printStackTrace();
            return;
        }

        // Release the craft, and start the repair
        CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.REPAIR, true); // Note: This change is "temporary" and means that repairs allow the player to repilot and could have damaging effects on combat releases
        MovecraftRepair.getInstance().getRepairManager().start(repair);
    }

    private void createProtoRepair(Sign sign, UUID uuid, Player player, PlayerCraft craft) {
        String stateName = ChatColor.stripColor(sign.getLine(1));

        // Get the repair state from file
        RepairState state;
        try {
            state = new RepairState(uuid, stateName);
        }
        catch (IOException e) {
            player.sendMessage(I18nSupport.getInternationalisedString("Repair - State not found"));
            return;
        }

        // Convert to a proto repair
        ProtoRepair protoRepair;
        try {
            protoRepair = state.execute(sign);
        }
        catch (WorldEditException e) {
            player.sendMessage(I18nSupport.getInternationalisedString("Repair - State not found"));
            return;
        }
        if (protoRepair == null) {
            player.sendMessage(I18nSupport.getInternationalisedString("Repair - State not found"));
            return;
        }

        player.sendMessage(I18nSupport.getInternationalisedString("Repair - Total damaged blocks") + ": " + protoRepair.getDamagedBlockCount());
        double percent = 100.0 * protoRepair.getDamagedBlockCount() / craft.getHitBox().size();
        player.sendMessage(I18nSupport.getInternationalisedString("Repair - Percentage of craft") + String.format(": %.2f", percent));
        if (percent > Config.RepairMaxPercent) {
            player.sendMessage(I18nSupport.getInternationalisedString("Repair - Failed Craft Too Damaged"));
            return;
        }

        for (RepairBlob blob : protoRepair.getMaterials().getKeySet()) {
            player.sendMessage(String.format(
                "%s : %d",
                blob.getName(),
                (int) Math.ceil(protoRepair.getMaterials().get(blob))
            ));
        }

        long duration = (long) Math.ceil(protoRepair.getQueue().size() * Config.RepairTicksPerBlock / 20.0);
        player.sendMessage(I18nSupport.getInternationalisedString("Repair - Seconds to complete repair") + String.format(": %d", duration));

        player.sendMessage(I18nSupport.getInternationalisedString("Repair - Money to complete repair") + String.format(": %.2f", protoRepair.getQueue().size() * Config.RepairMoneyPerBlock));

        // Add to cache only if not empty
        if (!protoRepair.getQueue().isEmpty())
            MovecraftRepair.getInstance().getProtoRepairCache().add(protoRepair);
    }

    public void onLeftClick(Sign sign, Player player, PlayerCraft craft) {
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
