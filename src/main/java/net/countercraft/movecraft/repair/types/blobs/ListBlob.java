package net.countercraft.movecraft.repair.types.blobs;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;

import net.countercraft.movecraft.craft.type.TypeData.InvalidValueException;
import net.countercraft.movecraft.util.Tags;

public class ListBlob implements RepairBlob {
    private String name;
    private Set<Material> materials;

    public ListBlob(List<?> input) {
        List<String> names = new ArrayList<>();
        materials = EnumSet.noneOf(Material.class);
        for (Object o : input) {
            if (!(o instanceof String))
                throw new InvalidValueException("RepairBlobs array entries must be strings.");

            String s = (String) o;
            EnumSet<Material> set = Tags.parseBlockRegistry(s);
            s = s.toUpperCase();
            if (set == null) {
                materials.add(Material.valueOf(s));
            }
            else {
                materials.addAll(set);
            }
            names.add(s);
        }
        name = "[" + String.join(", ", names) + "]";
    }

    @Override
    public Set<Material> getMaterials() {
        return materials;
    }

    @Override
    public String getName() {
        return name;
    }
}
