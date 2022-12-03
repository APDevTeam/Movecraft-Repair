package net.countercraft.movecraft.repair.types;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

public class RepairCounter {
    private Object2DoubleMap<RepairBlob> backing = new Object2DoubleOpenHashMap<>();

    public RepairCounter() {
        backing.defaultReturnValue(0.0);
    }

    public double get(RepairBlob blob) {
        return backing.getDouble(blob);
    }

    public double add(RepairBlob blob, double count) {
        return backing.put(blob, backing.getDouble(blob) + count);
    }

    public double subtract(RepairBlob blob, double count) {
        return backing.put(blob, backing.getDouble(blob) - count);
    }

    public void clear(RepairBlob blob) {
        backing.removeDouble(blob);
    }

    public Set<RepairBlob> getKeySet() {
        return backing.keySet();
    }

    public boolean isEmpty() {
        return backing.isEmpty();
    }
}
