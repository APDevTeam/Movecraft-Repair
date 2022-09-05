package net.countercraft.movecraft.repair.util;

import org.bukkit.Material;

public class RepairUtils {
    public static boolean needsRepair(Material targetType, Material currentType) {
        if (targetType.isAir())
            return false;
        
        return targetType != currentType;
    }
}
