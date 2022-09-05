package net.countercraft.movecraft.repair.config;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;

public class Config {
    public static boolean Debug = false;

    // Localisation
    public static String Locale = "en";

    public static int RepairTicksPerBlock = 0;
    public static double RepairMaxPercent = 50.0;
    public static double RepairMoneyPerBlock = 0.0;
    public static Set<EnumSet<Material>> RepairBlobs = new HashSet<>();
    public static EnumSet<Material> RepairFirstPass = EnumSet.noneOf(Material.class);
    public static EnumSet<Material> RepairLastPass = EnumSet.noneOf(Material.class);
}
