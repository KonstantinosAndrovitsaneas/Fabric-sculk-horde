package com.github.sculkhorde.systems.event_system.events;

import com.github.sculkhorde.common.entity.SculkGhastEntity;
import com.github.sculkhorde.core.ModSavedData;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.event_system.Event;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.ChunkLoading.EntityChunkLoaderHelper;
import com.github.sculkhorde.util.PlayerProfileHandler;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

public class GhastDeploymentEvent extends Event {
    protected final int MAX_DISTANCE_FROM_PLAYER = 150;

    protected UUID target;
    protected SculkGhastEntity ghast;

    protected static final Vec3 targetPos = null; //todo finish

    protected enum State {
        INITIALIZATION,
        PURSUIT,
        ENGAGING,
        SUCCESS,
        FAILURE
    }

    protected State state;
    protected boolean isEventOver = false;

    protected Optional<SculkGhastSpawnFinder> spawnFinder = Optional.empty();

    protected Optional<BlockPos> desiredSpawnPos = Optional.empty();

    public GhastDeploymentEvent(ResourceKey<Level> dimension, UUID target) {
        this(dimension);
        this.target = target;
    }

    public GhastDeploymentEvent(ResourceKey<Level> dimension) {
        super(dimension);
        setEventCost(100);
        setState(State.INITIALIZATION);
    }

    public Optional<SculkGhastEntity> getGhast()
    {
        return Optional.ofNullable(ghast);
    }

    public boolean canContinue()
    {
        return !isEventOver;
    }

    @Override
    public void serverTick() {

        if(state == State.INITIALIZATION)
        {
            initializationTick();
        }
        else if(state == State.PURSUIT)
        {
            pursuitTick();
        }
        else if(state == State.ENGAGING)
        {
            engagingTick();
        }
        else if(state == State.SUCCESS)
        {
            successTick();
        }
        else if(state == State.FAILURE)
        {
            failureTick();
        }

    }

    protected void setState(State state)
    {
        this.state = state;
        SculkHorde.LOGGER.info("HitSquadEvent | " + "State: " + state.toString());
    }


    public Optional<BlockPos> findValidSpawnPosition(int cubeLength) {
        // Calculate the bounds of the cube
        int halfLength = cubeLength / 2;
        int minX = getEventLocation().getX() - halfLength;
        int minY = getEventLocation().getY() - halfLength;
        int minZ = getEventLocation().getZ() - halfLength;
        int maxX = getEventLocation().getX() + halfLength;
        int maxY = getEventLocation().getY() + halfLength;
        int maxZ = getEventLocation().getZ() + halfLength;

        // Create a mutable block position to avoid creating new objects in the loop
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // Iterate through each block position in the cube
        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutablePos.set(x, y, z);

                    // Check if the block is air
                    if (BlockAlgorithms.isReplaceable(getDimension().getBlockState(mutablePos))) {
                        // Check if the position is a valid spawn position
                        if (isValidSpawnPos(mutablePos)) {
                            return Optional.of(mutablePos.immutable());
                        }
                    }
                }
            }
        }

        // If no valid spawn position is found, return an empty Optional
        return Optional.empty();
    }

    public boolean isValidSpawnPos(BlockPos.MutableBlockPos pos)
    {
        if(pos == null) { return false; }

        if(!BlockAlgorithms.isAir(ghast.level().getBlockState(pos)))
        {
            return false;
        }

        return true;
    };

    public final Predicate<BlockPos> isObstructed = (blockPos) ->
    {
        boolean isBlockNotAir = !getDimension().getBlockState(blockPos).is(Blocks.AIR);

        return isBlockNotAir;
    };


    protected void initializationTick()
    {
        Optional<ModSavedData.NodeEntry> cloestNode;
        Optional<BlockPos> potentialSpawnPoint = Optional.empty();

        if(spawnFinder.isEmpty())
        {
            cloestNode = ModSavedData.getSaveData().getClosestNodeEntry(getDimension(), getEventLocation());

            if(cloestNode.isEmpty() || !cloestNode.get().isEntryValid()) {
                SculkHorde.LOGGER.error("GhastDeploymentEvent | Error: Could not initialize, no valid nodes nearby.");
                setState(State.FAILURE);
                return;
            }

            spawnFinder = Optional.of(new SculkGhastSpawnFinder(getDimension(), getEventLocation(), cloestNode.get().getPosition()));
            spawnFinder.get().isObstructed = new Predicate<BlockPos>() {
                @Override
                public boolean test(BlockPos pos) {
                    return !BlockAlgorithms.isAir(getDimension().getBlockState(pos));
                }
            };
            //123;

            spawnFinder.get().isValidTargetBlock = new Predicate<BlockPos>() {
                @Override
                public boolean test(BlockPos pos) {
                    return false;
                }
            };

        }






        if(potentialSpawnPoint.isPresent())
        {
            //ghast = SculkSoulReaperEntity.spawnWithDifficulty(player.level(), potentialSpawnPoint.get().getCenter(), getTargetProfile().getDifficultyOfNextHit(), true);
            //ghast.setHitTarget(player);
            //ghast.setParentEventUUID(getEventUUID());
            //ghast.addEffect(new MobEffectInstance(MobEffects.GLOWING, Integer.MAX_VALUE));
            setState(State.PURSUIT);
            return;
        }


        if(SculkHorde.isDebugMode()) {
            SculkHorde.LOGGER.info("HitSquadEvent | FAILURE, Could not find good spawn pos.");
        }
        setState(State.FAILURE);

    }

    protected void pursuitTick()
    {
        if(getPlayerIfOnline().isEmpty())
        {
            return;
        }
        Player player = getPlayerIfOnline().get();

        if(player.distanceTo(ghast) <= 64)
        {
            setState(State.ENGAGING);
            return;
        }

        if(player.isDeadOrDying())
        {
            setState(State.SUCCESS);
            SculkHorde.LOGGER.info("HitSquadEvent | EVENT SUCCESS: Player " + player.getScoreboardName() + " died.");
            return;
        }

        if(ghast.isDeadOrDying())
        {
            setState(State.FAILURE);
            SculkHorde.LOGGER.info("HitSquadEvent | EVENT FAILURE: Player " + player.getScoreboardName() + " killed the Soul Reaper.");
            return;
        }

        if(player.distanceTo(ghast) > MAX_DISTANCE_FROM_PLAYER + 50)
        {
            setState(State.FAILURE);
            PlayerProfileHandler.getOrCreatePlayerProfile(player).setTimeOfLastHit(0);
            SculkHorde.LOGGER.info("HitSquadEvent | EVENT FAILURE: Player " + player.getScoreboardName() + " moved too far away from Soul Reaper.");
            return;
        }

        EntityChunkLoaderHelper.getEntityChunkLoaderHelper().createChunkLoadRequestSquareForEntityIfAbsent(ghast,3, 3, TickUnits.convertMinutesToTicks(1));

    }

    protected void engagingTick()
    {
        if(getPlayerIfOnline().isEmpty())
        {
            return;
        }
        Player player = getPlayerIfOnline().get();

        if(player.distanceTo(ghast) > 70)
        {
            setState(State.PURSUIT);
            return;
        }

        if(player.isDeadOrDying())
        {
            setState(State.SUCCESS);
            return;
        }

        if(ghast.isDeadOrDying())
        {
            setState(State.FAILURE);
            SculkHorde.LOGGER.info("HitSquadEvent | EVENT FAILURE: Player " + player.getScoreboardName() + " killed the Soul Reaper.");
            return;
        }

        if(player.distanceTo(ghast) > MAX_DISTANCE_FROM_PLAYER + 50)
        {
            setState(State.FAILURE);
            PlayerProfileHandler.getOrCreatePlayerProfile(player).setTimeOfLastHit(0);
            SculkHorde.LOGGER.info("HitSquadEvent | EVENT FAILURE: Player " + player.getScoreboardName() + " moved too far away from Soul Reaper.");
            ghast.discard();
            return;
        }

        EntityChunkLoaderHelper.getEntityChunkLoaderHelper().createChunkLoadRequestSquareForEntityIfAbsent(ghast,3, 3, TickUnits.convertMinutesToTicks(1));

    }

    protected void successTick()
    {
        getTargetProfile().decreaseDifficultyOfNextHit();
        isEventOver = true;
    }

    protected void failureTick()
    {
        getTargetProfile().increaseDifficultyOfNextHit();
        isEventOver = true;
    }

    public ModSavedData.PlayerProfileEntry getTargetProfile()
    {
        return PlayerProfileHandler.getOrCreatePlayerProfile(target);
    }

    public Optional<Player> getPlayerIfOnline()
    {
        return getTargetProfile().getPlayer();
    }

    @Override
    public void loadAdditional(CompoundTag tag) {

    }

    @Override
    public void saveAdditional(CompoundTag tag) {

    }

    public class SculkGhastSpawnFinder {
        private final ServerLevel level;
        private final BlockPos origin;
        private final BlockPos target;
        private final PriorityQueue<BlockPos> queue = new PriorityQueue<>(Comparator.comparingInt(this::heuristic));
        private final Map<Long, Boolean> visitedPositions = new HashMap<>();
        private final Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        private boolean debugMode = false;
        private ArmorStand debugStand;
        private boolean pathFound = false;

        private boolean isFinished = false;
        private List<BlockPos> path = new ArrayList<>();

        private int MAX_DISTANCE = 150;

        protected Predicate<BlockPos> isObstructed;
        protected Predicate<BlockPos> isValidTargetBlock;

        protected BlockPos foundBlock;

        public SculkGhastSpawnFinder(ServerLevel level, BlockPos origin, BlockPos target) {
            this.level = level;
            this.origin = origin;
            this.target = target;
            queue.add(origin);
        }

        public void enableDebugMode() {
            debugMode = true;
        }

        private int heuristic(BlockPos pos) {
            // Only consider x and z coordinates
            return Math.abs(pos.getX() - target.getX()) + Math.abs(pos.getZ() - target.getZ());
        }

        public void tick() {
            if (pathFound || queue.isEmpty()) {

                if(pathFound && debugMode)
                {
                    SculkHorde.LOGGER.info("HitSquadSpawnFinder | Found Target Block at" + foundBlock.toShortString());
                }
                else if(debugMode)
                {
                    SculkHorde.LOGGER.info("HitSquadSpawnFinder | Did Not Target Block");
                }

                isFinished = true;
                return;
            }

            // Spawn Debug Stand if Necessary
            if(debugStand == null && debugMode)
            {
                debugStand = new ArmorStand(level, origin.getX(), origin.getY(), origin.getZ());
                debugStand.setInvisible(true);
                debugStand.setNoGravity(true);
                debugStand.addEffect(new MobEffectInstance(MobEffects.GLOWING, TickUnits.convertHoursToTicks(1), 3));
                level.addFreshEntity(debugStand);
            }

            BlockPos current = queue.poll();

            if(debugMode)
            {
                debugStand.teleportTo(current.getX() + 0.5, current.getY(), current.getZ() + 0.5);
            }

            if (isValidTargetBlock.test(current)) {
                path = reconstructPath(current);
                pathFound = true;
                foundBlock = current;
                return;
            }

            for (BlockPos neighbor : BlockAlgorithms.getNeighborsCube(current, false)) {
                if (visitedPositions.getOrDefault(neighbor.asLong(), false)) {
                    continue;
                }

                if (isObstructed.test(neighbor)) {
                    continue;
                }

                if(neighbor.distManhattan(origin) > MAX_DISTANCE)
                {
                    continue;
                }

                queue.add(neighbor);
                visitedPositions.put(neighbor.asLong(), true);
                cameFrom.put(neighbor, current);

                if (debugMode) {
                    //level.setBlockAndUpdate(neighbor, Blocks.GREEN_STAINED_GLASS.defaultBlockState());
                }
            }
        }

        private List<BlockPos> reconstructPath(BlockPos current) {
            List<BlockPos> path = new ArrayList<>();
            while (current != null) {
                path.add(current);
                current = cameFrom.get(current);
            }
            Collections.reverse(path);
            return path;
        }

        public List<BlockPos> getPath() {
            return path;
        }

        public boolean isPathFound() {
            return pathFound;
        }

        public void setTargetBlockPredicate(Predicate<BlockPos> predicate) {
            isValidTargetBlock = predicate;
        }

        public void setObstructionPredicate(Predicate<BlockPos> predicate) {
            isObstructed = predicate;
        }

        public void setMaxDistance(int value) {
            MAX_DISTANCE = value;
        }

        public boolean isFinished()
        {
            return isFinished;
        }

        public BlockPos getFoundBlock()
        {
            return foundBlock;
        }
    }
}
