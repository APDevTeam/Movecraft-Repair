package net.countercraft.movecraft.repair.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import com.sk89q.worldedit.EditSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.enginehub.linbus.tree.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
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
    public static final ClipboardFormat SCHEMATIC_FORMAT = BuiltInClipboardFormat.SPONGE_SCHEMATIC;

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
            EditSession source = WorldEdit.getInstance().newEditSession(world);
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
            source.close();
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

        LinListTag<?> blockItems;
        try {
            blockItems = blockNBT.getListTag("Items", LinTagType.compoundTag());
        } catch (NoSuchElementException e) {
            return null;
        }
        for (var t : blockItems.value()) {
            if (!(t instanceof LinCompoundTag ct))
                continue;

            LinStringTag id;
            try {
                id = ct.getTag("id", LinTagType.stringTag());
            } catch (NoSuchElementException e) {
                continue;
            }

            Material material = getMaterial(id.value());
            if (material == null)
                continue;

            LinIntTag count;
            try {
                count = ct.getTag("count", LinTagType.intTag());
            } catch (NoSuchElementException e) {
                continue;
            }

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
    public static Component[] getBlockSignLines(@NotNull BaseBlock block, Side side) {
        LinCompoundTag blockNBT = block.getNbt();
        if (blockNBT == null) {
            return null;
        }

        LinCompoundTag text;
        try {
            text = switch (side) {
                case FRONT -> blockNBT.getTag("front_text", LinTagType.compoundTag());
                case BACK -> blockNBT.getTag("back_text", LinTagType.compoundTag());
            };
        }
        catch (NoSuchElementException e) {
            return null;
        }

        return getSideSignLines(text);
    }

    @Nullable
    private static Component[] getSideSignLines(@NotNull LinCompoundTag text) {
        List<? extends LinTag<?>> messages;
        try {
            messages = text.getListTag("messages", LinTagType.stringTag()).value();
        }
        catch (NoSuchElementException e) {
            return null;
        }

        return messages.stream().map(WEUtils::getSignLine).toArray(Component[]::new);
    }

    @NotNull
    private static Component getSignLine(@NotNull LinTag<?> message) {
        if (!(message instanceof LinStringTag stringTag))
            return Component.text("");
        return GsonComponentSerializer.gson().deserializeOr(stringTag.value(), Component.text(""));
    }
}
