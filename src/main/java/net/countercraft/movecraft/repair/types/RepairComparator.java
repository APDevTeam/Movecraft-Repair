package net.countercraft.movecraft.repair.types;

import java.util.Comparator;

import org.bukkit.Location;
import org.bukkit.Material;

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.tasks.BlockRepair;
import net.countercraft.movecraft.repair.tasks.RepairTask;

public class RepairComparator implements Comparator<RepairTask> {
    private enum Result {
        FIRST,
        NO_ORDER,
        UNKNOWN,
        SECOND
    }

    private int resultToCompare(Result result) {
        switch (result) {
            case FIRST:
                return -1;
            case SECOND:
                return 1;
            case NO_ORDER:
            case UNKNOWN:
            default:
                return 0;
        }
    }

    @Override
    public int compare(RepairTask first, RepairTask second) {
        Result result = compareClasses(first, second);
        if (result != Result.NO_ORDER)
            return resultToCompare(result);

        if ((first instanceof BlockRepair) && (second instanceof BlockRepair)) {
            result = compareMaterials((BlockRepair) first, (BlockRepair) second);
            if (result != Result.NO_ORDER)
                return resultToCompare(result);
        }

        result = compareLocations(first, second);
        return resultToCompare(result);
    }

    private Result compareClasses(RepairTask first, RepairTask second) {
        if (first.getPriority() == second.getPriority()) {
            return Result.NO_ORDER;
        } else if (first.getPriority() > second.getPriority()) {
            return Result.FIRST;
        } else {
            return Result.SECOND;
        }
    }

    private Result compareMaterials(BlockRepair first, BlockRepair second) {
        int firstPriority = materialPriority(first.getMaterial());
        int secondPriority = materialPriority(second.getMaterial());

        if (firstPriority == secondPriority) {
            return Result.NO_ORDER;
        } else if (firstPriority > secondPriority) {
            return Result.FIRST;
        } else {
            return Result.SECOND;
        }
    }

    private int materialPriority(Material material) {
        if (material == Material.OBSERVER)
            return Integer.MIN_VALUE;
        if (Config.RepairFirstPass.contains(material))
            return Integer.MAX_VALUE;
        if (Config.RepairLastPass.contains(material))
            return -1000;
        return 0;
    }

    private Result compareLocations(RepairTask first, RepairTask second) {
        Location firstLocation = first.getLocation();
        Location secondLocation = second.getLocation();

        // Lower is higher priority, with the craft building across z, then x, then y
        if (firstLocation.getBlockY() < secondLocation.getBlockY())
            return Result.FIRST;
        else if (firstLocation.getBlockY() > secondLocation.getBlockY())
            return Result.SECOND;

        if (firstLocation.getBlockX() < secondLocation.getBlockX())
            return Result.FIRST;
        else if (firstLocation.getBlockX() > secondLocation.getBlockX())
            return Result.SECOND;

        if (firstLocation.getBlockZ() < secondLocation.getBlockZ())
            return Result.FIRST;
        else if (firstLocation.getBlockZ() > secondLocation.getBlockZ())
            return Result.SECOND;

        return Result.NO_ORDER; // Somehow two repairs of the same location, class and material?!?
    }
}
