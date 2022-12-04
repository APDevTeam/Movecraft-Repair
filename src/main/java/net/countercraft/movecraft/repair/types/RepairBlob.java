package net.countercraft.movecraft.repair.types;

import java.util.Set;

import org.bukkit.Material;

public interface RepairBlob {
    public Set<Material> getMaterials();
    public String getName();
}
