package net.countercraft.movecraft.repair.types;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BaseBlock;

import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.RepairBlobManager;
import net.countercraft.movecraft.repair.tasks.BlockRepair;
import net.countercraft.movecraft.repair.tasks.InventoryRepair;
import net.countercraft.movecraft.repair.util.ClipboardUtils;
import net.countercraft.movecraft.repair.util.RepairUtils;
import net.countercraft.movecraft.repair.util.RotationUtils;
import net.countercraft.movecraft.repair.util.WEUtils;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Pair;

public class RepairState {
    private UUID uuid;
    private String name;
    private Clipboard schematic;
    private BlockVector3 schematicMinPos;
    private BlockVector3 schematicSignOffset;
    private BlockVector3 size;

    public RepairState(UUID uuid, String name) throws IOException, IllegalStateException {
        this.uuid = uuid;
        this.name = name;
        File dataDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(dataDirectory, uuid.toString());
        if (!playerDirectory.exists())
            throw new IllegalStateException("Unable to create player directory");

        schematic = WEUtils.loadSchematic(playerDirectory, name);
        schematicMinPos = schematic.getMinimumPoint();
        schematicSignOffset = schematic.getOrigin().subtract(schematicMinPos);
        size = schematic.getDimensions();
    }

    public UUID getUUID() {
        return uuid;
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

    public ProtoRepair execute(Sign sign) throws WorldEditException {
        // Rotate repair around the sign
        Clipboard clipboard = schematic;//rotate(sign);

        // Gather the required materials and tasks
        World world = sign.getWorld();
        RepairCounter materials = new RepairCounter();
        RepairQueue queue = new RepairQueue();
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
                    if (RepairUtils.needsBlockRepair(schematicMaterial, worldMaterial)) {
                        Material requiredMaterial = RepairUtils.remapMaterial(schematicMaterial);
                        materials.add(RepairBlobManager.get(requiredMaterial), RepairUtils.blockCost(requiredMaterial));
                        queue.add(new BlockRepair(worldPosition, schematicData));
                        damagedBlockCount++;
                    }

                    // Handle inventory repair
                    Counter<Material> schematicContents = WEUtils.getBlockContents(schematicBlock);
                    Pair<Boolean, Counter<Material>> inventoryRepair = RepairUtils.checkInventoryRepair(worldMaterial, worldState, schematicContents);
                    if (!inventoryRepair.getLeft())
                        continue;

                    for (Material m : inventoryRepair.getRight().getKeySet()) {
                        Material requiredMaterial = RepairUtils.remapMaterial(m);
                        materials.add(RepairBlobManager.get(requiredMaterial), inventoryRepair.getRight().get(m));
                    }
                    addInventoryTasks(queue, worldPosition, inventoryRepair.getRight());
                }
            }
        }

        // TODO: Do stuff with repair blobs

        return new ProtoRepair(uuid, queue, materials, damagedBlockCount, MathUtils.bukkit2MovecraftLoc(sign.getLocation()));
    }

    private void addInventoryTasks(RepairQueue tasks, Location location, Counter<Material> counter) {
        for (Material m : counter.getKeySet()) {
            ItemStack items = new ItemStack(m, counter.get(m));
            tasks.add(new InventoryRepair(location, items));
        }
    }
}
