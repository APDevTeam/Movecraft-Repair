package net.countercraft.movecraft.repair.utils;

import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class UpdateCommandsQueuePair extends Pair<LinkedList<UpdateCommand>, LinkedList<UpdateCommand>> {

    public UpdateCommandsQueuePair(@NotNull LinkedList<UpdateCommand> updateCommands, @NotNull LinkedList<UpdateCommand> updateCommandsFragileBlocks) {
        super(updateCommands, updateCommandsFragileBlocks);
    }

    public LinkedList<UpdateCommand> getUpdateCommands() {
        return getLeft();
    }

    public LinkedList<UpdateCommand> getUpdateCommandsFragileBlocks() {
        return getRight();
    }

}
