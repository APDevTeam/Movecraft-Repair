package net.countercraft.movecraft.repair;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BaseBlock;

import net.countercraft.movecraft.repair.util.ClipboardUtils;
import net.countercraft.movecraft.repair.util.RotationUtils;
import net.countercraft.movecraft.repair.util.WEUtils;

public class RepairState {
    private Clipboard schematic;
    private BlockVector3 schematicMinPos;
    private BlockVector3 schematicOffset;

    public RepairState(UUID uuid, String name) throws IOException, IllegalStateException {
        File dataDirectory = new File(MovecraftRepair.getInstance().getDataFolder(), "RepairStates");
        File playerDirectory = new File(dataDirectory, uuid.toString());
        if (!playerDirectory.exists())
            throw new IllegalStateException();

        schematic = WEUtils.loadSchematic(playerDirectory, name);
        schematicMinPos = schematic.getMinimumPoint();
        schematicOffset = schematic.getOrigin().subtract(schematicMinPos);
    }

    public void rotate(Sign sign) throws WorldEditException {
        BlockVector3 signPosition = BlockVector3.at(sign.getX(), sign.getY(), sign.getZ());

        BlockVector3 offset = signPosition.subtract(schematicOffset);
        BlockVector3 schematicSignPosition = signPosition.subtract(offset).add(schematicMinPos);
        BaseBlock schematicSign = schematic.getFullBlock(schematicSignPosition);
        BlockFace schematicSignFacing = RotationUtils.getRotation(schematicSign);

        BlockFace worldSignFacing = RotationUtils.getRotation(sign.getBlock());

        int angle = RotationUtils.angleBetweenBlockFaces(worldSignFacing, schematicSignFacing);

        schematic = ClipboardUtils.transform(schematic, new AffineTransform().rotateY(angle));
    }
}
