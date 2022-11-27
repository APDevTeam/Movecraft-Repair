package net.countercraft.movecraft.repair.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
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

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;

public class WEUtils {
    private static final ClipboardFormat SCHEMATIC_FORMAT = BuiltInClipboardFormat.SPONGE_SCHEMATIC;

    /**
     * Load a schematic from disk
     * 
     * @param directory Directory for the schematic file
     * @param name      Name of the schematic file (without the extension)
     * @return A clipboard containing the schematic
     * @throws FileNotFoundException Schematic file not found
     * @throws IOException
     */
    @Nullable
    public static Clipboard loadSchematic(File directory, String name) throws IOException {
        name += SCHEMATIC_FORMAT.getPrimaryFileExtension();
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
        if (!repairDirectory.exists())
            repairDirectory.mkdirs();
        File playerDirectory = new File(repairDirectory, craft.getPilot().getUniqueId().toString());
        String repairName = ChatColor.stripColor(sign.getLine(1));
        repairName += SCHEMATIC_FORMAT.getPrimaryFileExtension();
        File repairFile = new File(playerDirectory, repairName);

        HitBox hitbox = craft.getHitBox();
        BlockVector3 minPos = BlockVector3.at(hitbox.getMinX(), hitbox.getMinY(), hitbox.getMinZ());
        BlockVector3 maxPos = BlockVector3.at(hitbox.getMaxX(), hitbox.getMaxY(), hitbox.getMaxZ());
        BlockVector3 origin = BlockVector3.at(sign.getX(), sign.getY(), sign.getZ());
        CuboidRegion region = new CuboidRegion(minPos, maxPos);
        // Calculate a hitbox of all blocks within the cuboid region but not within the hitbox (so we don't save them)
        HitBox surrounding = new SolidHitBox(
            new MovecraftLocation(hitbox.getMinX(), hitbox.getMinY(), hitbox.getMinZ()),
            new MovecraftLocation(hitbox.getMaxX(), hitbox.getMaxY(), hitbox.getMaxZ())
        );
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
                    BlockTypes.AIR.getDefaultState().toBaseBlock()
                );
            }
            ClipboardWriter writer = SCHEMATIC_FORMAT.getWriter(new FileOutputStream(repairFile, false));
            writer.write(clipboard);
            writer.close();
        }
        catch (IOException | NullPointerException | WorldEditException e) {
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
    public static Counter<Material> getBlockContents(BaseBlock block) {
        Counter<Material> counter = new Counter<>();
        ListTag blockItems = block.getNbtData().getListTag("Items");
        if (blockItems == null)
            return null;

        for (Tag t : blockItems.getValue()) {
            if (!(t instanceof CompoundTag))
                continue;

            CompoundTag ct = (CompoundTag) t;
            String id = ct.getString("id");
            BlockType type = new BlockType(id);
            Material material = BukkitAdapter.adapt(type);

            byte count = ct.getByte("count");

            counter.add(material, (int) count);
        }
        return counter;
    }
}
