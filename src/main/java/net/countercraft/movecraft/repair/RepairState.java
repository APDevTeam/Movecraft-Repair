package net.countercraft.movecraft.repair;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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

import net.countercraft.movecraft.repair.tasks.BlockRepair;
import net.countercraft.movecraft.repair.tasks.InventoryRepair;
import net.countercraft.movecraft.repair.tasks.RepairTask;
import net.countercraft.movecraft.repair.types.MaterialCounter;
import net.countercraft.movecraft.repair.util.ClipboardUtils;
import net.countercraft.movecraft.repair.util.RepairUtils;
import net.countercraft.movecraft.repair.util.RotationUtils;
import net.countercraft.movecraft.repair.util.WEUtils;
import net.countercraft.movecraft.util.Pair;

public class RepairState {
    private UUID uuid;
    private Clipboard schematic;
    private BlockVector3 minPos;
    private BlockVector3 worldOffset;
    private BlockVector3 size;

    public RepairState(UUID uuid, String name) throws IOException, IllegalStateException {
        this.uuid = uuid;
        File dataDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(dataDirectory, uuid.toString());
        if (!playerDirectory.exists())
            throw new IllegalStateException();

        schematic = WEUtils.loadSchematic(playerDirectory, name);
        minPos = schematic.getMinimumPoint();
        worldOffset = schematic.getOrigin().subtract(minPos);
        size = schematic.getDimensions();
    }

    private void rotate(Sign sign) throws WorldEditException {
        BlockVector3 signPosition = BlockVector3.at(sign.getX(), sign.getY(), sign.getZ());

        BlockVector3 offset = signPosition.subtract(worldOffset);
        BlockVector3 schematicSignPosition = signPosition.subtract(offset).add(minPos);
        BaseBlock schematicSign = schematic.getFullBlock(schematicSignPosition);
        BlockFace schematicSignFacing = RotationUtils.getRotation(schematicSign);

        BlockFace worldSignFacing = RotationUtils.getRotation(sign.getBlock());

        int angle = RotationUtils.angleBetweenBlockFaces(worldSignFacing, schematicSignFacing);

        schematic = ClipboardUtils.transform(schematic, new AffineTransform().rotateY(angle));
    }

    public Repair execute(Sign sign) throws WorldEditException {
        // Rotate repair around the sign
        rotate(sign);

        // Gather the required materials and tasks
        World world = sign.getWorld();
        MaterialCounter materials = new MaterialCounter();
        Set<RepairTask> tasks = new HashSet<>();
        for (int x = 0; x < size.getBlockX(); x++) {
            for (int z = 0; z < size.getBlockZ(); z++) {
                for (int y = 0; y < size.getBlockY(); y++) {
                    BlockVector3 schematicPosition = minPos.add(x, y, z);
                    BaseBlock schematicBlock = schematic.getFullBlock(schematicPosition);
                    Material schematicMaterial = BukkitAdapter.adapt(schematicBlock.getBlockType());
                    BlockData schematicData = BukkitAdapter.adapt(schematicBlock);

                    Location worldPosition = new Location(world, x, y, z);
                    Block worldBlock = worldPosition.getBlock();
                    Material worldMaterial = worldBlock.getType();
                    BlockState worldState = worldBlock.getState();

                    // Handle block repair
                    if (RepairUtils.needsBlockRepair(schematicMaterial, worldMaterial)) {
                        materials.add(schematicMaterial, 1);
                        tasks.add(new BlockRepair(worldPosition, schematicData));
                    }

                    // Handle inventory repair
                    MaterialCounter schematicContents = WEUtils.getBlockContents(schematicBlock);
                    Pair<Boolean, MaterialCounter> inventoryRepair = RepairUtils.checkInventoryRepair(worldMaterial,
                            worldState, schematicContents);
                    if (!inventoryRepair.getLeft())
                        continue;

                    for (ItemStack i : inventoryRepair.getRight()) {
                        materials.add(i.getType(), i.getAmount());
                        tasks.add(new InventoryRepair(worldPosition, i));
                    }
                }
            }
        }

        return null;
    }
}
