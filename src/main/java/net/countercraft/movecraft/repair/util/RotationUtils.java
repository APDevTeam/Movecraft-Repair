package net.countercraft.movecraft.repair.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.util.Direction;
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
        for (var e : block.getStates().entrySet()) {
            String key = e.getKey().getName();

            if (key.equals("rotation")) {
                // Applies to "floor" signs
                switch ((int) e.getValue()) {
                    case 0:
                        return BlockFace.SOUTH;
                    case 1:
                        return BlockFace.SOUTH_SOUTH_WEST;
                    case 2:
                        return BlockFace.SOUTH_WEST;
                    case 3:
                        return BlockFace.WEST_SOUTH_WEST;
                    case 4:
                        return BlockFace.WEST;
                    case 5:
                        return BlockFace.WEST_NORTH_WEST;
                    case 6:
                        return BlockFace.NORTH_WEST;
                    case 7:
                        return BlockFace.NORTH_NORTH_WEST;
                    case 8:
                        return BlockFace.NORTH;
                    case 9:
                        return BlockFace.NORTH_NORTH_EAST;
                    case 10:
                        return BlockFace.NORTH_EAST;
                    case 11:
                        return BlockFace.EAST_NORTH_EAST;
                    case 12:
                        return BlockFace.EAST;
                    case 13:
                        return BlockFace.EAST_SOUTH_EAST;
                    case 14:
                        return BlockFace.SOUTH_EAST;
                    case 15:
                        return BlockFace.SOUTH_SOUTH_EAST;
                    default:
                        return null;
                }
            } else if (key.equals("facing")) {
                // Applies to wall signs
                switch ((Direction) e.getValue()) {
                    case SOUTH:
                        return BlockFace.SOUTH;
                    case WEST:
                        return BlockFace.WEST;
                    case NORTH:
                        return BlockFace.NORTH;
                    case EAST:
                        return BlockFace.EAST;
                    default:
                        return null;
                }
            }
        }
        return null;
    }

    /**
     * Get the rotation of a Spigot block
     * 
     * @param block Block to check
     * @return Spigot BlockFace to represent the rotation
     */
    @Nullable
    public static BlockFace getRotation(Block block) {
        if (block.getBlockData() instanceof Rotatable) {
            // Applies to "floor" signs
            return ((Rotatable) block.getBlockData()).getRotation();
        }
        if (block.getBlockData() instanceof Directional) {
            // Applies to wall signs
            return ((Directional) block.getBlockData()).getFacing();
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
