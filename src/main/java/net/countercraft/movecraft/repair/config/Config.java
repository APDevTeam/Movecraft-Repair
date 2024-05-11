package net.countercraft.movecraft.repair.config;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;

public class Config {
    public static boolean Debug = false;

    // Localisation
    public static String Locale = "en";

    public static int RepairTicksPerBlock = 0;
    public static long RepairMaxTickTime = 5000000;
    public static int RepairMaxBlocksPerTick = 2;
    public static double RepairMoneyPerBlock = 0.0;
    public static double RepairMaxPercent = 50.0;
    public static Material RepairTool = Material.FIREWORK_ROCKET;
    public static boolean DisableDoubleClick = false;
    public static Set<Material> RepairDispenserItems = EnumSet.noneOf(Material.class);
    public static Set<Material> RepairFurnaceItems = EnumSet.noneOf(Material.class);
    public static Set<Material> RepairDropperItems = EnumSet.noneOf(Material.class);
    public static Set<Material> RepairFirstPass = EnumSet.noneOf(Material.class);
    public static Set<Material> RepairLastPass = EnumSet.noneOf(Material.class);
}
