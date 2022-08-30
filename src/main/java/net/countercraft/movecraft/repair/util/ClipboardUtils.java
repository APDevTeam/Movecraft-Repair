package net.countercraft.movecraft.repair.util;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.CombinedTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import net.countercraft.movecraft.repair.MovecraftRepair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ClipboardUtils {
    /**
     * Apply a transformation to a clipboard
     *
     * @param original original clipboard
     * @param transform transformation to apply
     * @return returns transformed clipboard
     * @throws WorldEditException
     */
    public static @NotNull Clipboard transform(@NotNull Clipboard original, @NotNull Transform transform) throws WorldEditException {
        // Prepare target
        Region targetRegion = getTransformedRegion(original, transform);
        Clipboard target = new BlockArrayClipboard(targetRegion);
        target.setOrigin(original.getOrigin());

        // Prepare operation
        BlockTransformExtent extent = new BlockTransformExtent(original, transform);
        ForwardExtentCopy copy = new ForwardExtentCopy(extent, original.getRegion(), original.getOrigin(), target, original.getOrigin());
        copy.setTransform(transform);

        // Execute operation
        Operations.complete(copy);
        return target;
    }

    /**
     * Get the transformed region.
     *
     * @return the transformed region
     */
    @Contract("_, _ -> new")
    private static @NotNull Region getTransformedRegion(@NotNull Clipboard original, @NotNull Transform transform) {
        Region region = original.getRegion();
        Vector3 minimum = region.getMinimumPoint().toVector3();
        Vector3 maximum = region.getMaximumPoint().toVector3();
        MovecraftRepair.getInstance().getLogger().info("\n\n\tOrigin: '" + region + "'\n");

        Transform transformAround =
                new CombinedTransform(
                        new AffineTransform().translate(original.getOrigin().multiply(-1)),
                        transform,
                        new AffineTransform().translate(original.getOrigin())
                );

        Vector3[] corners = new Vector3[] {
                minimum,
                maximum,
                minimum.withX(maximum.getX()),
                minimum.withY(maximum.getY()),
                minimum.withZ(maximum.getZ()),
                maximum.withX(minimum.getX()),
                maximum.withY(minimum.getY()),
                maximum.withZ(minimum.getZ())
        };

        for (int i = 0; i < corners.length; i++) {
            corners[i] = transformAround.apply(corners[i]);
        }

        Vector3 newMinimum = corners[0];
        Vector3 newMaximum = corners[0];

        for (int i = 1; i < corners.length; i++) {
            newMinimum = newMinimum.getMinimum(corners[i]);
            newMaximum = newMaximum.getMaximum(corners[i]);
        }

        Region result = new CuboidRegion(newMinimum.floor().toBlockPoint(), newMaximum.ceil().toBlockPoint());
        MovecraftRepair.getInstance().getLogger().info("\n\n\tResult: '" + result + "'\n");
        return result;
    }
}