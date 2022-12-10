package net.countercraft.movecraft.repair;

import org.bukkit.Material;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.countercraft.movecraft.craft.type.TypeData.InvalidValueException;
import net.countercraft.movecraft.repair.types.blobs.RepairBlob;

public class RepairBlobManager {
    private static Object2ObjectMap<Material, RepairBlob> backing = new Object2ObjectOpenHashMap<>();

    public static void add(RepairBlob blob) {
        for (Material m : blob.getMaterials()) {
            RepairBlob otherBlob = backing.get(m);
            if (otherBlob != null) {
                throw new InvalidValueException("RepairBlob " + blob.getName() + " and " + otherBlob.getName() + " both contain " + m);
            }

            backing.put(m, blob);
        }
    }

    public static RepairBlob get(Material material) {
        return backing.get(material);
    }
}
