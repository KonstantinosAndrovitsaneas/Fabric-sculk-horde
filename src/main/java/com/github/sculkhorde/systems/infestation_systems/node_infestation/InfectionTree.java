package com.github.sculkhorde.systems.infestation_systems.node_infestation;

import com.github.sculkhorde.core.ModSavedData;
import com.github.sculkhorde.systems.cursor_system.CursorSystem;
import com.github.sculkhorde.systems.cursor_system.VirtualProberInfestorCursor;
import com.github.sculkhorde.systems.cursor_system.VirtualSurfaceInfestorCursor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class InfectionTree {
    protected TreeNode root;
    protected boolean Active = false;
    protected final Direction direction;
    protected VirtualProberInfestorCursor cursorProbe;
    protected VirtualSurfaceInfestorCursor cursorInfection;
    protected final ServerLevel world;
    protected state currentState = state.IDLE;
    protected enum state {
        IDLE,
        PROBING,
        INFECTION,
        COMPLETE
    }

    protected BlockPos potentialNodePosition = null;
    protected int failedProbeAttempts = 0;
    protected final int MAX_FAILED_PROBE_ATTEMPTS = 2;
    protected int currentProbeRange = 10;
    protected final int MAX_PROBE_RANGE = 5000;
    protected final int MIN_PROBE_RANGE = 10;
    protected final int PROBE_RANGE_INCREMENT = 50;
    protected final int MAX_INFECTOR_RANGE = 100;
    protected final int MIN_INFECTOR_RANGE = 10;
    protected final int MAX_INFECTOR_RANGE_INCREMENT = 10;

    protected BlockPos infectedTargetPosition = null;
    protected int failedInfectionAttempts = 0;
    protected final int MAX_FAILED_INFECTION_ATTEMPTS = 10;

    protected boolean isPerformanceExempt = false;

    /**
     * Creates a new binary tree with the given value.
     */
    public InfectionTree(ServerLevel world, Direction direction, BlockPos rootPos, boolean isPerformanceExempt)
    {
        this.root = new TreeNode(rootPos);
        this.direction = direction;
        this.world = world;
        this.isPerformanceExempt = isPerformanceExempt;
    }

    // Getters and Setters

    public boolean isActive() {
        return Active;
    }

    public void activate() {
        Active = true;
    }

    public void deactivate() {
        Active = false;
    }

    /**
     * Gets the root node.
     * @return The root node.
     */
    public TreeNode getRoot() {
        return root;
    }

    /**
     * Sets the root node.
     */
    public void setRoot(TreeNode root) {
        this.root = root;
    }

    public void setOrigin(BlockPos origin)
    {
        this.root.blockPos = origin;
    }

    // Events

    /**
     * Creates a new probe cursor
     * @param maxDistance The maximum distance the cursor can travel
     */
    public void createProbeCursor(int maxDistance) {
        BlockPos spawnPos = infectedTargetPosition == null ? root.getBlockPos() : infectedTargetPosition;

        Optional<VirtualProberInfestorCursor> possibleCursor = Optional.empty();
        if(isPerformanceExempt)
        {
            possibleCursor = Optional.of(CursorSystem.createPerformanceExemptProberVirtualCursor(world, spawnPos));

        }
        else
        {
            possibleCursor = CursorSystem.createProberVirtualCursor(world, spawnPos);
        }

        if(possibleCursor.isEmpty())
        {
            return;
        }

        cursorProbe = possibleCursor.get();
        cursorProbe.moveTo(this.root.blockPos.getX(), this.root.blockPos.getY(), this.root.blockPos.getZ());
        cursorProbe.setMaxRange(maxDistance);
        //cursorProbe.setPreferedDirection(direction);
        cursorProbe.setMaxTransformations(1);
    }

    /**
     * Creates a new infection cursor
     * @param maxInfections The maximum number of infections the cursor can perform
     */
    public void createInfectionCursor(int maxInfections) {
        BlockPos spawnPos = infectedTargetPosition == null ? root.getBlockPos() : infectedTargetPosition;
        Optional<VirtualSurfaceInfestorCursor> possibleCursor = Optional.empty();

        if(isPerformanceExempt)
        {
            possibleCursor = Optional.of(CursorSystem.createPerformanceExemptSurfaceInfestorVirtualCursor(world, spawnPos));

        }
        else
        {
            possibleCursor = CursorSystem.createSurfaceInfestorVirtualCursor(world, spawnPos);
        }

        if(possibleCursor.isEmpty())
        {
            return;
        }

        cursorInfection = possibleCursor.get();
        cursorInfection.setMaxRange(maxInfections);
        cursorInfection.setTickIntervalTicks(2);
        cursorInfection.setSearchIterationsPerTick(50);
        cursorInfection.setMaxTransformations(50);
    }

    /**
     * Ticks the infection tree
     */
    public void tick()
    {
        // If the root is null, or the tree is not active, do nothing
        if(root.blockPos == BlockPos.ZERO || !isActive())
        {
            return;
        }

        if(ModSavedData.getSaveData().getSculkAccumulatedMass() <= 0)
        {
            return;
        }


        // If the probe has failed too many times, change state to complete
        if(failedProbeAttempts >= MAX_FAILED_PROBE_ATTEMPTS)
        {
            // Change State to Complete
            currentState = state.COMPLETE;
        }

        // If the probe range is too large, reset it
        if(currentProbeRange > MAX_PROBE_RANGE)
        {
            // Reset the probe range
            currentProbeRange = MIN_PROBE_RANGE;
        }



        if(currentState == state.IDLE)
        {
            currentState = state.PROBING;
        }
        else if(currentState == state.PROBING)
        {
            // If the probe is null, create a new one
            if(cursorProbe == null)
            {
                createProbeCursor(currentProbeRange);
                return;
            }
            // If the probe is still active, wait for it to finish
            else if(!cursorProbe.isFinished())
            {
                return;
            }

            // If the probe is successful, record the findings
            if(cursorProbe.isSuccessful())
            {
                potentialNodePosition = cursorProbe.getBlockPosition();
                failedProbeAttempts = 0;
                cursorProbe = null;
                // Change State to Infection Mode
                currentState = state.INFECTION;
            }
            // If the probe is not successful, record the findings
            else
            {
                cursorProbe = null;
                failedProbeAttempts++;
                potentialNodePosition = BlockPos.ZERO;
            }
        }
        else if(currentState == state.INFECTION)
        {
            infectedTargetPosition = potentialNodePosition;

            // If the infection cursor is null, create a new one
            if(cursorInfection == null)
            {
                createInfectionCursor(MAX_INFECTOR_RANGE);
                return;
            }
            // If the infection cursor is still active, wait for it to finish
            else if(!cursorInfection.isFinished())
            {
                return;
            }

            // If the infection is successful, record the findings
            if(cursorInfection.isSuccessful())
            {
                failedInfectionAttempts = 0;
                cursorInfection = null;
            }
            // If the infection is not successful, record the findings
            else
            {
                failedInfectionAttempts++;
                cursorInfection = null;
            }

            // If the infection range is too large, reset it and change state to complete
            if(failedInfectionAttempts >= MAX_FAILED_INFECTION_ATTEMPTS)
            {
                failedInfectionAttempts = 0;
                currentState = state.PROBING;
            }
        }
        else if(currentState == state.COMPLETE)
        {
            if(failedProbeAttempts >= MAX_FAILED_PROBE_ATTEMPTS)
            {
                currentProbeRange += PROBE_RANGE_INCREMENT;
                failedProbeAttempts = 0;
                currentState = state.IDLE;
            }

            if(failedInfectionAttempts >= MAX_FAILED_INFECTION_ATTEMPTS)
            {
                currentState = state.IDLE;
                failedInfectionAttempts = 0;
            }
        }

    }

    /**
     * A node in a binary tree.
     */
    public class TreeNode {
        protected BlockPos blockPos;
        protected TreeNode left;
        protected TreeNode right;

        public TreeNode(BlockPos blockPos) {
            this.blockPos = blockPos;
        }

        public BlockPos getBlockPos() {
            return blockPos;
        }

        public void setBlockPos(BlockPos blockPos) {
            this.blockPos = blockPos;
        }

        public TreeNode getLeft() {
            return left;
        }

        public void setLeft(TreeNode left) {
            this.left = left;
        }

        public TreeNode getRight() {
            return right;
        }

        public void setRight(TreeNode right) {
            this.right = right;
        }
    }
}
