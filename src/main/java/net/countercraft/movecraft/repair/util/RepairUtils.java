package net.countercraft.movecraft.repair.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;

public class RepairUtils {
    private enum Dependency {
        NONE,
        UP,
        SIDE,
        DOWN,
        ANY,
        UNKNOWN
    }

    /**
     * Remap a material from an input to a required material
     * Note: Material.AIR means the item is free
     * 
     * @param material The material to map
     * @return The material to require
     */
    public static Material remapMaterial(Material material) {
        if (Material.REDSTONE_WIRE == material)
            return Material.REDSTONE;
        if (Tags.FLUID.contains(material))
            return Material.AIR;
        if (Tags.BUCKETS.contains(material))
            return Material.AIR;

        return material;
    }

    /**
     * Gets the item cost per block of the specified material
     * 
     * @param material The material to check
     * @return The cost of the block (half for doors)
     */
    public static double blockCost(Material material) {
        if (Tag.BEDS.isTagged(material))
            return 0.5;
        if (Tag.DOORS.isTagged(material))
            return 0.5;

        return 1;
    }

    /**
     * Get the dependency blocks from a material and location
     * 
     * @param material The material to check
     * @param blockData The block data to check
     * @param location The location of the block to check
     * @return The location of the dependency
     */
    @Nullable
    public static Location getDependency(Material material, BlockData blockData, Location location) {
        Dependency dependency = getDependencyType(material);
        switch (dependency) {
            case NONE:
                return null;
            case UP:
                return location.clone().add(0, 1, 0);
            case DOWN:
                return location.clone().add(0, -1, 0);
            case ANY:
            case SIDE:
                BlockFace direction = getFaceFromBlockData(blockData);
                if (direction == null)
                    return null;

                return location.clone().add(direction.getModX(), direction.getModY(), direction.getModZ());
            case UNKNOWN:
            default:
                return null;
        }
    }

    @Nullable
    private static BlockFace getFaceFromBlockData(BlockData data) {
        if (!(data instanceof Directional))
            return null;

        return ((Directional) data).getFacing();
    }

    private static Dependency getDependencyType(Material material) {
        switch (material) {
            case LEVER:
                return Dependency.ANY;
            case LADDER:
                return Dependency.SIDE;
            case WALL_TORCH:
                return Dependency.SIDE;
            case TORCH:
                return Dependency.DOWN;
            case REDSTONE_WALL_TORCH:
                return Dependency.SIDE;
            case REDSTONE_TORCH:
                return Dependency.DOWN;
            case REPEATER:
                return Dependency.DOWN;
            case COMPARATOR:
                return Dependency.DOWN;
            case HEAVY_WEIGHTED_PRESSURE_PLATE:
                return Dependency.DOWN;
            case LIGHT_WEIGHTED_PRESSURE_PLATE:
                return Dependency.DOWN;
            case SNOW:
                return Dependency.DOWN;
            case TRIPWIRE_HOOK:
                return Dependency.SIDE;
            case TRIPWIRE:
                return Dependency.DOWN;
            default:
                break;
        }

        if (material.hasGravity())
            return Dependency.DOWN;

        if (Tag.WALL_SIGNS.isTagged(material))
            return Dependency.SIDE;
        else if (Tag.SIGNS.isTagged(material))
            return Dependency.DOWN;

        if (Tag.BUTTONS.isTagged(material))
            return Dependency.ANY;

        if (Tag.CARPETS.isTagged(material))
            return Dependency.DOWN;
        if (Tags.FLUID.contains(material))
            return Dependency.DOWN;
        if (Tag.DOORS.isTagged(material))
            return Dependency.DOWN;
        if (Tag.BEDS.isTagged(material))
            return Dependency.DOWN;
        if (Tag.WOODEN_PRESSURE_PLATES.isTagged(material))
            return Dependency.DOWN;
        if (Tag.BANNERS.isTagged(material))
            return Dependency.ANY;
        if (Tag.RAILS.isTagged(material))
            return Dependency.DOWN;

        return Dependency.UNKNOWN;
    }

    /**
     * Check if a block needs to be block-repaired
     * 
     * @param targetType Type for the block to become
     * @param currentType Current type of the block
     * @return True if the block needs repairing
     */
    public static boolean needsBlockRepair(Material targetType, Material currentType) {
        if (targetType.isAir())
            return false;
        if (Config.RepairBlackList.contains(targetType) || Config.RepairBlackList.contains(currentType))
            return false;

        return targetType != currentType;
    }

    /**
     * Check if the block needs inventory repair
     * 
     * @param currentType Current block type
     * @param currentState Current block state
     * @param targetContents Target contents of the block
     * @return A boolean as to if the block needs repair, and a counter of the materials needed.
     */
    public static Pair<Boolean, Counter<Material>> checkInventoryRepair(Material currentType, BlockState currentState,
            @Nullable Counter<Material> targetContents) {
        if (targetContents == null || targetContents.getKeySet().isEmpty())
            return new Pair<>(false, new Counter<>());

        if (!(currentState instanceof Container container))
            return new Pair<>(true, targetContents);

        ItemStack[] items = container.getInventory().getContents();
        Counter<Material> currentContents = new Counter<>();
        for (ItemStack stack : items) {
            if (stack == null || !checkAllowedInventoryFill(currentType, stack.getType()))
                continue;

            currentContents.add(stack.getType(), stack.getAmount());
        }

        Counter<Material> result = new Counter<>();
        for (Material material : targetContents.getKeySet()) {
            if (!checkAllowedInventoryFill(currentType, material))
                continue; // Skip un-allowed materials

            int target = targetContents.get(material);
            int current = currentContents.get(material);
            if (current == 0) {
                result.add(material, target);
            }
            else {
                int count = target - current;
                if (count > 0)
                    result.add(material, target - current);
            }
        }

        if (result.getKeySet().isEmpty())
            return new Pair<>(false, new Counter<>());

        return new Pair<>(true, result);
    }

    private static boolean checkAllowedInventoryFill(Material block, Material item) {
        if (block == Material.DISPENSER) {
            return Config.RepairDispenserItems.contains(item);
        }
        else if (Tags.FURNACES.contains(block)) {
            return Config.RepairFurnaceItems.contains(item);
        }
        else if (block == Material.DROPPER) {
            return Config.RepairDropperItems.contains(item);
        }
        else {
            return false;
        }
    }
}
