package net.countercraft.movecraft.repair.utils;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.util.Pair;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Predicate;

public abstract class WEUtils {
    protected final HashMap<String, ArrayDeque<MovecraftRepairLocation>> locMissingBlocksMap = new HashMap<>();
    protected final HashMap<String, Long> numDiffBlocksMap = new HashMap<>();
    protected final HashMap<String, HashMap<Pair<Material, Byte>, Double>> missingBlocksMap = new HashMap<>();
    protected final File dataFolder;
    public WEUtils(Plugin movecraftRepair) {
        dataFolder = movecraftRepair.getDataFolder();
    }

    public abstract boolean saveCraftRepairState(PlayerCraft craft, Sign sign);

    public abstract Clipboard loadCraftRepairStateClipboard(PlayerCraft craft, Sign sign);

    public abstract HashMap<Pair<Material, Byte>, Double> getMissingBlocks(String repairName);

    public abstract ArrayDeque<MovecraftRepairLocation> getMissingBlockLocations(String repairName);

    public abstract long getNumDiffBlocks(String repairName);

    public abstract boolean saveChunk(Chunk c, File directory, @Nullable HashSet<Material> materialMask);

    public abstract boolean repairChunk(Chunk c, File directory, Predicate<MovecraftLocation> p);

    public abstract UpdateCommandsQueuePair getUpdateCommands(Clipboard clipboard, World world, ArrayDeque<MovecraftRepairLocation> locMissingBlocks);
}
