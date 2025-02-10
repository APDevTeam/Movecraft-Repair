package net.countercraft.movecraft.repair.types;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.tasks.LecternRepair;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
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
import org.bukkit.block.sign.Side;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
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
    private final UUID playerUUID;
    private final String name;
    private final Clipboard schematic;

    public RepairState(@NotNull UUID playerUUID, String name) throws FileNotFoundException, IllegalStateException {
        this.playerUUID = playerUUID;
        this.name = name;
        File dataDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(dataDirectory, playerUUID.toString());
        if (!playerDirectory.exists() && !playerDirectory.mkdirs())
            throw new IllegalStateException("Unable to create player directory");

        schematic = WEUtils.loadSchematic(playerDirectory, name);
    }

    public UUID getUUID() {
        return playerUUID;
    }

    public String getName() {
        return name;
    }

    @NotNull
    private Clipboard rotate(@NotNull Sign sign) throws WorldEditException {
        BaseBlock schematicSign = schematic.getFullBlock(schematic.getOrigin());
        BlockFace schematicSignFacing = RotationUtils.getRotation(schematicSign);

        BlockFace worldSignFacing = RotationUtils.getRotation(sign.getBlock());

        int angle = RotationUtils.angleBetweenBlockFaces(worldSignFacing, schematicSignFacing);

        return ClipboardUtils.transform(schematic, new AffineTransform().rotateY(angle));
    }

    @NotNull
    public ProtoRepair execute(@NotNull Sign sign) throws WorldEditException, ProtoRepairCancelledException {
        // Rotate repair around the sign
        Clipboard clipboard = rotate(sign);

        // Gather the required materials and tasks
        RepairCounter materials = new RepairCounter();
        RepairQueue queue = new RepairQueue();
        Map<Location, BlockRepair> blockRepairs = new HashMap<>();
        int damagedBlockCount = 0;
        BitmapHitBox hitBox = new BitmapHitBox();

        // We use offsets from the sign (clipboard origin) to calculate actual coordinates
        // This is straightforward since we already know the position of the sign in both the schematic and the world
        BlockVector3 minOffset = clipboard.getMinimumPoint().subtract(clipboard.getOrigin());
        BlockVector3 maxOffset = clipboard.getMaximumPoint().subtract(clipboard.getOrigin());

        for (int x = minOffset.x(); x <= maxOffset.x(); x++) {
            for (int z = minOffset.z(); z <= maxOffset.z(); z++) {
                for (int y = minOffset.y(); y <= maxOffset.y(); y++) {
                    BlockVector3 schematicPosition = clipboard.getOrigin().add(x, y, z);
                    BaseBlock schematicBlock = clipboard.getFullBlock(schematicPosition);
                    Material schematicMaterial = BukkitAdapter.adapt(schematicBlock.getBlockType());
                    BlockData schematicData = BukkitAdapter.adapt(schematicBlock);

                    Location worldPosition = sign.getLocation().add(x, y, z);
                    Block worldBlock = worldPosition.getBlock();
                    Material worldMaterial = worldBlock.getType();
                    BlockState worldState = worldBlock.getState();

                    // Handle block repair
                    BlockRepair blockRepair = null;
                    if (RepairUtils.needsBlockRepair(schematicMaterial, worldMaterial)) {
                        blockRepair = new BlockRepair(worldPosition, schematicData);
                        queue.add(blockRepair);
                        blockRepairs.put(worldPosition, blockRepair);
                        hitBox.add(MathUtils.bukkit2MovecraftLoc(worldPosition));
                        damagedBlockCount++;

                        // Handle dependent block repairs
                        Location dependency = RepairUtils.getDependency(schematicMaterial, schematicData, worldPosition);
                        if (dependency != null) {
                            // Found a dependency, set the block repair's dependency to the respective block repair
                            blockRepair.setDependency(blockRepairs.get(dependency));
                        }

                        Material requiredMaterial = RepairUtils.remapMaterial(schematicMaterial);
                        if (requiredMaterial.isAir())
                            continue;

                        materials.add(RepairBlobManager.get(requiredMaterial), RepairUtils.blockCost(requiredMaterial));
                    }

                    // Handle sign repair
                    if (Tag.ALL_SIGNS.isTagged(schematicMaterial) && blockRepair != null) {
                        SignRepair signRepair = new SignRepair(
                                worldPosition,
                                WEUtils.getBlockSignLines(schematicBlock, Side.FRONT),
                                WEUtils.getBlockSignLines(schematicBlock, Side.BACK)
                        );
                        signRepair.setDependency(blockRepair);
                        queue.add(signRepair);
                    }

                    // Handle lectern repair
                    if (schematicMaterial == Material.LECTERN && blockRepair != null) {
                        LecternRepair lecternRepair = new LecternRepair(
                                worldPosition,
                                schematicBlock
                        );
                        lecternRepair.setDependency(blockRepair);
                        queue.add(lecternRepair);
                        materials.add(RepairBlobManager.get(Material.WRITABLE_BOOK), 1);
                        continue;
                    }

                    // Handle inventory repair
                    Counter<Material> schematicContents = WEUtils.getBlockContents(schematicBlock);
                    Pair<Boolean, Counter<Material>> inventoryRepair = RepairUtils.checkInventoryRepair(worldMaterial, worldState, schematicContents);
                    if (!inventoryRepair.getLeft())
                        continue;

                    hitBox.add(MathUtils.bukkit2MovecraftLoc(worldPosition));
                    for (Material m : inventoryRepair.getRight().getKeySet()) {
                        Material requiredMaterial = RepairUtils.remapMaterial(m);
                        if (requiredMaterial.isAir() || Config.RepairBlackList.contains(requiredMaterial))
                            continue;

                        materials.add(RepairBlobManager.get(requiredMaterial), inventoryRepair.getRight().get(m));
                    }
                    addInventoryTasks(queue, blockRepair, worldPosition, inventoryRepair.getRight());
                }
            }
        }

        ProtoRepair result = new ProtoRepair(playerUUID, name, queue, materials, damagedBlockCount, MathUtils.bukkit2MovecraftLoc(sign.getLocation()), sign.getWorld(), hitBox);

        ProtoRepairCreateEvent event = new ProtoRepairCreateEvent(result);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            throw new ProtoRepairCancelledException(event.getFailMessage());

        return result;
    }

    private void addInventoryTasks(RepairQueue tasks, @Nullable BlockRepair blockRepair, Location location, @NotNull Counter<Material> counter) {
        for (Material m : counter.getKeySet()) {
            if (Config.RepairBlackList.contains(m))
                continue;

            ItemStack items = new ItemStack(m, counter.get(m));
            InventoryRepair inventoryRepair = new InventoryRepair(location, items);
            inventoryRepair.setDependency(blockRepair);
            tasks.add(inventoryRepair);
        }
    }

    public static class ProtoRepairCancelledException extends IllegalStateException {
        private final String failMessage;

        public ProtoRepairCancelledException(String failMessage) {
            this.failMessage = failMessage;
        }

        public String getFailMessage() {
            return failMessage;
        }
    }
}
