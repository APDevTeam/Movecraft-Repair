package net.countercraft.movecraft.repair.utils;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractBufferingExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.buffer.ExtentBuffer;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.mapUpdater.WE7UpdateCommand;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class WE7Utils extends WEUtils {
    private static final ClipboardFormat SCHEMATIC_FORMAT = BuiltInClipboardFormat.SPONGE_SCHEMATIC;

    public WE7Utils(Plugin movecraftRepair) {
        super(movecraftRepair);
    }

    public boolean saveCraftRepairState(@NotNull PlayerCraft craft, @NotNull Sign sign) {
        HitBox hitBox = craft.getHitBox();
        File saveDirectory = new File(dataFolder, "RepairStates");
        World world = craft.getWorld();
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        BlockVector3 origin = BlockVector3.at(sign.getX(),sign.getY(),sign.getZ());
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        BlockVector3 minPos = BlockVector3.at(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ());
        BlockVector3 maxPos = BlockVector3.at(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ());
        CuboidRegion cRegion = new CuboidRegion(minPos, maxPos);
        File playerDirectory = new File(saveDirectory, craft.getPlayer().getUniqueId().toString());
        if (!playerDirectory.exists()){
            playerDirectory.mkdirs();
        }
        String repairName = ChatColor.stripColor(sign.getLine(1));
        repairName += SCHEMATIC_FORMAT.getPrimaryFileExtension();
        File repairStateFile = new File(playerDirectory, repairName);
        Set<BaseBlock> blockSet = baseBlocksFromCraft(craft);
        BitmapHitBox outsideLocs = (BitmapHitBox) solidBlockLocs(craft.getW(), cRegion).difference(craft.getHitBox());
        try {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(cRegion);
            clipboard.setOrigin(origin);
            Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
            ForwardExtentCopy copy = new ForwardExtentCopy(source, cRegion, clipboard.getOrigin(), clipboard, clipboard.getOrigin());
            BlockMask mask = new BlockMask(source, blockSet);
            copy.setSourceMask(mask);
            Operations.completeLegacy(copy);
            for (MovecraftLocation outsideLoc : outsideLocs){
                clipboard.setBlock(BlockVector3.at(outsideLoc.getX(), outsideLoc.getY(), outsideLoc.getZ()), BlockTypes.AIR.getDefaultState().toBaseBlock());
            }
            ClipboardWriter writer = SCHEMATIC_FORMAT.getWriter(new FileOutputStream(repairStateFile, false));
            writer.write(clipboard);
            writer.close();
            return true;

        } catch (IOException | WorldEditException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Clipboard loadCraftRepairStateClipboard(@NotNull PlayerCraft craft, Sign sign) {
        File dataDirectory = new File(dataFolder, "RepairStates");
        File playerDirectory = new File(dataDirectory, craft.getPlayer().getUniqueId().toString());
        if (!playerDirectory.exists()){
            return null;
        }
        String repairName = ChatColor.stripColor(sign.getLine(1));
        repairName += SCHEMATIC_FORMAT.getPrimaryFileExtension();
        File file = new File(playerDirectory, repairName); // The schematic file
        Clipboard clipboard;
        try {
            clipboard = SCHEMATIC_FORMAT.getReader(new FileInputStream(file)).read();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (clipboard == null) {
            return null;
        }

        BlockVector3 minPos = clipboard.getMinimumPoint();
        BlockVector3 size = clipboard.getDimensions();
        BlockVector3 distance = clipboard.getOrigin().subtract(clipboard.getMinimumPoint());
        BlockVector3 offset = BlockVector3.at(sign.getX(), sign.getY(), sign.getZ()).subtract(distance);

        // Calculate rotation for schematic
        BlockVector3 schematicSignPosition = BlockVector3.at(sign.getX(), sign.getY(), sign.getZ()).subtract(offset).add(minPos);
        BaseBlock schematicSign = clipboard.getFullBlock(schematicSignPosition);
        int rotation = -1;
        for(var e : schematicSign.getStates().entrySet()) {
            if(e.getKey().getName().equals("rotation")) {
                rotation = (int) e.getValue();
            }
        }
        BlockFace schematicSignFacing = blockFaceFromNBTRotation(rotation);
        BlockData signData = sign.getBlockData();
        BlockFace repairSignFacing = null;
        if(signData instanceof org.bukkit.block.data.type.Sign) {
            repairSignFacing = ((org.bukkit.block.data.type.Sign) signData).getRotation();
        }
        else if(signData instanceof org.bukkit.block.data.type.WallSign) {
            // TODO: Something still wrong here
            repairSignFacing = ((org.bukkit.block.data.type.WallSign) signData).getFacing().getOppositeFace();
        }
        // If unavailable, default to no rotation
        long angle = angleBetweenBlockFaces(repairSignFacing, schematicSignFacing);

        // Apply rotation
        try {
            clipboard = ClipboardUtils.transform(clipboard, new AffineTransform().rotateY(angle));
        } catch (WorldEditException e) {
            e.printStackTrace();
        }

        long numDiffBlocks = 0;
        HashMap<Pair<Material, Byte>, Double> missingBlocks = new HashMap<>();
        ArrayDeque<MovecraftRepairLocation> locMissingBlocks = new ArrayDeque<>();
        for (int x = 0; x <= size.getBlockX(); x++) {
            for (int z = 0; z <= size.getBlockZ(); z++) {
                for (int y = 0; y <= size.getBlockY(); y++) {
                    BlockVector3 position = BlockVector3.at(minPos.getBlockX() + x, minPos.getBlockY() + y, minPos.getBlockZ() + z);
                    Location bukkitLoc = new Location(sign.getWorld(), offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z);
                    BaseBlock block = clipboard.getFullBlock(position);
                    Block bukkitBlock = sign.getWorld().getBlockAt(bukkitLoc);
                    if (block.getBlockType() != BlockTypes.AIR &&
                            block.getBlockType() != BlockTypes.CAVE_AIR &&
                            block.getBlockType() != BlockTypes.AIR &&
                            bukkitBlock.getType() != BukkitAdapter.adapt(block.getBlockType())) {
                        Material itemToConsume = BukkitAdapter.adapt(block.getBlockType());
                        double qtyToConsume = 1.0;
                        numDiffBlocks++;
                        //some blocks aren't represented by items with the same number as the block
                        if (itemToConsume.name().endsWith("WALL_SIGN")) {
                            itemToConsume = Material.getMaterial(itemToConsume.name().replace("WALL_", ""));
                        } else if (itemToConsume.name().endsWith("DOOR") && !itemToConsume.name().endsWith("TRAPDOOR")) {
                            qtyToConsume = 0.5;
                        } else if (itemToConsume.name().endsWith("BED")) {
                            qtyToConsume = 0.5;
                        }
                        switch (itemToConsume) {
                            case FURNACE:
                                //Count fuel in furnaces
                                ListTag list = block.getNbtData().getListTag("Items");
                                if (list != null){
                                    for (Tag t : list.getValue()) {
                                        if (!(t instanceof CompoundTag)) {
                                            continue;
                                        }
                                        CompoundTag ct = (CompoundTag) t;
                                        if (ct.getByte("Slot") == 2) {//Ignore the result slot
                                            continue;
                                        }
                                        String id = ct.getString("id");
                                        Pair<Material, Byte> content;
                                        if (id.equals("minecraft:coal")){
                                            content = new Pair<>(Material.COAL, (byte) 0);
                                            if (!missingBlocks.containsKey(content)) {
                                                missingBlocks.put(content, (double) ct.getByte("Count"));
                                            } else {
                                                double num = missingBlocks.get(content);
                                                num += ct.getByte("Count");
                                                missingBlocks.put(content, num);
                                            }
                                        }
                                        if (id.equals("minecraft:charcoal")){
                                            content = new Pair<>(Material.CHARCOAL, (byte) 0);
                                            if (!missingBlocks.containsKey(content)) {
                                                missingBlocks.put(content, (double) ct.getByte("Count"));
                                            } else {
                                                double num = missingBlocks.get(content);
                                                num += ct.getByte("Count");
                                                missingBlocks.put(content, num);
                                            }
                                        }
                                        if (id.equals("minecraft:coal_block")) {
                                            content = new Pair<>(Material.COAL_BLOCK, (byte) 0);
                                            if (!missingBlocks.containsKey(content)){
                                                missingBlocks.put(content, (double) ct.getByte("Count"));
                                            } else {
                                                double num = missingBlocks.get(content);
                                                num +=  ct.getByte("Count");
                                                missingBlocks.put(content, num);
                                            }
                                        }
                                    }
                                }
                                break;
                            case REDSTONE_WIRE:// redstone
                                itemToConsume = Material.REDSTONE;
                                break;
                            case WATER:  // don't require water to be in the chest
                                itemToConsume = Material.AIR;
                                qtyToConsume = 0.0;
                                break;
                            case LAVA: // don't require lava either, yeah you could exploit this for free lava, so make sure you set a price per block
                                itemToConsume = Material.AIR;
                                qtyToConsume = 0.0;
                                break;
                            case DISPENSER: {
                                Tag t = block.getNbtData().getValue().get("Items");
                                ListTag lt = null;
                                if (t instanceof ListTag) {
                                    lt = (ListTag) t;
                                }
                                int numTNT = 0;
                                int numFireCharges = 0;
                                int numWaterBuckets = 0;
                                if (lt != null) {
                                    for (Tag entryTag : lt.getValue()) {
                                        if (entryTag instanceof CompoundTag) {
                                            CompoundTag cTag = (CompoundTag) entryTag;
                                            if (cTag.toString().contains("minecraft:tnt")) {
                                                numTNT += cTag.getByte("Count");
                                            }
                                            if (cTag.toString().contains("minecraft:fire_charge")) {
                                                numFireCharges += cTag.getByte("Count");
                                            }
                                            if (cTag.toString().contains("minecraft:water_bucket")) {
                                                numWaterBuckets += cTag.getByte("Count");
                                            }
                                        }
                                    }
                                }
                                Pair<Material, Byte> content;
                                if (numTNT > 0) {
                                    content = new Pair<>(Material.TNT, (byte) 0);
                                    if (!missingBlocks.containsKey(content)) {
                                        missingBlocks.put(content, (double) numTNT);
                                    } else {
                                        double num = missingBlocks.get(content);
                                        num += numTNT;
                                        missingBlocks.put(content, num);
                                    }
                                }
                                if (numFireCharges > 0) {
                                    content = new Pair<>(Material.FIRE_CHARGE, (byte) 0);
                                    if (!missingBlocks.containsKey(content)) {
                                        missingBlocks.put(content, (double) numFireCharges);
                                    } else {
                                        double num = missingBlocks.get(content);
                                        num += numFireCharges;
                                        missingBlocks.put(content, num);
                                    }
                                }
                                if (numWaterBuckets > 0) {
                                    content = new Pair<>(Material.WATER_BUCKET, (byte) 0);
                                    if (!missingBlocks.containsKey(content)) {
                                        missingBlocks.put(content, (double) numWaterBuckets);
                                    } else {
                                        double num = missingBlocks.get(content);
                                        num += numWaterBuckets;
                                        missingBlocks.put(content, num);
                                    }
                                }
                                break;
                            }
                        }
                        if (!itemToConsume.name().endsWith("AIR")) {
                            Pair<Material, Byte> missingBlock = new Pair<>(itemToConsume, (byte) 0);
                            if (!missingBlocks.containsKey(missingBlock)) {
                                missingBlocks.put(missingBlock, qtyToConsume);
                            } else {
                                Double num = missingBlocks.get(missingBlock);
                                num += qtyToConsume;
                                missingBlocks.put(missingBlock, num);
                            }
                        }
                        if (!block.getBlockType().getName().endsWith("air")){
                            locMissingBlocks.addLast(new MovecraftRepairLocation(new MovecraftLocation(offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z),new MovecraftLocation(position.getBlockX(),position.getBlockY(),position.getBlockZ())));
                        }
                    }
                    if (bukkitBlock.getType() == Material.DISPENSER && block.getBlockType() == BlockTypes.DISPENSER) {
                        boolean needReplace = false;
                        Tag t = block.getNbtData().getValue().get("Items");
                        ListTag lt = null;
                        if (t instanceof ListTag) {
                            lt = (ListTag) t;
                        }
                        int numTNT = 0;
                        int numFireCharges = 0;
                        int numWaterBuckets = 0;
                        if (lt != null) {
                            for (Tag entryTag : lt.getValue()) {
                                if (entryTag instanceof CompoundTag) {
                                    CompoundTag cTag = (CompoundTag) entryTag;
                                    if (cTag.toString().contains("minecraft:tnt")) {
                                        numTNT += cTag.getByte("Count");
                                    }
                                    if (cTag.toString().contains("minecraft:fire_charge")) {
                                        numFireCharges += cTag.getByte("Count");
                                    }
                                    if (cTag.toString().contains("minecraft:water_bucket")) {
                                        numWaterBuckets += cTag.getByte("Count");
                                    }
                                }
                            }
                        }
                        Dispenser bukkitDispenser = (Dispenser) bukkitBlock.getState();
                        //Bukkit.getLogger().info(String.format("TNT: %d, Fireballs: %d, Water buckets: %d", numTNT, numFireCharges, numWaterBuckets));
                        for (ItemStack iStack : bukkitDispenser.getInventory().getContents()) {
                            if (iStack != null) {
                                if (iStack.getType() == Material.TNT) {
                                    numTNT -= iStack.getAmount();
                                }
                                if (iStack.getType() == Material.FIRE_CHARGE) {
                                    numFireCharges -= iStack.getAmount();
                                }
                                if (iStack.getType() == Material.WATER_BUCKET) {
                                    numWaterBuckets -= iStack.getAmount();
                                }
                            }
                        }
                        //Bukkit.getLogger().info(String.format("TNT: %d, Fireballs: %d, Water buckets: %d", numTNT, numFireCharges, numWaterBuckets));
                        Pair<Material, Byte> content;
                        if (numTNT > 0) {
                            content = new Pair<>(Material.TNT, (byte) 0);
                            if (!missingBlocks.containsKey(content)) {
                                missingBlocks.put(content, (double) numTNT);
                            } else {
                                double num = missingBlocks.get(content);
                                num += numTNT;
                                missingBlocks.put(content, num);
                            }
                            needReplace = true;
                        }
                        if (numFireCharges > 0) {
                            content = new Pair<>(Material.FIRE_CHARGE, (byte) 0);
                            if (!missingBlocks.containsKey(content)) {
                                missingBlocks.put(content, (double) numFireCharges);
                            } else {
                                double num = missingBlocks.get(content);
                                num += numFireCharges;
                                missingBlocks.put(content, num);
                            }
                            needReplace = true;
                        }
                        if (numWaterBuckets > 0) {
                            content = new Pair<>(Material.WATER_BUCKET, (byte) 0);
                            if (!missingBlocks.containsKey(content)) {
                                missingBlocks.put(content, (double) numWaterBuckets);
                            } else {
                                double num = missingBlocks.get(content);
                                num += numWaterBuckets;
                                missingBlocks.put(content, num);
                            }
                            needReplace = true;
                        }
                        if (needReplace) {
                            numDiffBlocks++;
                            locMissingBlocks.addLast(new MovecraftRepairLocation(new MovecraftLocation(offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z),new MovecraftLocation(position.getBlockX(),position.getBlockY(),position.getBlockZ())));
                        }
                    }
                    if (bukkitBlock.getType() == Material.FURNACE && block.getBlockType() == BlockTypes.FURNACE){
                        //Count fuel in furnaces
                        ListTag list = block.getNbtData().getListTag("Items");
                        FurnaceInventory fInv = ((Furnace) bukkitBlock.getState()).getInventory();
                        byte needsRefill = 0;
                        if (list != null){
                            for (Tag t : list.getValue()){
                                if (!(t instanceof CompoundTag)){
                                    continue;
                                }
                                CompoundTag ct = (CompoundTag) t;
                                byte slot = ct.getByte("Slot");
                                if (slot == 2){//Ignore the result slot
                                    continue;
                                }
                                String id = ct.getString("id");
                                Pair<Material, Byte> content;
                                if (id.equals("minecraft:coal")){
                                    byte data = (byte)ct.getShort("Damage");
                                    byte count = ct.getByte("Count");
                                    if (slot == 0) {//Smelting slot
                                        if (fInv.getSmelting() != null && fInv.getSmelting().getData().getData() == data){
                                            count -= (byte) fInv.getSmelting().getAmount();
                                        }
                                    } else if (slot == 1) {//Fuel slot
                                        if (fInv.getFuel() != null && fInv.getFuel().getData().getData() == data){
                                            count -= (byte) fInv.getFuel().getAmount();
                                        }
                                    }
                                    if (count > 0) {
                                        content = new Pair<>(Material.COAL, data);
                                        if (!missingBlocks.containsKey(content)) {
                                            missingBlocks.put(content, (double) count);
                                        } else {
                                            double num = missingBlocks.get(content);
                                            num += (double) count;
                                            missingBlocks.put(content, num);
                                        }
                                        needsRefill++;
                                    }
                                }
                                if (id.equals("minecraft:coal_block")){
                                    byte count = ct.getByte("Count");
                                    //Smelting slot
                                    //Fuel slot
                                    if (slot == 0) {
                                        count -= fInv.getSmelting() != null ? (byte) fInv.getSmelting().getAmount() : 0;
                                    } else if (slot == 1) {
                                        count -= fInv.getFuel() != null ? (byte) fInv.getFuel().getAmount() : 0;
                                    }
                                    if (count > 0) {
                                        content = new Pair<>(Material.COAL_BLOCK, (byte) 0);
                                        if (!missingBlocks.containsKey(content)) {
                                            missingBlocks.put(content, (double) count);
                                        } else {
                                            double num = missingBlocks.get(content);
                                            num += (double) count;
                                            missingBlocks.put(content, num);
                                        }
                                        needsRefill++;
                                    }
                                }
                            }
                        }
                        if (needsRefill > 0){
                            numDiffBlocks++;
                            locMissingBlocks.addLast(new MovecraftRepairLocation(new MovecraftLocation(offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z),new MovecraftLocation(position.getBlockX(),position.getBlockY(),position.getBlockZ())));
                        }
                    }
                }
            }
        }
        String repairStateName = craft.getPlayer().getUniqueId().toString();
        repairStateName += "_";
        repairStateName += repairName.replace(SCHEMATIC_FORMAT.getPrimaryFileExtension(), "");
        locMissingBlocksMap.put(repairStateName, locMissingBlocks);
        missingBlocksMap.put(repairStateName, missingBlocks);
        numDiffBlocksMap.put(repairStateName, numDiffBlocks);
        return clipboard;
    }

    @Contract(pure = true)
    private @Nullable BlockFace blockFaceFromNBTRotation(int rotation) {
        switch(rotation) {
            case 0: return BlockFace.SOUTH;
            case 1: return BlockFace.SOUTH_SOUTH_WEST;
            case 2: return BlockFace.SOUTH_WEST;
            case 3: return BlockFace.WEST_SOUTH_WEST;
            case 4: return BlockFace.WEST;
            case 5: return BlockFace.WEST_NORTH_WEST;
            case 6: return BlockFace.NORTH_WEST;
            case 7: return BlockFace.NORTH_NORTH_WEST;
            case 8: return BlockFace.NORTH;
            case 9: return BlockFace.NORTH_NORTH_EAST;
            case 10: return BlockFace.NORTH_EAST;
            case 11: return BlockFace.EAST_NORTH_EAST;
            case 12: return BlockFace.EAST;
            case 13: return BlockFace.EAST_SOUTH_EAST;
            case 14: return BlockFace.SOUTH_EAST;
            case 15: return BlockFace.SOUTH_SOUTH_EAST;
            case -1:
            default: return null;
        }
    }

    private long angleBetweenBlockFaces(@Nullable BlockFace base, @Nullable BlockFace other) {
        if(base == null || other == null)
            return 0;

        // Vector#angle() does not return the direction, merely the magnitude of the angle difference
        //  Therefore, we have to calculate this manually
        //  Also, Minecraft flips the Z axis, so *yay*
        double baseAngle = Math.atan2(-1.0 * base.getDirection().getZ(), base.getDirection().getX());
        double otherAngle = Math.atan2(-1.0 * other.getDirection().getZ(), other.getDirection().getX());
        double angle = otherAngle - baseAngle;
        angle /= 2 * Math.PI; // Convert from radians to turns
        angle *= 4; // Convert to quarter-turns
        angle *= -1; // Flip
        long angleDegrees = Math.round(angle); // Round to nearest quarter-turn
        angleDegrees *= 90; // Convert to degrees
        return angleDegrees;
    }

    public HashMap<Pair<Material, Byte>, Double> getMissingBlocks(String repairName) {
        return missingBlocksMap.get(repairName);
    }

    public ArrayDeque<MovecraftRepairLocation> getMissingBlockLocations(String repairName) {
        return locMissingBlocksMap.get(repairName);
    }

    public long getNumDiffBlocks(String s) {
        return numDiffBlocksMap.get(s);
    }

    private Set<BaseBlock> baseBlocksFromCraft(Craft craft) {
        HashSet<BaseBlock> returnSet = new HashSet<>();
        HitBox hitBox = craft.getHitBox();
        World w = craft.getW();
        for (MovecraftLocation location : hitBox) {
            BlockData data = w.getBlockAt(location.toBukkit(w)).getBlockData();
            returnSet.add(BukkitAdapter.adapt(data).toBaseBlock());
        }
        if (Settings.Debug) {
            Bukkit.getLogger().info(returnSet.toString());
        }
        return returnSet;
    }

    private BitmapHitBox solidBlockLocs(World w, CuboidRegion cr){
        BitmapHitBox returnSet = new BitmapHitBox();
        for (int x = cr.getMinimumPoint().getBlockX(); x <= cr.getMaximumPoint().getBlockX(); x++){
            for (int y = cr.getMinimumPoint().getBlockY(); y <= cr.getMaximumPoint().getBlockY(); y++){
                for (int z = cr.getMinimumPoint().getBlockZ(); z <= cr.getMaximumPoint().getBlockZ(); z++){
                    MovecraftLocation ml = new MovecraftLocation(x, y, z);
                    if (ml.toBukkit(w).getBlock().getType() != Material.AIR){
                        returnSet.add(ml);
                    }
                }
            }
        }
        return returnSet;
    }

    public boolean saveChunk(Chunk c, File directory, @Nullable HashSet<Material> materialMask) {
        if(!directory.exists())
            directory.mkdirs();

        // Load chunk if not loaded already
        if(!c.isLoaded())
            c.load();

        BlockVector3 minPos = BlockVector3.at(c.getX() * 16, 0, c.getZ() * 16);
        BlockVector3 maxPos = BlockVector3.at((c.getX() * 16) + 15, 255, (c.getZ() * 16) + 15);
        com.sk89q.worldedit.world.World world = new BukkitWorld(c.getWorld());
        CuboidRegion region = new CuboidRegion(world, minPos, maxPos);

        // Copy chunk into memory
        Set<BaseBlock> baseBlockSet = new HashSet<>();
        for(int x = 0; x < 16; x++) {
            for(int y = 0; y < 256; y++) {
                for(int z = 0; z < 16; z++) {
                    Block b = c.getBlock(x, y, z);
                    if(b.getType().equals(Material.AIR))
                        continue;

                    // A null materialMask will be understood as saving every block
                    if(materialMask == null || !materialMask.contains(b.getType()))
                        continue;

                    baseBlockSet.add(BukkitAdapter.adapt(b.getBlockData()).toBaseBlock());
                }
            }
        }

        // Save chunk to disk
        File file = new File(directory, c.getX() + "_" + c.getZ() + SCHEMATIC_FORMAT.getPrimaryFileExtension());
        try {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, 16*16*257);
            ForwardExtentCopy copy = new ForwardExtentCopy(source, region, clipboard.getOrigin(), clipboard, minPos);
            if(materialMask != null) {
                // A null materialMask will be understood as saving every block
                Mask mask = new MaterialMask(materialMask, c.getWorld());
                copy.setSourceMask(mask);
            }
            Operations.completeLegacy(copy);
            ClipboardWriter writer = SCHEMATIC_FORMAT.getWriter(new FileOutputStream(file, false));
            writer.write(clipboard);
            writer.close();
            return true;
        }
        catch (MaxChangedBlocksException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean repairChunk(Chunk c, File directory, Predicate<MovecraftLocation> p) {
        // Load schematic from disk
        File file = new File(directory, c.getX() + "_" + c.getZ() + SCHEMATIC_FORMAT.getPrimaryFileExtension());
        Clipboard clipboard;
        World w = c.getWorld();
        try {
            clipboard = SCHEMATIC_FORMAT.getReader(new FileInputStream(file)).read();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Load chunk if not loaded already
        if(!c.isLoaded())
            c.load();

        // Repair chunk
        for (int x = clipboard.getMinimumPoint().getBlockX(); x <= clipboard.getMaximumPoint().getBlockX(); x++) {
            for (int y = clipboard.getMinimumPoint().getBlockY(); y <= clipboard.getMaximumPoint().getBlockY(); y++) {
                for (int z = clipboard.getMinimumPoint().getBlockZ(); z <= clipboard.getMaximumPoint().getBlockZ(); z++) {
                    BlockVector3 ccloc = BlockVector3.at(x, y, z);
                    BaseBlock bb = clipboard.getFullBlock(ccloc);
                    if (bb.getBlockType() == BlockTypes.AIR || bb.getBlockType() == BlockTypes.CAVE_AIR || bb.getBlockType() == BlockTypes.VOID_AIR)
                        continue; // most blocks will be air, quickly move on to the next. This loop will run millions of times, needs to be fast
                    if (!w.getBlockAt(x, y, z).isEmpty() && !w.getBlockAt(x, y, z).isLiquid())
                        continue; // Don't replace blocks which aren't liquid or air

                    MovecraftLocation moveloc = new MovecraftLocation(x, y, z);
                    if(!p.test(moveloc))
                        continue;

                    WE7UpdateCommand updateCommand = new WE7UpdateCommand(bb, w, moveloc, BukkitAdapter.adapt(bb.getBlockType()));
                    MapUpdateManager.getInstance().scheduleUpdate(updateCommand);
                }
            }
        }
        return true;
    }

    @Override
    public UpdateCommandsQueuePair getUpdateCommands(Clipboard clipboard, World world, ArrayDeque<MovecraftRepairLocation> locMissingBlocks) {
        final LinkedList<UpdateCommand> updateCommands = new LinkedList<>();
        final LinkedList<UpdateCommand> updateCommandsFragileBlocks = new LinkedList<>();
        while (!locMissingBlocks.isEmpty()){
            MovecraftRepairLocation locs = locMissingBlocks.pollFirst();
            assert locs != null;
            MovecraftLocation cLoc = locs.getOrigin();
            MovecraftLocation moveLoc = locs.getOffset();
            //To avoid any issues during the repair, keep certain blocks in different linked lists
            BaseBlock baseBlock = clipboard.getFullBlock(BlockVector3.at(cLoc.getX(),cLoc.getY(),cLoc.getZ()));
            Material type = BukkitAdapter.adapt(baseBlock.getBlockType());
            if (fragileBlock(type)) {
                WE7UpdateCommand updateCommand = new WE7UpdateCommand(baseBlock, world, moveLoc, type);
                updateCommandsFragileBlocks.add(updateCommand);
            } else {
                WE7UpdateCommand updateCommand = new WE7UpdateCommand(baseBlock, world, moveLoc, type);
                updateCommands.add(updateCommand);
            }
        }
        return new UpdateCommandsQueuePair(updateCommands, updateCommandsFragileBlocks);
    }


    private boolean fragileBlock(Material type) {
        return type.name().endsWith("BUTTON")
                || type.name().endsWith("DOOR_BLOCK")
                || type.name().startsWith("DIODE")
                || type.name().startsWith("REDSTONE_COMPARATOR")
                || type.name().endsWith("WATER")
                || type.name().endsWith("LAVA")
                || type.equals(Material.LEVER)
                || type.name().endsWith("WALL_SIGN")
                || type.name().endsWith("WALL_BANNER")
                || type.equals(Material.REDSTONE_WIRE)
                || type.equals(Material.LADDER)
                || type.name().endsWith("BED")
                || type.equals(Material.TRIPWIRE_HOOK)
                || type.name().endsWith("WALL_TORCH");
    }

    private class MaterialMask implements Mask {
        private final HashSet<Material> materialMask;
        private final World w;

        public MaterialMask(HashSet<Material> materialMask, World w) {
            this.materialMask = materialMask;
            this.w = w;
        }

        @Override
        public boolean test(BlockVector3 vector) {
            Block b = w.getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
            return materialMask.contains(b.getType());
        }

        @javax.annotation.Nullable
        @Override
        public Mask2D toMask2D() {
            return null;
        }
    }
}
