package net.countercraft.movecraft.repair.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BaseBlock;

public class RotationUtils {
    /**
     * Get the rotation of a WorldEdit block
     * 
     * @param block Block to check
     * @return Spigot BlockFace to represent the rotation
     */
    @Nullable
    public static BlockFace getRotation(BaseBlock block) {
        return getRotation(BukkitAdapter.adapt(block));
    }

    /**
     * Get the rotation of a Spigot block
     * 
     * @param block Block to check
     * @return Spigot BlockFace to represent the rotation
     */
    @Nullable
    public static BlockFace getRotation(Block block) {
        return getRotation(block.getBlockData());
    }

    /**
     * Get the rotation of a Spigot block data
     * 
     * @param blockData Block data to check
     * @return Spigot BlockFace to represent the rotation
     */
    @Nullable
    public static BlockFace getRotation(BlockData blockData) {
        if (blockData instanceof Rotatable) {
            // Applies to "floor" signs
            return ((Rotatable) blockData).getRotation();
        }
        if (blockData instanceof Directional) {
            // Applies to wall signs
            return ((Directional) blockData).getFacing();
        }
        return null;
    }

    /**
     * Returns the angle between two block faces
     * 
     * @param base  Base BlockFace
     * @param other BlockFace to compare it to
     * @return The angle between them (rounded to quarter turns), in degrees
     */
    public static int angleBetweenBlockFaces(@Nullable BlockFace base, @Nullable BlockFace other) {
        if (base == null || other == null)
            return 0;

        // Vector#angle() does not return the direction, merely the magnitude of the
        // angle difference
        // Therefore, we have to calculate this manually
        // Also, Minecraft flips the Z axis, so *yay*
        double baseAngle = Math.atan2(-1.0 * base.getDirection().getZ(), base.getDirection().getX());
        double otherAngle = Math.atan2(-1.0 * other.getDirection().getZ(), other.getDirection().getX());
        double angle = otherAngle - baseAngle;
        angle /= 2 * Math.PI; // Convert from radians to turns
        angle *= 4; // Convert to quarter-turns
        angle *= -1; // Flip
        int angleDegrees = (int) Math.round(angle); // Round to nearest quarter-turn
        angleDegrees *= 90; // Convert to degrees
        return angleDegrees;
    }
}
