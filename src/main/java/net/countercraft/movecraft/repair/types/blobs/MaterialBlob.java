package net.countercraft.movecraft.repair.types.blobs;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;

public class MaterialBlob implements RepairBlob {
    private Material material;

    public MaterialBlob(String input) {
        material = Material.valueOf(input.toUpperCase());
    }

    @Override
    public Set<Material> getMaterials() {
        return EnumSet.of(material);
    }

    @Override
    public String getName() {
        return material.toString();
    }
}
