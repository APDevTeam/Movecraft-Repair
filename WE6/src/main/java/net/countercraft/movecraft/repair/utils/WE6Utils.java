package net.countercraft.movecraft.repair.utils;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.registry.WorldData;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.repair.mapUpdater.WE6UpdateCommand;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.MutableHitBox;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.block.Sign;
import com.sk89q.worldedit.Vector;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;


public class WE6Utils extends WEUtils {

    public WE6Utils(Plugin movecraftRepair) {
        super(movecraftRepair);
    }

    public boolean saveCraftRepairState(Craft craft, Sign sign) {
        HitBox hitBox = craft.getHitBox();
        File saveDirectory = new File(dataFolder, "RepairStates");
        World world = craft.getW();
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        WorldData worldData = weWorld.getWorldData();
        Vector origin = new Vector(sign.getX(),sign.getY(),sign.getZ());
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        Vector minPos = new Vector(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ());
        Vector maxPos = new Vector(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ());
        CuboidRegion cRegion = new CuboidRegion(minPos, maxPos);
        File playerDirectory = new File(saveDirectory,craft.getNotificationPlayer().getUniqueId().toString());
        if (!playerDirectory.exists()){
            playerDirectory.mkdirs();
        }
        String repairName = ChatColor.stripColor(sign.getLine(1));
        repairName += ".schematic";
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
                clipboard.setBlock(new Vector(outsideLoc.getX(), outsideLoc.getY(), outsideLoc.getZ()), new BaseBlock(0));
            }
            ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(repairStateFile, false));
            writer.write(clipboard, worldData);
            writer.close();
            return true;

        } catch (IOException | WorldEditException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Clipboard loadCraftRepairStateClipboard(Craft craft, Sign sign) {
        File dataDirectory = new File(dataFolder, "RepairStates");
        File playerDirectory = new File(dataDirectory,craft.getNotificationPlayer().getUniqueId().toString());
        if (!playerDirectory.exists()){
            return null;
        }
        String repairName = ChatColor.stripColor(sign.getLine(1));
        repairName += ".schematic";
        File file = new File(playerDirectory, repairName); // The schematic file
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(sign.getWorld());
        WorldData worldData = weWorld.getWorldData();
        Clipboard clipboard;
        try {
            clipboard = ClipboardFormat.SCHEMATIC.getReader(new FileInputStream(file)).read(worldData);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (clipboard == null) {
            return null;
        }
        long numDiffBlocks = 0;
        HashMap<Pair<Material, Byte>, Double> missingBlocks = new HashMap<>();
        ArrayDeque<MovecraftRepairLocation> locMissingBlocks = new ArrayDeque<>();
        Vector minPos = clipboard.getMinimumPoint();
        Vector distance = clipboard.getOrigin().subtract(clipboard.getMinimumPoint());
        Vector size = clipboard.getDimensions();
        Vector offset = new Vector(sign.getX() - distance.getBlockX(), sign.getY() - distance.getBlockY(), sign.getZ() - distance.getBlockZ());
        for (int x = 0; x <= size.getBlockX(); x++) {
            for (int z = 0; z <= size.getBlockZ(); z++) {
                for (int y = 0; y <= size.getBlockY(); y++) {
                    Vector position = new Vector(minPos.getBlockX() + x, minPos.getBlockY() + y, minPos.getBlockZ() + z);
                    Location bukkitLoc = new Location(sign.getWorld(), offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z);
                    BaseBlock block = clipboard.getBlock(position);
                    Block bukkitBlock = sign.getWorld().getBlockAt(bukkitLoc);
                    if (block.getType() != 0 && bukkitBlock.getTypeId() != block.getType()) {
                        int itemToConsume = block.getType();
                        byte dataToConsume = (byte) block.getData();
                        double qtyToConsume = 1.0;
                        numDiffBlocks++;
                        //some blocks aren't represented by items with the same number as the block
                        switch (itemToConsume) {
                            case 61:
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
                                            byte data = (byte) ct.getShort("Damage");
                                            content = new Pair<>(Material.COAL, data);
                                            if (!missingBlocks.containsKey(content)) {
                                                missingBlocks.put(content, (double) ct.getByte("Count"));
                                            } else {
                                                double num = missingBlocks.get(content);
                                                num += (double) ct.getByte("Count");
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
                            case 62://burning furnace
                                itemToConsume = 61;
                                break;
                            case 63:// signs
                            case 68:
                                itemToConsume = 323;
                                break;
                            case 93:// repeaters
                            case 94:
                                itemToConsume = 356;
                                break;
                            case 149:// comparators
                            case 150:
                                itemToConsume = 404;
                                break;
                            case 55:// redstone
                                itemToConsume = 331;
                                break;
                            case 118:// cauldron
                                itemToConsume = 380;
                                break;
                            case 124: // lit redstone lamp
                                itemToConsume = 123;
                                break;
                            case 75: // lit redstone torch
                                itemToConsume = 76;
                                break;
                            case 8:
                            case 9:  // don't require water to be in the chest
                                itemToConsume = 0;
                                qtyToConsume = 0.0;
                                break;
                            case 10:
                            case 11: // don't require lava either, yeah you could exploit this for free lava, so make sure you set a price per block
                                itemToConsume = 0;
                                qtyToConsume = 0.0;
                                break;
                            case 26:  //beds
                                itemToConsume = 355;
                                qtyToConsume = 0.5;
                                break;
                            case 64:  //doors
                                itemToConsume = 324;   //since doors and beds encompass two blocks, require only 0.5 block for each of the two blocks
                                qtyToConsume = 0.5;
                                break;
                            case 71:
                                itemToConsume = 330;
                                qtyToConsume = 0.5;
                                break;
                            case 193:
                                itemToConsume = 427;
                                qtyToConsume = 0.5;
                                break;
                            case 194:
                                itemToConsume = 428;
                                qtyToConsume = 0.5;
                                break;
                            case 195:
                                itemToConsume = 429;
                                qtyToConsume = 0.5;
                                break;
                            case 196:
                                itemToConsume = 430;
                                qtyToConsume = 0.5;
                                break;
                            case 197:
                                itemToConsume = 431;
                                qtyToConsume = 0.5;
                                break;
                            case 23: {
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
                                    content = new Pair<>(Material.FIREBALL, (byte) 0);
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
                            case 43: { // for double slabs, require 2 slabs
                                itemToConsume = 44;
                                qtyToConsume = 2;
                                break;
                            }
                            case 125: { // for double wood slabs, require 2 wood slabs
                                itemToConsume = 126;
                                qtyToConsume = 2;
                                break;
                            }
                            case 181: { // for double red sandstone slabs, require 2 red sandstone slabs
                                itemToConsume = 182;
                                qtyToConsume = 2;
                                break;
                            }
                        }
                        if (itemToConsume != 0) {
                            Pair<Material, Byte> missingBlock = new Pair<>(Material.getMaterial(itemToConsume), (byte) 0);
                            if (!missingBlocks.containsKey(missingBlock)) {
                                missingBlocks.put(missingBlock, qtyToConsume);
                            } else {
                                Double num = missingBlocks.get(missingBlock);
                                num += qtyToConsume;
                                missingBlocks.put(missingBlock, num);
                            }

                        }
                        if (block.getType() != 0){
                            locMissingBlocks.addLast(new MovecraftRepairLocation(new MovecraftLocation(offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z),new MovecraftLocation(position.getBlockX(),position.getBlockY(),position.getBlockZ())));
                        }
                    }
                    if (bukkitBlock.getType() == Material.DISPENSER && block.getType() == 23) {
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
                                if (iStack.getType() == Material.FIREBALL) {
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
                            content = new Pair<>(Material.FIREBALL, (byte) 0);
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
                    if (bukkitBlock.getType() == Material.FURNACE && block.getType() == 61){
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
        String repairStateName = craft.getNotificationPlayer().getUniqueId().toString();
        repairStateName += "_";
        repairStateName += repairName.replace(".schematic","");
        locMissingBlocksMap.put(repairStateName, locMissingBlocks);
        missingBlocksMap.put(repairStateName, missingBlocks);
        numDiffBlocksMap.put(repairStateName, numDiffBlocks);
        return clipboard;
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

    @NotNull
    private Set<BaseBlock> baseBlocksFromCraft(@NotNull Craft craft) {
        HashSet<BaseBlock> returnSet = new HashSet<>();
        HitBox hitBox = craft.getHitBox();
        World w = craft.getW();
        for (MovecraftLocation location : hitBox) {
            int id = w.getBlockTypeIdAt(location.toBukkit(w));
            byte data = w.getBlockAt(location.getX(), location.getY(), location.getZ()).getData();
            returnSet.add(new BaseBlock(id, data));
        }
        if (Settings.Debug) {
            Bukkit.getLogger().info(returnSet.toString());
        }
        return returnSet;
    }

    @NotNull
    private BitmapHitBox solidBlockLocs(World w, @NotNull CuboidRegion cr){
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

    public boolean saveChunk(Chunk c, @NotNull File directory, @Nullable HashSet<Material> materialMask) {
        if(!directory.exists())
            directory.mkdirs();

        // Load chunk if not loaded already
        if(!c.isLoaded())
            c.load();

        Vector minPos = new Vector(c.getX() * 16, 0, c.getZ() * 16);
        Vector maxPos = new Vector((c.getX() * 16) + 15, 255, (c.getZ() * 16) + 15);
        com.sk89q.worldedit.world.World world = new BukkitWorld(c.getWorld());
        CuboidRegion region = new CuboidRegion(world, minPos, maxPos);

        // Save chunk to disk
        File file = new File(directory, c.getX() + "_" + c.getZ() + ".schematic");
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
            ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(file, false));
            writer.write(clipboard, world.getWorldData());
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
        File file = new File(directory, c.getX() + "_" + c.getZ() + ".schematic");
        Clipboard clipboard;
        World w = c.getWorld();
        try {
            WorldData worldData = (new BukkitWorld(w)).getWorldData();
            clipboard = ClipboardFormat.SCHEMATIC.getReader(new FileInputStream(file)).read(worldData);
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
                    Vector ccloc = new Vector(x, y, z);
                    BaseBlock bb = clipboard.getBlock(ccloc);
                    if (bb.isAir())
                        continue; // most blocks will be air, quickly move on to the next. This loop will run millions of times, needs to be fast
                    if (!w.getBlockAt(x, y, z).isEmpty() && !w.getBlockAt(x, y, z).isLiquid())
                        continue; // Don't replace blocks which aren't liquid or air

                    MovecraftLocation moveloc = new MovecraftLocation(x, y, z);
                    if(!p.test(moveloc))
                        continue;

                    WE6UpdateCommand updateCommand = new WE6UpdateCommand(bb, w, moveloc, Material.getMaterial(bb.getType()), (byte) bb.getData());
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
            BaseBlock baseBlock = clipboard.getBlock(new Vector(cLoc.getX(),cLoc.getY(),cLoc.getZ()));
            Material type =  Material.getMaterial(baseBlock.getType());
            if (fragileBlock(type)) {
                WE6UpdateCommand updateCommand = new WE6UpdateCommand(baseBlock, world, moveLoc, type, (byte) baseBlock.getData());
                updateCommandsFragileBlocks.add(updateCommand);
            } else {
                WE6UpdateCommand updateCommand = new WE6UpdateCommand(baseBlock, world, moveLoc, type, (byte) baseBlock.getData());
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
                || type.equals(Material.WALL_SIGN)
                || type.equals(Material.WALL_BANNER)
                || type.equals(Material.REDSTONE_WIRE)
                || type.equals(Material.LADDER)
                || type.equals(Material.BED_BLOCK)
                || type.equals(Material.TRIPWIRE_HOOK)
                || type.equals(Material.TORCH)
                || type.equals(Material.REDSTONE_TORCH_OFF)
                || type.equals(Material.REDSTONE_TORCH_ON);
    }

    private class MaterialMask implements Mask {
        private final HashSet<Material> materialMask;
        private final World w;

        public MaterialMask(HashSet<Material> materialMask, World w) {
            this.materialMask = materialMask;
            this.w = w;
        }

        @Override
        public boolean test(Vector vector) {
            Block b = w.getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
            return materialMask.contains(b.getType());
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return null;
        }
    }
}
