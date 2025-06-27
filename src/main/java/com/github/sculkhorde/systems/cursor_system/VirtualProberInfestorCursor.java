package com.github.sculkhorde.systems.cursor_system;

import com.github.sculkhorde.util.BlockAlgorithms;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class VirtualProberInfestorCursor extends VirtualSurfaceInfestorCursor{

    Stack<BlockPos> searchStack = new Stack<>();

    public VirtualProberInfestorCursor(Level level)
    {
        super(level);
    }

    @Override
    protected void resetSearchTick()
    {
        searchStack.clear();
        positionsSearched.clear();
    }

    @Override
    protected void addPositionToQueueIfValid(BlockPos pos)
    {
        boolean isPositionNotVisited = !positionsSearched.containsKey(pos.asLong());
        BlockState neighborBlockState = getLevel().getBlockState(pos);
        boolean isPositionNotObstructed = !isObstructed(neighborBlockState, pos);

        // If not visited and is a valid block to navigate
        if (isPositionNotVisited && isPositionNotObstructed) {
            searchStack.add(pos);
            positionsSearched.put(pos.asLong(), true);
        }
    }

    @Override
    protected void IdleTick()
    {
        searchStack.add(getBlockPosition());
        setState(State.SEARCHING);
    }


    /**
     * Use Breadth-First Search to find the nearest infectable block within a certain maximum distance.
     * @return true if complete. false if not complete.
     */
    @Override
    protected boolean searchTick() {
        // Initialize the visited positions map and the queue
        // Complete 20 times.
        for (int i = 0; i < Math.max(searchIterationsPerTick, 1); i++)
        {
            // Breadth-First Search

            if (searchStack.isEmpty()) {
                isSuccessful = false;
                target = BlockPos.ZERO;
                return true;
            }

            BlockPos currentBlock = searchStack.pop();

            // If the current block is a target, return it
            if (isTarget(currentBlock)) {
                isSuccessful = true;
                target = currentBlock;
                return true;
            }

            // Get all possible directions
            ArrayList<BlockPos> possibleBlocksToVisit = BlockAlgorithms.getNeighborsCube(currentBlock, false);
            Collections.shuffle(possibleBlocksToVisit);

            // Add all neighbors to the queue
            for (BlockPos neighbor : possibleBlocksToVisit) {
                addPositionToQueueIfValid(neighbor);
            }
        }

        return false;
    }


}

