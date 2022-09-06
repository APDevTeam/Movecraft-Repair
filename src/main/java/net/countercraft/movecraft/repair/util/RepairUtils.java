package net.countercraft.movecraft.repair.util;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.types.MaterialCounter;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;

public class RepairUtils {
    public static boolean needsBlockRepair(Material targetType, Material currentType) {
        if (targetType.isAir())
            return false;

        return targetType != currentType;
    }

    public static Pair<Boolean, MaterialCounter> checkInventoryRepair(Material currentType, BlockState currentState,
            MaterialCounter targetContents) {
        if (!(currentState instanceof Container))
            return new Pair<>(false, null);

        Container container = (Container) currentState;
        ItemStack[] items = container.getInventory().getContents();
        MaterialCounter currentContents = new MaterialCounter();
        for (ItemStack stack : items) {
            if (!checkAllowedInventoryFill(currentType, stack.getType()))
                continue;

            currentContents.add(stack.getType(), stack.getAmount());
        }
        // TODO
        return new Pair<>(true, currentContents);
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
