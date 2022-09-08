package net.countercraft.movecraft.repair.types;

import java.util.EnumMap;
import java.util.Set;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: Move this class up to Movecraft core
public class MaterialCounter {
    private EnumMap<Material, Integer> map = new EnumMap<>(Material.class);

    public void add(@NotNull Material material, @Nullable Integer count) {
        if (count == null)
            return;

        Integer current = map.get(material);
        if (current == null) {
            current = count;
        } else {
            current += count;
        }
        map.put(material, current);
    }

    public Set<Material> getMaterials() {
        return map.keySet();
    }

    @Nullable
    public Integer get(Material material) {
        return map.get(material);
    }

    public void add(MaterialCounter other) {
        for (Material key : other.getMaterials()) {
            this.add(key, other.get(key));
        }
    }
}
