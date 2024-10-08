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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;

public class WEUtils {
    // Default format is the first one of the list, previous formats follow
    public static final List<ClipboardFormat> SCHEMATIC_FORMATS = List.of(BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC, BuiltInClipboardFormat.SPONGE_V2_SCHEMATIC, BuiltInClipboardFormat.MCEDIT_SCHEMATIC);

    /**
     * Load a schematic from disk
     *
     * @param directory Directory for the schematic file
     * @param name      Name of the schematic file (without the extension)
     * @return A clipboard containing the schematic
     * @throws FileNotFoundException Schematic file not found
     */
    @NotNull
    public static Clipboard loadSchematic(File directory, String name) throws FileNotFoundException {
        Clipboard result = null;
        for (ClipboardFormat format : SCHEMATIC_FORMATS) {
            Clipboard temp;
            try {
                temp = loadSchematic(directory, name, format);
            } catch (FileNotFoundException e) {
                continue; // normal operation
            } catch (IOException e) {
                // Abnormal, but report to console and continue reading
                e.printStackTrace();
                continue;
            }
            if (temp == null)
                continue;

            result = temp;
            break;
        }

        if (result == null)
            throw new FileNotFoundException();

        return result;
    }

    @Nullable
    private static Clipboard loadSchematic(File directory, String name, @NotNull ClipboardFormat format) throws IOException {
        name += "." + format.getPrimaryFileExtension();
        File file = new File(directory, name);
        if (!format.isFormat(file))
            return null;
        Clipboard clipboard;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            ClipboardReader reader = format.getReader(inputStream);
            clipboard = reader.read();
        } catch (FileNotFoundException e) {
            // Normal operation, pass the exception up
            throw e;
        } catch (IOException e) {
            // Abnormal, add more logging info
            throw new IOException("Failed to load " + directory.getName() + "/" + name + " as format " + format.getName(), e);
        }
        return clipboard;
    }

    /**
     * Save a schematic
     *
     * @param directory Directory for the schematic file
     * @param name      Name of the schematic file (without the extension)
     * @param clipboard The clipboard to save from
     */
    public static boolean saveSchematic(File directory, String name, Clipboard clipboard) {
        File file = new File(directory, name + "." + SCHEMATIC_FORMATS.getFirst().getPrimaryFileExtension());
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            ClipboardWriter writer = SCHEMATIC_FORMATS.getFirst().getWriter(outputStream);
            writer.write(clipboard);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Save a schematic from a craft
     *
     * @param directory Directory to save in
     * @param name Name to save as
     * @param world World to save from
     * @param hitbox Hitbox to save from
     * @param origin Origin point to save from
     * @return true on success
     */
    public static boolean saveCraftSchematic(File directory, String name, World world, @NotNull HitBox hitbox, @NotNull Location origin) {
        BlockVector3 minPos = BlockVector3.at(hitbox.getMinX(), hitbox.getMinY(), hitbox.getMinZ());
        BlockVector3 maxPos = BlockVector3.at(hitbox.getMaxX(), hitbox.getMaxY(), hitbox.getMaxZ());
        BlockVector3 weOrigin = BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
        CuboidRegion region = new CuboidRegion(minPos, maxPos);
        // Calculate a hitbox of all blocks within the cuboid region but not within the
        // hitbox (so we don't save them)
        HitBox surrounding = new SolidHitBox(
                new MovecraftLocation(hitbox.getMinX(), hitbox.getMinY(), hitbox.getMinZ()),
                new MovecraftLocation(hitbox.getMaxX(), hitbox.getMaxY(), hitbox.getMaxZ()));
        surrounding = new BitmapHitBox(surrounding).difference(hitbox);

        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);

        Set<BaseBlock> blocks = getWorldEditBlocks(hitbox, world);

        Clipboard clipboard;
        try {
            clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(weOrigin);
            EditSession source = WorldEdit.getInstance().newEditSession(weWorld);
            ForwardExtentCopy copy = new ForwardExtentCopy(source, region, weOrigin, clipboard, weOrigin);
            BlockMask mask = new BlockMask(source, blocks);
            copy.setSourceMask(mask);
            Operations.complete(copy);
            for (MovecraftLocation location : surrounding) {
                clipboard.setBlock(
                        BlockVector3.at(location.getX(), location.getY(), location.getZ()),
                        BlockTypes.AIR.getDefaultState().toBaseBlock());
            }
            source.close();
        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }

        return saveSchematic(directory, name, clipboard);
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
        } catch (IllegalStateException e) {
            return null; // empty list
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
