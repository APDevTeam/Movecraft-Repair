package net.countercraft.movecraft.repair.util;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.repair.config.Config;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class WarfareUtils {
    public boolean repairChunk(Chunk chunk, File directory, Predicate<MovecraftLocation> check) {
        // Load schematic from disk
        File file = new File(directory,
                chunk.getX() + "_" + chunk.getZ() + "." + WEUtils.SCHEMATIC_FORMAT.getPrimaryFileExtension());
        Clipboard clipboard;
        World world = chunk.getWorld();
        try {
            clipboard = WEUtils.SCHEMATIC_FORMAT.getReader(new FileInputStream(file)).read();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Load chunk if not loaded already
        if (!chunk.isLoaded())
            chunk.load();

        // Repair chunk
        for (int x = clipboard.getMinimumPoint().getBlockX(); x <= clipboard.getMaximumPoint().getBlockX(); x++) {
            for (int y = clipboard.getMinimumPoint().getBlockY(); y <= clipboard.getMaximumPoint().getBlockY(); y++) {
                for (int z = clipboard.getMinimumPoint().getBlockZ(); z <= clipboard.getMaximumPoint()
                        .getBlockZ(); z++) {
                    BlockVector3 clipboardLocation = BlockVector3.at(x, y, z);
                    BaseBlock baseBlock = clipboard.getFullBlock(clipboardLocation);
                    Material material = BukkitAdapter.adapt(baseBlock.getBlockType());
                    if (material.isAir())
                        continue; // most blocks will be air, quickly move on to the next. This loop will run
                                  // millions of times and needs to be fast
                    if (Config.RepairBlackList.contains(material))
                        continue;
                    if (!world.getBlockAt(x, y, z).isEmpty() && !world.getBlockAt(x, y, z).isLiquid())
                        continue; // Don't replace blocks which aren't liquid or air

                    MovecraftLocation location = new MovecraftLocation(x, y, z);
                    if (!check.test(location))
                        continue;

                    WorldManager.INSTANCE.submit(new BlockTask(location.toBukkit(world), BukkitAdapter.adapt(baseBlock)));
                }
            }
        }
        return true;
    }

    public boolean saveChunk(Chunk c, File directory, @Nullable Set<Material> materialMask) {
        if (!directory.exists())
            directory.mkdirs();

        // Load chunk if not loaded already
        if (!c.isLoaded())
            c.load();

        BlockVector3 minPos = BlockVector3.at(c.getX() * 16, 0, c.getZ() * 16);
        BlockVector3 maxPos = BlockVector3.at((c.getX() * 16) + 15, 255, (c.getZ() * 16) + 15);
        com.sk89q.worldedit.world.World world = new BukkitWorld(c.getWorld());
        CuboidRegion region = new CuboidRegion(world, minPos, maxPos);

        // Copy chunk into memory
        Set<BaseBlock> baseBlockSet = new HashSet<>();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    Block b = c.getBlock(x, y, z);
                    if (b.getType().equals(Material.AIR))
                        continue;

                    // A null materialMask will be understood as saving every block
                    if (materialMask == null || !materialMask.contains(b.getType()))
                        continue;

                    baseBlockSet.add(BukkitAdapter.adapt(b.getBlockData()).toBaseBlock());
                }
            }
        }

        // Save chunk to disk
        File file = new File(directory, c.getX() + "_" + c.getZ() + "." + WEUtils.SCHEMATIC_FORMAT.getPrimaryFileExtension());
        try {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            Extent source = WorldEdit.getInstance().newEditSessionBuilder().world(world).maxBlocks(16 * 16 * (world.getMaxY() - world.getMinY() + 2)).build(); // Get enough space for the chunk, with a little extra wiggle room
            ForwardExtentCopy copy = new ForwardExtentCopy(source, region, clipboard.getOrigin(), clipboard, minPos);
            if (materialMask != null) {
                // A null materialMask will be understood as saving every block
                Mask mask = new MaterialMask(materialMask, c.getWorld());
                copy.setSourceMask(mask);
            }
            Operations.completeLegacy(copy);
            ClipboardWriter writer = WEUtils.SCHEMATIC_FORMAT.getWriter(new FileOutputStream(file, false));
            writer.write(clipboard);
            writer.close();
            return true;
        } catch (MaxChangedBlocksException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class BlockTask implements Supplier<Effect> {
        private final Location location;
        private final BlockData blockData;

        public BlockTask(Location location, BlockData blockData) {
            this.location = location;
            this.blockData = blockData;
        }

        public Effect get() {
            return () -> Movecraft.getInstance().getWorldHandler().setBlockFast(location, blockData);
        }
    }

    private static class MaterialMask implements Mask {
        private final Set<Material> materials;
        private final World world;

        public MaterialMask(Set<Material> materials, World world) {
            this.materials = materials;
            this.world = world;
        }

        @Override
        public boolean test(BlockVector3 vector) {
            Block block = world.getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
            return materials.contains(block.getType());
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return null;
        }
    }
}
