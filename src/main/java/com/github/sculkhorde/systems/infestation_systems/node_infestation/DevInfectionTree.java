package com.github.sculkhorde.systems.infestation_systems.node_infestation;

import com.github.sculkhorde.systems.cursor_system.CursorSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public class DevInfectionTree extends InfectionTree{

    /**
     * Creates a new binary tree with the given value.
     *
     * @param world
     * @param direction
     * @param rootPos
     * @param isPerformanceExempt
     */
    public DevInfectionTree(ServerLevel world, Direction direction, BlockPos rootPos) {
        super(world, direction, rootPos, true);
    }

    /**
     * Creates a new infection cursor
     * @param maxInfections The maximum number of infections the cursor can perform
     */
    @Override
    public void createInfectionCursor(int maxInfections) {

        cursorInfection = CursorSystem.createPerformanceExemptSurfaceInfestorVirtualCursor(world, infectedTargetPosition);
        cursorInfection.setMaxRange(maxInfections);
        cursorInfection.setTickIntervalTicks(0);
        cursorInfection.setSearchIterationsPerTick(50);
        cursorInfection.setMaxTransformations(1000);
    }
}
