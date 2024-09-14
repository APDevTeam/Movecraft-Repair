package net.countercraft.movecraft.repair.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.enginehub.linbus.tree.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;

public class WEUtils {
    public static final ClipboardFormat SCHEMATIC_FORMAT = BuiltInClipboardFormat.SPONGE_V2_SCHEMATIC;

    /**
     * Load a schematic from disk
     *
     * @param directory Directory for the schematic file
     * @param name      Name of the schematic file (without the extension)
     * @return A clipboard containing the schematic
     * @throws FileNotFoundException Schematic file not found
     * @throws IOException           Other I/O exception
     */
    @NotNull
    public static Clipboard loadSchematic(File directory, String name) throws FileNotFoundException, IOException {
        name += "." + SCHEMATIC_FORMAT.getPrimaryFileExtension();
        File file = new File(directory, name);
        Clipboard clipboard;
        try {
            ClipboardReader reader = SCHEMATIC_FORMAT.getReader(new FileInputStream(file));
            clipboard = reader.read();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException("Failed to load schematic", e);
        }
        return clipboard;
    }

    /**
     * Save a schematic from a craft
     *
     * @param craft The craft to save
     * @return true on success
     */
    public static boolean saveCraftSchematic(@NotNull PilotedCraft craft, @NotNull Sign sign) {
        File repairDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(repairDirectory, craft.getPilot().getUniqueId().toString());
        if (!playerDirectory.exists())
            playerDirectory.mkdirs();
        String repairName = ChatColor.stripColor(sign.getLine(1));
        repairName += "." + SCHEMATIC_FORMAT.getPrimaryFileExtension();
        File repairFile = new File(playerDirectory, repairName);

        HitBox hitbox = craft.getHitBox();
        BlockVector3 minPos = BlockVector3.at(hitbox.getMinX(), hitbox.getMinY(), hitbox.getMinZ());
        BlockVector3 maxPos = BlockVector3.at(hitbox.getMaxX(), hitbox.getMaxY(), hitbox.getMaxZ());
        BlockVector3 origin = BlockVector3.at(sign.getX(), sign.getY(), sign.getZ());
        CuboidRegion region = new CuboidRegion(minPos, maxPos);
        // Calculate a hitbox of all blocks within the cuboid region but not within the
        // hitbox (so we don't save them)
        HitBox surrounding = new SolidHitBox(
                new MovecraftLocation(hitbox.getMinX(), hitbox.getMinY(), hitbox.getMinZ()),
                new MovecraftLocation(hitbox.getMaxX(), hitbox.getMaxY(), hitbox.getMaxZ()));
        surrounding = new BitmapHitBox(surrounding).difference(hitbox);

        World bukkitWorld = craft.getWorld();
        com.sk89q.worldedit.world.World world = new BukkitWorld(bukkitWorld);

        Set<BaseBlock> blocks = getWorldEditBlocks(craft.getHitBox(), bukkitWorld);

        try {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(origin);
            Extent source = WorldEdit.getInstance().newEditSession(world);
            ForwardExtentCopy copy = new ForwardExtentCopy(source, region, origin, clipboard, origin);
            BlockMask mask = new BlockMask(source, blocks);
            copy.setSourceMask(mask);
            Operations.complete(copy);
            for (MovecraftLocation location : surrounding) {
                clipboard.setBlock(
                        BlockVector3.at(location.getX(), location.getY(), location.getZ()),
                        BlockTypes.AIR.getDefaultState().toBaseBlock());
            }
            ClipboardWriter writer = SCHEMATIC_FORMAT.getWriter(new FileOutputStream(repairFile, false));
            writer.write(clipboard);
            writer.close();
        } catch (IOException | NullPointerException | WorldEditException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @NotNull
    private static Set<BaseBlock> getWorldEditBlocks(@NotNull HitBox hitbox, @NotNull World world) {
        Set<BaseBlock> result = new HashSet<>();
        for (MovecraftLocation location : hitbox) {
            BlockData data = world.getBlockAt(location.toBukkit(world)).getBlockData();
            result.add(BukkitAdapter.adapt(data).toBaseBlock());
        }
        return result;
    }

    /**
     * Get the contents of a WorldEdit block
     *
     * @param block block to check
     * @return Counter of the materials in the block
     */
    @Nullable
    public static Counter<Material> getBlockContents(@NotNull BaseBlock block) {
        Counter<Material> counter = new Counter<>();
        LinCompoundTag blockNBT = block.getNbt();
        if (blockNBT == null)
            return null;

        LinListTag<?> blockItems = blockNBT.getListTag("Items", LinTagType.compoundTag());
        for (var t : blockItems.value()) {
            if (!(t instanceof LinCompoundTag ct))
                continue;

            LinStringTag id = ct.getTag("id", LinTagType.stringTag());
            Material material = getMaterial(id.value());
            LinByteTag count = ct.getTag("Count", LinTagType.byteTag());
            if (material == null)
                continue;

            counter.add(material, count.value());
        }
        return counter;
    }

    @Nullable
    private static Material getMaterial(String str) {
        BlockType block = BlockTypes.get(str);
        if (block != null)
            return BukkitAdapter.adapt(block);

        ItemType item = ItemTypes.get(str);
        if (item != null)
            return BukkitAdapter.adapt(item);

        return null;
    }

    /**
     * Get the sign contents of a WorldEdit block
     *
     * @param block block to check
     * @return Array of sign lines in the block
     */
    @Nullable
    public static String[] getBlockSignLines(@NotNull BaseBlock block) {
        LinCompoundTag blockNBT = block.getNbt();
        if (blockNBT == null)
            return null;

        String[] result = new String[8];
        for (int i = 0; i < result.length; i++) {
            result[i] = getSignTextFromJSON(blockNBT.getTag("Text" + i, LinTagType.stringTag()).value());
        }
        return result;
    }

    private static final String[] TEXT_STYLES = {"bold", "italic", "underline", "strikethrough"};

    @NotNull
    private static String getSignTextFromJSON(String json) {
        try {
            Gson gson = new Gson();
            Map<?, ?> lineData = gson.fromJson(json, Map.class);
            String result = "";
            if (lineData == null)
                return result;

            result += getSignTextFromMap(lineData);
            if (!lineData.containsKey("extra"))
                return result;

            Object extrasObject = lineData.get("extra");
            if (!(extrasObject instanceof List<?> extras))
                return result;

            StringBuilder builder = new StringBuilder();
            for (Object componentObject : extras) {
                if (!(componentObject instanceof Map))
                    continue;

                builder.append(getSignTextFromMap((Map<?, ?>) componentObject));
            }
            result += builder.toString();

            return result;
        } catch (Exception e) {
            MovecraftRepair.getInstance().getLogger().severe("Got exception when parsing '" + json + "'");
            e.printStackTrace();
            return "";
        }
    }

    private static @NotNull String getSignTextFromMap(@NotNull Map<?, ?> component) {
        StringBuilder builder = new StringBuilder();
        if (component.containsKey("color")) {
            builder.append(ChatColor.valueOf(((String) component.get("color")).toUpperCase()));
        }
        for (String style : TEXT_STYLES) {
            if (component.containsKey(style)) {
                builder.append(ChatColor.valueOf(((String) component.get(style)).toUpperCase()));
            }
        }
        builder.append(component.get("text"));
        return builder.toString();
    }
}
