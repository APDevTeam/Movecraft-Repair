package net.countercraft.movecraft.repair.utils;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Chunk;
import org.bukkit.Material;
import net.countercraft.movecraft.utils.*;
import org.bukkit.block.Sign;
import com.sk89q.worldedit.Vector;

import java.io.File;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;

public abstract class WEUtils {
    public WEUtils() {
    }

    public abstract boolean saveCraftRepairState(Craft craft, Sign sign);

    public abstract Clipboard loadCraftRepairStateClipboard(Craft craft, Sign sign);

    public abstract HashMap<Pair<Material, Byte>, Double> getMissingBlocks(String repairName);

    public abstract ArrayDeque<Pair<Vector, Vector>> getMissingBlockLocations(String repairName);

    public abstract long getNumDiffBlocks(String repairName);

    public abstract boolean saveChunk(Chunk c, File directory, HashSet<Material> materialMask);
}
