package net.countercraft.movecraft.repair.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Attachable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BaseBlock;

public class WEUtils {
    private static final ClipboardFormat SCHEMATIC_FORMAT = BuiltInClipboardFormat.SPONGE_SCHEMATIC;

    @Nullable
    public static Clipboard loadSchematic(File file) throws IOException {
        Clipboard clipboard;
        try {
            ClipboardReader reader = SCHEMATIC_FORMAT.getReader(new FileInputStream(file));
            clipboard = reader.read();
        } catch (IOException e) {
            throw new IOException("Failed to load schematic", e);
        }
        return clipboard;
    }

    @Nullable
    private static BlockFace getRotationFromBaseBlock(BaseBlock block) {
        for (var e : block.getStates().entrySet()) {
            if (!e.getKey().getName().equals("rotation"))
                continue;

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
        }
        return null;
    }

    @Nullable
    private static BlockFace getRotationFromWorldBlock(Block block) {
        if (block.getBlockData() instanceof Rotatable) {
            return ((Rotatable) block.getBlockData()).getRotation();
        }
        if (block.getBlockData() instanceof Directional) {
            return ((Directional) block.getBlockData()).getFacing().getOppositeFace();
        }
        return null;
    }

    private static int angleBetweenBlockFaces(@Nullable BlockFace base, @Nullable BlockFace other) {
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

    private static int getRotation(Clipboard clipboard, Sign sign) {
        BlockVector3 signPosition = BlockVector3.at(sign.getX(), sign.getY(), sign.getZ());

        BlockVector3 minPos = clipboard.getMinimumPoint();
        BlockVector3 distance = clipboard.getOrigin().subtract(clipboard.getMinimumPoint());
        BlockVector3 offset = signPosition.subtract(distance);
        BlockVector3 schematicSignPosition = signPosition.subtract(offset).add(minPos);
        BaseBlock schematicSign = clipboard.getFullBlock(schematicSignPosition);
        BlockFace schematicSignFacing = getRotationFromBaseBlock(schematicSign);

        BlockFace worldSignFacing = getRotationFromWorldBlock(sign.getBlock());

        return angleBetweenBlockFaces(worldSignFacing, schematicSignFacing);
    }

    private static Clipboard applyRotation(Clipboard clipboard, int angle) throws WorldEditException {
        return ClipboardUtils.transform(clipboard, new AffineTransform().rotateY(angle));
    }
}
