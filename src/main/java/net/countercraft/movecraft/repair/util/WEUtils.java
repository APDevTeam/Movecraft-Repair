package net.countercraft.movecraft.repair.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;

import net.countercraft.movecraft.repair.types.MaterialCounter;

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

    @Nullable
    public static MaterialCounter getBlockContents(BaseBlock block) {
        MaterialCounter counter = new MaterialCounter();
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
