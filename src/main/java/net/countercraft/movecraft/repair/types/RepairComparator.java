package net.countercraft.movecraft.repair.types;

import java.util.Comparator;

import org.bukkit.Location;
import org.bukkit.Material;

import net.countercraft.movecraft.repair.config.Config;
import net.countercraft.movecraft.repair.tasks.BlockRepair;
import net.countercraft.movecraft.repair.tasks.InventoryRepair;
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
        if (result != Result.UNKNOWN)
            return resultToCompare(result);

        if (!(first instanceof BlockRepair) || !(second instanceof BlockRepair))
            throw new IllegalStateException();

        result = compareMaterials((BlockRepair) first, (BlockRepair) second);
        if (result != Result.NO_ORDER)
            return resultToCompare(result);

        result = compareLocations((BlockRepair) first, (BlockRepair) second);
        return resultToCompare(result);
    }

    private Result compareClasses(RepairTask first, RepairTask second) {
        if (first instanceof BlockRepair) {
            if (second instanceof BlockRepair) {
                return Result.UNKNOWN;
            } else if (second instanceof InventoryRepair) {
                return Result.FIRST;
            } else {
                return Result.FIRST;
            }
        } else if (first instanceof InventoryRepair) {
            if (second instanceof BlockRepair) {
                return Result.SECOND;
            } else if (second instanceof InventoryRepair) {
                return Result.NO_ORDER;
            } else {
                return Result.SECOND;
            }
        } else {
            if (second instanceof BlockRepair) {
                return Result.SECOND;
            } else if (second instanceof InventoryRepair) {
                return Result.FIRST;
            } else {
                return Result.NO_ORDER;
            }
        }
    }

    private Result compareMaterials(BlockRepair first, BlockRepair second) {
        Material firstMaterial = first.getMaterial();
        Material secondMaterial = second.getMaterial();

        if (Config.RepairFirstPass.contains(firstMaterial)) {
            if (Config.RepairFirstPass.contains(secondMaterial)) {
                return Result.NO_ORDER;
            }
            else {
                return Result.FIRST;
            }
        }
        else if (Config.RepairLastPass.contains(firstMaterial)) {
            if (Config.RepairLastPass.contains(secondMaterial)) {
                return Result.NO_ORDER;
            }
            else {
                return Result.SECOND;
            }
        }
        else {
            if (Config.RepairFirstPass.contains(secondMaterial)) {
                return Result.SECOND;
            }
            else if (Config.RepairLastPass.contains(secondMaterial)) {
                return Result.FIRST;
            }
            else {
                return Result.NO_ORDER;
            }
        }
    }

    private Result compareLocations(BlockRepair first, BlockRepair second) {
        Location firstLocation = first.getLocation();
        Location secondLocation = second.getLocation();

        if (firstLocation.getBlockY() < secondLocation.getBlockY())
            return Result.FIRST;
        else if (secondLocation.getBlockY() < firstLocation.getBlockY())
            return Result.SECOND;

        if (firstLocation.getBlockX() < secondLocation.getBlockX())
            return Result.FIRST;
        else if (secondLocation.getBlockX() < firstLocation.getBlockX())
            return Result.SECOND;

        if (firstLocation.getBlockZ() < secondLocation.getBlockZ())
            return Result.FIRST;
        else if (secondLocation.getBlockZ() < firstLocation.getBlockZ())
            return Result.SECOND;

        return Result.NO_ORDER; // Somehow two repairs of the same location and material?!?
    }
}
