package net.countercraft.movecraft.repair.types;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BaseBlock;

import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.RepairBlobManager;
import net.countercraft.movecraft.repair.events.ProtoRepairCreateEvent;
import net.countercraft.movecraft.repair.tasks.BlockRepair;
import net.countercraft.movecraft.repair.tasks.InventoryRepair;
import net.countercraft.movecraft.repair.tasks.SignRepair;
import net.countercraft.movecraft.repair.util.ClipboardUtils;
import net.countercraft.movecraft.repair.util.RepairUtils;
import net.countercraft.movecraft.repair.util.RotationUtils;
import net.countercraft.movecraft.repair.util.WEUtils;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Pair;

public class RepairState {
    private UUID playerUUID;
    private String name;
    private Clipboard schematic;
    private BlockVector3 schematicMinPos;
    private BlockVector3 schematicSignOffset;
    private BlockVector3 size;

    public RepairState(UUID playerUUID, String name) throws IOException, IllegalStateException {
        this.playerUUID = playerUUID;
        this.name = name;
        File dataDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(dataDirectory, playerUUID.toString());
        if (!playerDirectory.exists() && !playerDirectory.mkdirs())
            throw new IllegalStateException("Unable to create player directory");

        schematic = WEUtils.loadSchematic(playerDirectory, name);
        schematicMinPos = schematic.getMinimumPoint();
        schematicSignOffset = schematic.getOrigin().subtract(schematicMinPos);
        size = schematic.getDimensions();
    }

    public UUID getUUID() {
        return playerUUID;
    }

    public String getName() {
        return name;
    }

    private Clipboard rotate(Sign sign) throws WorldEditException {
        BlockVector3 signPosition = BlockVector3.at(sign.getX(), sign.getY(), sign.getZ());

        BlockVector3 offset = signPosition.subtract(schematicSignOffset);
        BlockVector3 schematicSignPosition = signPosition.subtract(offset).add(schematicMinPos);
        BaseBlock schematicSign = schematic.getFullBlock(schematicSignPosition);
        BlockFace schematicSignFacing = RotationUtils.getRotation(schematicSign);

        BlockFace worldSignFacing = RotationUtils.getRotation(sign.getBlock());

        int angle = RotationUtils.angleBetweenBlockFaces(worldSignFacing, schematicSignFacing);

        return ClipboardUtils.transform(schematic, new AffineTransform().rotateY(angle));
    }

    @Nullable
    public ProtoRepair execute(Sign sign) throws WorldEditException {
        // Rotate repair around the sign
        Clipboard clipboard = schematic;//rotate(sign);

        // Gather the required materials and tasks
        World world = sign.getWorld();
        RepairCounter materials = new RepairCounter();
        RepairQueue queue = new RepairQueue();
        Map<Location, BlockRepair> blockRepairs = new HashMap<>();
        int damagedBlockCount = 0;
        Location worldMinPos = sign.getLocation().subtract(schematicSignOffset.getBlockX(), schematicSignOffset.getBlockY(), schematicSignOffset.getBlockZ());
        for (int x = 0; x < size.getBlockX(); x++) {
            for (int z = 0; z < size.getBlockZ(); z++) {
                for (int y = 0; y < size.getBlockY(); y++) {
                    BlockVector3 schematicPosition = schematicMinPos.add(x, y, z);
                    BaseBlock schematicBlock = clipboard.getFullBlock(schematicPosition);
                    Material schematicMaterial = BukkitAdapter.adapt(schematicBlock.getBlockType());
                    BlockData schematicData = BukkitAdapter.adapt(schematicBlock);

                    Location worldPosition = new Location(world, x, y, z).add(worldMinPos);
                    Block worldBlock = worldPosition.getBlock();
                    Material worldMaterial = worldBlock.getType();
                    BlockState worldState = worldBlock.getState();

                    // Handle block repair
                    BlockRepair blockRepair = null;
                    if (RepairUtils.needsBlockRepair(schematicMaterial, worldMaterial)) {
                        blockRepair = new BlockRepair(worldPosition, schematicData);
                        queue.add(blockRepair);
                        blockRepairs.put(worldPosition, blockRepair);
                        damagedBlockCount++;

                        // Handle dependent block repairs
                        Location dependency = RepairUtils.getDependency(schematicMaterial, schematicData, worldPosition);
                        if (dependency != null) {
                            // Found a dependency, set the block repair's dependency to the respective block repair
                            blockRepair.setDependency(blockRepairs.get(dependency));
                        }

                        Material requiredMaterial = RepairUtils.remapMaterial(schematicMaterial);
                        if (requiredMaterial == Material.AIR)
                            continue;

                        materials.add(RepairBlobManager.get(requiredMaterial), RepairUtils.blockCost(requiredMaterial));
                    }

                    // Handle sign repair
                    if (Tag.SIGNS.isTagged(schematicMaterial) && blockRepair != null) {
                        String[] lines = WEUtils.getBlockSignLines(schematicBlock);
                        SignRepair signRepair = new SignRepair(worldPosition, lines);
                        signRepair.setDependency(blockRepair);
                        queue.add(signRepair);
                    }

                    // Handle inventory repair
                    Counter<Material> schematicContents = WEUtils.getBlockContents(schematicBlock);
                    Pair<Boolean, Counter<Material>> inventoryRepair = RepairUtils.checkInventoryRepair(worldMaterial, worldState, schematicContents);
                    if (!inventoryRepair.getLeft())
                        continue;

                    for (Material m : inventoryRepair.getRight().getKeySet()) {
                        Material requiredMaterial = RepairUtils.remapMaterial(m);
                        if (requiredMaterial == Material.AIR)
                            continue;

                        materials.add(RepairBlobManager.get(requiredMaterial), inventoryRepair.getRight().get(m));
                    }
                    addInventoryTasks(queue, blockRepair, worldPosition, inventoryRepair.getRight());
                }
            }
        }

        ProtoRepair result = new ProtoRepair(playerUUID, name, queue, materials, damagedBlockCount, MathUtils.bukkit2MovecraftLoc(sign.getLocation()));

        ProtoRepairCreateEvent event = new ProtoRepairCreateEvent(result);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return null;

        return result;
    }

    private void addInventoryTasks(RepairQueue tasks, @Nullable BlockRepair blockRepair, Location location, Counter<Material> counter) {
        for (Material m : counter.getKeySet()) {
            ItemStack items = new ItemStack(m, counter.get(m));
            InventoryRepair inventoryRepair = new InventoryRepair(location, items);
            inventoryRepair.setDependency(blockRepair);
            tasks.add(inventoryRepair);
        }
    }
}
