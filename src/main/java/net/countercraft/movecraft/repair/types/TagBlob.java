package net.countercraft.movecraft.repair.types;

import java.util.Set;

import org.bukkit.Material;

public class TagBlob implements RepairBlob {
    private String input;
    private Set<Material> materials;

    public TagBlob(String input, Set<Material> materials) {
        this.input = input.toUpperCase();
        this.materials = materials;
    }

    @Override
    public Set<Material> getMaterials() {
        return materials;
    }

    @Override
    public String getName() {
        return input;
    }
}
