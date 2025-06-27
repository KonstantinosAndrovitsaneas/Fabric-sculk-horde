package com.github.sculkhorde.systems.infestation_systems.node_infestation;

import com.github.sculkhorde.core.ModConfig;
import com.github.sculkhorde.core.ModSavedData;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NodeBranchingInfestationSystem {

    // The parent tile entity
    protected BlockEntity parent = null;
    protected ServerLevel world = null;
    protected BlockPos origin = null;

    public boolean spawnOnSurface = true;

    protected final int CHECK_FOR_ACTIVATION_INTERVAL = TickUnits.convertMinutesToTicks(1);
    protected int timeRemainingUntilNextActivationCheck = CHECK_FOR_ACTIVATION_INTERVAL;
    
    protected boolean isPerformanceExempt = false;

    protected boolean isActive = false;

    // The infection trees
    protected InfectionTree northInfectionTree;
    protected InfectionTree southInfectionTree;
    protected InfectionTree eastInfectionTree;
    protected InfectionTree westInfectionTree;
    protected InfectionTree upInfectionTree;
    protected InfectionTree downInfectionTree;

    protected int lastTimeSinceTick = 0;
    protected int TICK_COOLDOWN = TickUnits.convertSecondsToTicks(1);


    public NodeBranchingInfestationSystem(BlockEntity parent, BlockPos origin, boolean isPerformanceExempt) {
        this.parent = parent;
        this.world = (ServerLevel) parent.getLevel();
        this.origin = origin;
        this.isPerformanceExempt = isPerformanceExempt;

        northInfectionTree = new InfectionTree(world, Direction.NORTH, origin, isPerformanceExempt);
        northInfectionTree.activate();

        southInfectionTree = new InfectionTree(world, Direction.SOUTH, origin, isPerformanceExempt);
        southInfectionTree.activate();

        eastInfectionTree = new InfectionTree(world, Direction.EAST, origin, isPerformanceExempt);
        eastInfectionTree.activate();

        westInfectionTree = new InfectionTree(world, Direction.WEST, origin, isPerformanceExempt);
        westInfectionTree.activate();

        upInfectionTree = new InfectionTree(world, Direction.UP, origin, isPerformanceExempt);
        upInfectionTree.activate();

        downInfectionTree = new InfectionTree(world, Direction.DOWN, origin, isPerformanceExempt);
        downInfectionTree.activate();

    }

    public BlockPos calculateSpawnPosition()
    {
        if(!spawnOnSurface)
        {
            return origin;
        }

        // Do ray trace from bottom to top of world. Return last known solid block
        BlockPos.MutableBlockPos checkPosition = new BlockPos.MutableBlockPos(origin.getX(), world.getMinBuildHeight(), origin.getZ());
        BlockPos lastKnownSolidBlock = null;
        while(checkPosition.getY() < world.getMaxBuildHeight())
        {
            checkPosition.setY(checkPosition.getY() + 1);
            if(BlockAlgorithms.isSolid(world, checkPosition))
            {
                lastKnownSolidBlock = checkPosition.immutable();
            }
        }

        if(lastKnownSolidBlock != null)
        {
            origin = lastKnownSolidBlock;
            //SculkHorde.LOGGER.debug("Sculk Node found InfestationHandler spawn position at " + lastKnownSolidBlock + " of blockstate " + world.getBlockState(lastKnownSolidBlock));
        }

        return lastKnownSolidBlock;
    }

    public boolean canBeActivated()
    {
        if(timeRemainingUntilNextActivationCheck > 0)
        {
            timeRemainingUntilNextActivationCheck--;
            return false;
        }

        if(!ModSavedData.getSaveData().isHordeActive()) {
            timeRemainingUntilNextActivationCheck = CHECK_FOR_ACTIVATION_INTERVAL;
            return false;
        }

        if(parent == null || world == null || origin == null)
        {
            timeRemainingUntilNextActivationCheck = CHECK_FOR_ACTIVATION_INTERVAL;
            return false;
        }

        if(calculateSpawnPosition() != null)
        {
            timeRemainingUntilNextActivationCheck = CHECK_FOR_ACTIVATION_INTERVAL;
            return true;
        }

        SculkHorde.LOGGER.info("Sculk Node at " + parent.getBlockPos() + " cannot be activated because it has no spawn position.");
        timeRemainingUntilNextActivationCheck = CHECK_FOR_ACTIVATION_INTERVAL;
        return false;
    }

    public void activate()
    {
        isActive = true;
        northInfectionTree.setOrigin(origin);
        southInfectionTree.setOrigin(origin);
        eastInfectionTree.setOrigin(origin);
        westInfectionTree.setOrigin(origin);
    }

    public void deactivate()
    {
        isActive = false;
    }

    public void tick() {
        if(!isActive || !ModConfig.SERVER.block_infestation_enabled.get())
        {
            return;
        }

        if(lastTimeSinceTick < TICK_COOLDOWN)
        {
            lastTimeSinceTick++;
            return;
        }
        lastTimeSinceTick = 0;

        northInfectionTree.tick();
        southInfectionTree.tick();
        eastInfectionTree.tick();
        westInfectionTree.tick();
        upInfectionTree.tick();
        downInfectionTree.tick();
    }
}
