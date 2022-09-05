package net.countercraft.movecraft.repair.types;

import java.util.Comparator;

import org.bukkit.Material;

import net.countercraft.movecraft.repair.config.Config;

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
        if (result != Result.UNKNOWN) {
            return resultToCompare(result);
        }

        if (!(first instanceof BlockRepair) || !(second instanceof BlockRepair))
            throw new IllegalStateException();
        result = compareMaterials((BlockRepair) first, (BlockRepair) second);
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
}
