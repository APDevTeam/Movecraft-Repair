package net.countercraft.movecraft.repair.util;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;

public class RepairUtils {
    private static Object2ObjectMap<Material, Material> materialRemapping = new Object2ObjectOpenHashMap<>();
    private static Object2DoubleMap<Material> materialCosts = new Object2DoubleOpenHashMap<>();

    static {
        materialRemapping.put(Material.REDSTONE_WIRE, Material.REDSTONE);
        materialRemapping.put(Material.LAVA_BUCKET, Material.AIR);
        materialRemapping.put(Material.WATER_BUCKET, Material.AIR);

        // Doors and beds are two blocks each, so only charge half for each block
        for (Material m : Tag.DOORS.getValues()) {
            materialCosts.put(m, 0.5);
        }
        for (Material m : Tag.BEDS.getValues()) {
            materialCosts.put(m, 0.5);
        }
    }

    /**
     * Remap a material from an input to a required material
     * Note: Material.AIR means the item is free
     * 
     * @param material The material to map
     * @return The material to require
     */
    public static Material remapMaterial(Material material) {
        if (!materialRemapping.containsKey(material))
            return material;

        return materialRemapping.apply(material);
    }

    /**
     * Gets the item cost per block of the specified material
     * 
     * @param material The material to check
     * @return The cost of the block (half for doors)
     */
    public static double blockCost(Material material) {
        if (!materialCosts.containsKey(material))
            return 1;

        return materialCosts.apply(material);
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

        if (!(currentState instanceof Container))
            return new Pair<>(true, targetContents);

        Container container = (Container) currentState;
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
