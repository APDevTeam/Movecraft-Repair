package net.countercraft.movecraft.repair.utils;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import org.bukkit.Chunk;
import org.bukkit.Material;
import net.countercraft.movecraft.utils.*;
import org.bukkit.World;
import org.bukkit.block.Sign;
import com.sk89q.worldedit.Vector;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.Predicate;

public abstract class WEUtils {
    protected final HashMap<String, ArrayDeque<Pair<MovecraftLocation, MovecraftLocation>>> locMissingBlocksMap = new HashMap<>();
    protected final HashMap<String, Long> numDiffBlocksMap = new HashMap<>();
    protected final HashMap<String, HashMap<Pair<Material, Byte>, Double>> missingBlocksMap = new HashMap<>();
    protected final File dataFolder;
    public WEUtils(Plugin movecraftRepair) {
        dataFolder = movecraftRepair.getDataFolder();
    }

    public abstract boolean saveCraftRepairState(Craft craft, Sign sign);

    public abstract Clipboard loadCraftRepairStateClipboard(Craft craft, Sign sign);

    public abstract HashMap<Pair<Material, Byte>, Double> getMissingBlocks(String repairName);

    public abstract ArrayDeque<Pair<MovecraftLocation, MovecraftLocation>> getMissingBlockLocations(String repairName);

    public abstract long getNumDiffBlocks(String repairName);

    public abstract boolean saveChunk(Chunk c, File directory, @Nullable HashSet<Material> materialMask);

    public abstract boolean repairChunk(Chunk c, File directory, Predicate<MovecraftLocation> p);

    public abstract Pair<LinkedList<UpdateCommand>, LinkedList<UpdateCommand>> getUpdateCommands(Clipboard clipboard, World world, ArrayDeque<Pair<MovecraftLocation, MovecraftLocation>> locMissingBlocks);
}
