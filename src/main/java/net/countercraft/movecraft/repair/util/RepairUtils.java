package net.countercraft.movecraft.repair.util;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;

public class RepairUtils {
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
            if (stack == null)
                continue;
            if (!checkAllowedInventoryFill(currentType, stack.getType()))
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
