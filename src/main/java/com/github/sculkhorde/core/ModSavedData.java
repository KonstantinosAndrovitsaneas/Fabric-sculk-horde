package com.github.sculkhorde.core;

import com.github.sculkhorde.common.block.SculkBeeNestBlock;
import com.github.sculkhorde.common.blockentity.SculkNodeBlockEntity;
import com.github.sculkhorde.misc.StatisticsData;
import com.github.sculkhorde.systems.AutoPerformanceSystem;
import com.github.sculkhorde.systems.BeeNestActivitySystem;
import com.github.sculkhorde.systems.DebugSlimeSystem;
import com.github.sculkhorde.systems.SculkNodesSystem;
import com.github.sculkhorde.systems.chunk_cursor_system.ChunkInfestationSystem;
import com.github.sculkhorde.systems.cursor_system.CursorSystem;
import com.github.sculkhorde.systems.event_system.EventSystem;
import com.github.sculkhorde.systems.gravemind_system.Gravemind;
import com.github.sculkhorde.systems.path_builder_system.PathBuilderSystem;
import com.github.sculkhorde.systems.raid_system.RaidData;
import com.github.sculkhorde.systems.raid_system.RaidHandler;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.ChunkLoading.BlockEntityChunkLoaderHelper;
import com.github.sculkhorde.util.ChunkLoading.EntityChunkLoaderHelper;
import com.github.sculkhorde.util.DeathAreaInvestigator;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.sculkhorde.util.BlockAlgorithms.getBlockDistance;

/**
 * This class handels all data that gets saved to and loaded from the world. <br>
 * Learned World Data mechanics from: <a href="https://www.youtube.com/watch?v=tyTsdCzVz6w">...</a>
 */
public class ModSavedData extends SavedData {

    // identifier for debugmode
    private static final String debugModeIdentifier = "debugMode";

    public static enum HordeState {
        UNACTIVATED,
        ACTIVE,
        DEFEATED
    }
    HordeState hordeState = HordeState.UNACTIVATED;

    private final ArrayList<NodeEntry> nodeEntries = new ArrayList<>();
    private final ArrayList<BeeNestEntry> beeNestEntries = new ArrayList<>();
    private final Map<String, HostileEntry> hostileEntries = new HashMap<>();
    private final ArrayList<DeathAreaEntry> deathAreaEntries = new ArrayList<>();
    private final ArrayList<AreaOfInterestEntry> areasOfInterestEntries = new ArrayList<>();
    private final ArrayList<NoRaidZoneEntry> noRaidZoneEntries = new ArrayList<>();
    private final ArrayList<PlayerProfileEntry> playerProfileEntries = new ArrayList<>();

    private int sculkAccumulatedMass = 0;
    private static final String sculkAccumulatedMassIdentifier = "sculkAccumulatedMass";
    private int noNodeSpawningTicksElapsed = Gravemind.TICKS_BETWEEN_NODE_SPAWNS;
    private static final String ticksSinceSculkNodeDestructionIdentifier = "ticksSinceSculkNodeDestruction";
    private int ticksSinceLastRaid = TickUnits.convertHoursToTicks(8);
    private static final String ticksSinceLastRaidIdentifier = "ticksSinceLastRaid";

    /**
     * Default Constructor
     */
    public ModSavedData()
    {
        super();
        initializeSystems();
    }

    public static ModSavedData getSaveData()
    {
        ModSavedData data = ServerLifecycleHooks.getCurrentServer().overworld().getDataStorage().computeIfAbsent(ModSavedData::load, ModSavedData::new, SculkHorde.SAVE_DATA_ID);
        data.setDirty();
        return data;
    }

    public static void initializeData(){
        getSaveData();
    }

    public boolean isHordeUnactivated() {
        return hordeState == HordeState.UNACTIVATED;
    }

    public boolean isHordeActive() {
        return hordeState == HordeState.ACTIVE;
    }

    public boolean isHordeDefeated() {
        return hordeState == HordeState.DEFEATED;
    }

    public HordeState getHordeState() {
        return hordeState;
    }

    public void setHordeState(HordeState hordeStateIn) {
        this.hordeState = hordeStateIn;
        setDirty();
    }


    /**
     * Note: We initialize systems in {@link ModSavedData#load(CompoundTag)}
     * instead of {@link com.github.sculkhorde.util.ForgeEventSubscriber#onWorldLoad(LevelEvent.Load)} because
     * it fires after {@link ModSavedData#load(CompoundTag)} which causes the following side effects.
     * <br><br>
     * 1. Data will be loaded into systems, then wiped out by {@link com.github.sculkhorde.util.ForgeEventSubscriber#onWorldLoad(LevelEvent.Load)}<br>
     * 2. Data won't be properly reset between world loads if you try to remedy this with individual null checks.<br>
     * 3. If data not properly reset between world loads, the {@link CursorSystem} will cause the world to get stuck at 100% loading.<br><br>
     * In conclusion, it is better to initialize systems first, then load their data.
     * <br>
     * Reference: Nomatic Tents by skyjay1 <a test href="https://github.com/skyjay1/Nomadic-Tents/blob/master-1.19/src/main/java/nomadictents/NTSavedData.java#L30">...</a>
     * <br>
     */
    protected static void initializeSystems()
    {
        SculkHorde.LOGGER.info("ModSavedData | Initializing All Systems.");

        SculkHorde.LOGGER.info("ModSavedData | Initializing Gravemind.");
        SculkHorde.gravemind = new Gravemind();
        SculkHorde.LOGGER.info("ModSavedData | Initialized Gravemind Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing debugSlimeSystem.");
        SculkHorde.debugSlimeSystem = new DebugSlimeSystem();
        SculkHorde.LOGGER.info("ModSavedData | Initialized debugSlimeSystem Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing deathAreaInvestigator.");
        SculkHorde.deathAreaInvestigator = new DeathAreaInvestigator();
        SculkHorde.LOGGER.info("ModSavedData | Initialized deathAreaInvestigator.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing raidHandler.");
        SculkHorde.raidHandler = new RaidHandler();
        SculkHorde.LOGGER.info("ModSavedData | Initialized raidHandler Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing sculkNodesSystem.");
        SculkHorde.sculkNodesSystem = new SculkNodesSystem();
        SculkHorde.LOGGER.info("ModSavedData | Initialized sculkNodesSystem Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing entityChunkLoaderHelper.");
        SculkHorde.entityChunkLoaderHelper = new EntityChunkLoaderHelper();
        SculkHorde.LOGGER.info("ModSavedData | Initialized entityChunkLoaderHelper Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing blockEntityChunkLoaderHelper.");
        SculkHorde.blockEntityChunkLoaderHelper = new BlockEntityChunkLoaderHelper();
        SculkHorde.LOGGER.info("ModSavedData | Initialized blockEntityChunkLoaderHelper Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing eventSystem.");
        SculkHorde.eventSystem = new EventSystem();
        SculkHorde.LOGGER.info("ModSavedData | Initialized eventSystem Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing beeNestActivitySystem.");
        SculkHorde.beeNestActivitySystem = new BeeNestActivitySystem();
        SculkHorde.LOGGER.info("ModSavedData | Initialized beeNestActivitySystem Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing autoPerformanceSystem.");
        SculkHorde.autoPerformanceSystem = new AutoPerformanceSystem();
        SculkHorde.LOGGER.info("ModSavedData | Initialized autoPerformanceSystem Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing chunkInfestationSystem.");
        SculkHorde.chunkInfestationSystem = new ChunkInfestationSystem();
        SculkHorde.LOGGER.info("ModSavedData | Initialized chunkInfestationSystem Successfully.");



        SculkHorde.LOGGER.info("ModSavedData | Initializing CursorSystem.");
        SculkHorde.cursorSystem = new CursorSystem();
        SculkHorde.LOGGER.info("ModSavedData | Initialized CursorSystem Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading raidData.");
        RaidHandler.raidData = new RaidData();
        SculkHorde.LOGGER.info("ModSavedData | Loaded raidData Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading statisticsData.");
        SculkHorde.statisticsData = new StatisticsData();
        SculkHorde.LOGGER.info("ModSavedData | Loaded statisticsData Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading pathBuilderSystem.");
        SculkHorde.pathBuilderSystem = new PathBuilderSystem();
        SculkHorde.LOGGER.info("ModSavedData | Loaded pathBuilderSystem Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading list of items cursors can eat.");
        ModConfig.SERVER.loadItemsInfectionCursorsCanEat();
        SculkHorde.LOGGER.info("ModSavedData | Loaded list of items cursors can eat Successfully.");
        SculkHorde.LOGGER.info("ModSavedData | Loading list of configured infestable blocks.");
        ModConfig.SERVER.loadConfiguredInfestableBlocks();
        SculkHorde.LOGGER.info("ModSavedData | Loaded list of configured infestable blocks Successfully.");

        if(ModConfig.SERVER.purification_speed_multiplier.get() <= 0)
        {
            ModConfig.SERVER.purification_speed_multiplier.set(1.0);
            SculkHorde.LOGGER.info("ModSavedData | Detected configured purification speed below 0. Resetting to 1.0");
        }

        if(ModConfig.SERVER.infection_speed_multiplier.get() <= 0)
        {
            ModConfig.SERVER.infection_speed_multiplier.set(1.0);
            SculkHorde.LOGGER.info("ModSavedData | Detected configured infestation speed below 0. Resetting to 1.0");
        }
        SculkHorde.LOGGER.info("ModSavedData | Initialed All Systems Successfully.");
    }

    /**
     * This method gets called every time the world loads data from memory.
     * We extract data from the memory and store it in variables.
     *
     * @param nbt The memory where data is stored
     */
    public static ModSavedData load(CompoundTag nbt) {



        //CompoundTag gravemindData = nbt.getCompound("gravemindData");

        ModSavedData savedData = new ModSavedData();

        savedData.getNodeEntries().clear();
        savedData.getBeeNestEntries().clear();
        savedData.getHostileEntries().clear();
        savedData.getDeathAreaEntries().clear();
        savedData.getAreasOfInterestEntries().clear();

        savedData.setHordeState(HordeState.values()[nbt.getInt("hordeState")]);
        SculkHorde.LOGGER.info("ModSavedData | Loaded Horde State.");
        savedData.setSculkAccumulatedMass(nbt.getInt(sculkAccumulatedMassIdentifier));
        SculkHorde.LOGGER.info("ModSavedData | Loaded Gravemind State.");
        savedData.setNoNodeSpawningTicksElapsed(nbt.getInt(ticksSinceSculkNodeDestructionIdentifier));
        SculkHorde.LOGGER.info("ModSavedData | Loaded No Node Spawning Ticks Elapsed.");

        savedData.setTicksSinceLastRaid(nbt.getInt(ticksSinceLastRaidIdentifier));
        SculkHorde.LOGGER.info("ModSavedData | Loaded Ticks SInce Last Raid.");

        SculkHorde.setDebugMode(nbt.getBoolean(debugModeIdentifier));
        SculkHorde.LOGGER.info("ModSavedData | Loaded Debug Mode State.");

        SculkHorde.LOGGER.info("ModSavedData | Loading Node Entries.");
        for (int i = 0; nbt.contains("node_entry" + i); i++) {
            savedData.getNodeEntries().add(NodeEntry.serialize(nbt.getCompound("node_entry" + i)));
        }
        SculkHorde.LOGGER.info("ModSavedData | Loaded Node Entries Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading BeeNest Entries.");
        for (int i = 0; nbt.contains("bee_nest_entry" + i); i++) {
            savedData.getBeeNestEntries().add(BeeNestEntry.serialize(nbt.getCompound("bee_nest_entry" + i)));
        }
        SculkHorde.LOGGER.info("ModSavedData | Loaded BeeNest Entries Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading Hostile Entries.");
        for (int i = 0; nbt.contains("hostile_entry" + i); i++) {
            HostileEntry hostileEntry = HostileEntry.serialize(nbt.getCompound("hostile_entry" + i));
            savedData.getHostileEntries().putIfAbsent(hostileEntry.identifier, hostileEntry);
        }
        SculkHorde.LOGGER.info("ModSavedData | Loaded Hostile Entries Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading Death Area Entries.");
        for (int i = 0; nbt.contains("death_area_entry" + i); i++) {
            savedData.getDeathAreaEntries().add(DeathAreaEntry.serialize(nbt.getCompound("death_area_entry" + i)));
        }
        SculkHorde.LOGGER.info("ModSavedData | Loaded Death Area Entries Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading AreaOfInterest Entries.");
        for (int i = 0; nbt.contains("area_of_interest_entry" + i); i++) {
            savedData.getAreasOfInterestEntries().add(AreaOfInterestEntry.serialize(nbt.getCompound("area_of_interest_entry" + i)));
        }
        SculkHorde.LOGGER.info("ModSavedData | Loaded AreaOfInterest Entries Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading NoRaidZone Entries.");
        for(int i = 0; nbt.contains("no_raid_zone_entry" + i); i++) {
            savedData.getNoRaidZoneEntries().add(NoRaidZoneEntry.serialize(nbt.getCompound("no_raid_zone_entry" + i)));
        }
        SculkHorde.LOGGER.info("ModSavedData | Loaded NoRaidZone Entries Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading PlayerProfile Entries.");
        for(int i = 0; nbt.contains("player_profile_entry" + i); i++) {
            savedData.getPlayerProfileEntries().add(PlayerProfileEntry.serialize(nbt.getCompound("player_profile_entry" + i)));
        }
        SculkHorde.LOGGER.info("ModSavedData | Loaded PlayerProfile Entries Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading raidData.");
        if(RaidHandler.raidData == null)
        {
            RaidHandler.raidData = new RaidData();
        }
        RaidData.load(nbt);
        SculkHorde.LOGGER.info("ModSavedData | Loaded raidData Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading statisticsData.");
        if(SculkHorde.statisticsData == null)
        {
            SculkHorde.statisticsData = new StatisticsData();
        }
        StatisticsData.load(nbt);
        SculkHorde.LOGGER.info("ModSavedData | Loaded statisticsData Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading BlockEntityChunkLoaderHelper Data.");
        BlockEntityChunkLoaderHelper.load(nbt);
        SculkHorde.LOGGER.info("ModSavedData | Loaded BlockEntityChunkLoaderHelper Data Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading EntityChunkLoaderHelper Data.");
        EntityChunkLoaderHelper.load(nbt);
        SculkHorde.LOGGER.info("ModSavedData | Loaded BlockEntityChunkLoaderHelper Data Successfully.");

        SculkHorde.LOGGER.info("ModSavedData | Loading EventSystem Data.");
        EventSystem.load(nbt);
        SculkHorde.LOGGER.info("ModSavedData | Loaded EventSystem Data Successfully.");

        return savedData;

    }


    /**
     * This method gets called every time the world saves data from memory.
     * We take the data in our variables and store it to memory.
     *
     * @param nbt The memory where data is stored
     */
    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        //CompoundTag gravemindData = new CompoundTag();

        nbt.putInt("hordeState", hordeState.ordinal());
        nbt.putInt(sculkAccumulatedMassIdentifier, sculkAccumulatedMass);
        nbt.putInt(ticksSinceSculkNodeDestructionIdentifier, noNodeSpawningTicksElapsed);
        nbt.putInt(ticksSinceLastRaidIdentifier, ticksSinceLastRaid);
        nbt.putBoolean(debugModeIdentifier, SculkHorde.isDebugMode());

        for (ListIterator<NodeEntry> iterator = getNodeEntries().listIterator(); iterator.hasNext(); ) {
            nbt.put("node_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        for (ListIterator<BeeNestEntry> iterator = getBeeNestEntries().listIterator(); iterator.hasNext(); ) {
            nbt.put("bee_nest_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        int hostileIndex = 0;
        for (Map.Entry<String, HostileEntry> entry : getHostileEntries().entrySet()) {
            nbt.put("hostile_entry" + hostileIndex, entry.getValue().deserialize());
            hostileIndex++;
        }

        for (ListIterator<DeathAreaEntry> iterator = getDeathAreaEntries().listIterator(); iterator.hasNext(); ) {
            nbt.put("death_area_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        for (ListIterator<AreaOfInterestEntry> iterator = getAreasOfInterestEntries().listIterator(); iterator.hasNext(); ) {
            nbt.put("area_of_interest_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        for (ListIterator<NoRaidZoneEntry> iterator = getNoRaidZoneEntries().listIterator(); iterator.hasNext(); ) {
            nbt.put("no_raid_zone_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        for (ListIterator<PlayerProfileEntry> iterator = getPlayerProfileEntries().listIterator(); iterator.hasNext(); ) {
            nbt.put("player_profile_entry" + iterator.nextIndex(), iterator.next().deserialize());
        }

        //nbt.put("gravemindData", gravemindData);

        RaidData.save(nbt);
        StatisticsData.save(nbt);
        BlockEntityChunkLoaderHelper.save(nbt);
        EntityChunkLoaderHelper.save(nbt);
        EventSystem.save(nbt);

        return nbt;
    }

    public ArrayList<NoRaidZoneEntry> getNoRaidZoneEntries() {
        return noRaidZoneEntries;
    }

    /**
     * Accessors
     **/

    public boolean isRaidCooldownOver() {
        return getTicksSinceLastRaid() >= TickUnits.convertMinutesToTicks(ModConfig.SERVER.sculk_raid_global_cooldown_between_raids_minutes.get());
    }

    public int getTicksSinceLastRaid() {
        setDirty();
        return ticksSinceLastRaid;
    }

    public void setTicksSinceLastRaid(int ticksSinceLastRaid) {
        this.ticksSinceLastRaid = ticksSinceLastRaid;
        setDirty();
    }

    public void incrementTicksSinceLastRaid() {
        this.ticksSinceLastRaid++;
        setDirty();
    }

    public boolean isNodeSpawnCooldownOver() {
        long ticksElapsed = getTicksElapsedForNodeSpawningCooldown();
        long ticksNeeded = Gravemind.TICKS_BETWEEN_NODE_SPAWNS;
        boolean result = ticksElapsed >= ticksNeeded;
        return result;
    }

    public long getMinutesRemainingUntilNodeSpawn()
    {
        long ticksElapsed = getTicksElapsedForNodeSpawningCooldown();
        long ticksNeeded = Gravemind.TICKS_BETWEEN_NODE_SPAWNS;
        long result = Math.max(0, ticksNeeded - ticksElapsed);
        return TickUnits.convertTicksToMinutes(result);
    }

    public int getTicksElapsedForNodeSpawningCooldown() {
        setDirty();
        return noNodeSpawningTicksElapsed;
    }

    public void setNoNodeSpawningTicksElapsed(int noNodeSpawningTicksElapsed) {
        this.noNodeSpawningTicksElapsed = noNodeSpawningTicksElapsed;
        setDirty();
    }

    public void incrementNoNodeSpawningTicksElapsed() {
        this.noNodeSpawningTicksElapsed++;
        setDirty();
    }

    public void resetNoNodeSpawningTicksElapsed() {
        //Send message to all players that node has spawned
        this.noNodeSpawningTicksElapsed = 0;
        setDirty();
    }

    /**
     * Gets how much Sculk mass the Sculk horde has.
     *
     * @return An integer representing all Sculk mass accumulated.
     */
    public int getSculkAccumulatedMass() {
        setDirty();

        if(getSaveData().isHordeDefeated())
        {
            return 0;
        }

        return sculkAccumulatedMass;
    }

    /**
     * Adds to the sculk accumulated mass
     *
     * @param amount The amount you want to add
     */
    public int addSculkAccumulatedMass(int amount) {
        setDirty();
        sculkAccumulatedMass += amount;
        return sculkAccumulatedMass;
    }

    /**
     * Subtracts from the Sculk Accumulate Mass
     *
     * @param amount The amount to substract
     */
    public int subtractSculkAccumulatedMass(int amount) {
        setDirty();
        sculkAccumulatedMass -= amount;
        return sculkAccumulatedMass;
    }

    /**
     * Sets the value of sculk accumulate mass.
     *
     * @param amount The amount to set it to.
     */
    public int setSculkAccumulatedMass(int amount) {
        setDirty();
        sculkAccumulatedMass = amount;
        return sculkAccumulatedMass;
    }

    public ArrayList<NodeEntry> getNodeEntries() {
        return nodeEntries;
    }

    public ArrayList<BeeNestEntry> getBeeNestEntries() {
        return beeNestEntries;
    }

    public Map<String, HostileEntry> getHostileEntries() {
        return hostileEntries;
    }

    public ArrayList<DeathAreaEntry> getDeathAreaEntries() {
        return deathAreaEntries;
    }

    public ArrayList<AreaOfInterestEntry> getAreasOfInterestEntries() {
        return areasOfInterestEntries;
    }

    /**
     * Adds a position to the list if it does not already exist
     *
     * @param positionIn The Posoition to add
     */
    public void addNodeToMemory(ServerLevel level, BlockPos positionIn)
    {
        if (!isNodePositionInMemory(positionIn) && getNodeEntries() != null)
        {
            getNodeEntries().add(new NodeEntry(level, positionIn));
            setDirty();
        }
    }

    /**
     * Adds a position to the list if it does not already exist
     *
     * @param positionIn The Position to add
     */
    public void addBeeNestToMemory(ServerLevel level, BlockPos positionIn)
    {
        if (!isBeeNestPositionInMemory(positionIn) && getBeeNestEntries() != null)
        {
            getBeeNestEntries().add(new BeeNestEntry(level, positionIn));
            setDirty();
        }
        // TODO For some reason this continously gets called, find out why
        //else if(DEBUG_MODE) System.out.println("Attempted to Add Nest To Memory but failed.");
    }

    public void addHostileToMemory(LivingEntity entityIn)
    {
        if (entityIn == null || EntityAlgorithms.isSculkLivingEntity.test(entityIn) || entityIn instanceof Creeper)
        {
            return;
        }

        String identifier = entityIn.getType().toString();
        if (!identifier.isEmpty())
        {
            getHostileEntries().putIfAbsent(identifier, new HostileEntry(identifier));
            setDirty();
        }
    }

    public void addDeathAreaToMemory(ServerLevel dimension, BlockPos positionIn)
    {
        if(getDeathAreaEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to add a death area to memory but the list was null");
            return;
        }

        // If already exists in memory, dont add it again
        for(int i = 0; i < getDeathAreaEntries().size(); i++)
        {
            if(getDeathAreaEntries().get(i).position == positionIn)
            {
                return;
            }
        }

        SculkHorde.LOGGER.info("Adding Death Area in " + dimension.dimension() + " at " + positionIn + " to memory");
        getDeathAreaEntries().add(new DeathAreaEntry(dimension, positionIn));
        setDirty();
    }

    public Optional<AreaOfInterestEntry> addAreaOfInterestToMemory(ServerLevel dimension, BlockPos positionIn) {
        if(getAreasOfInterestEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to add an area of interest to memory but the list was null");
            return Optional.empty();
        }

        // If already exists in memory, dont add it again
        for(int i = 0; i < getAreasOfInterestEntries().size(); i++)
        {
            if(getAreasOfInterestEntries().get(i).position == positionIn || getAreasOfInterestEntries().get(i).position.closerThan(positionIn, 100))
            {
                return Optional.empty();
            }
        }

        SculkHorde.LOGGER.info("Adding Area of Interest at " + dimension.dimension() + " at " + positionIn + " to memory");
        AreaOfInterestEntry entry = new AreaOfInterestEntry(dimension, positionIn);
        getAreasOfInterestEntries().add(entry);
        setDirty();
        return Optional.of(entry);
    }

    public void addNoRaidZoneToMemory(ServerLevel dimension, BlockPos positionIn) {
        if(getNoRaidZoneEntries() == null)
        {
            SculkHorde.LOGGER.error("addNoRaidZoneToMemory | Cannot add, getNoRaidZoneEntries() is null.");
            return;
        }

        if(dimension == null)
        {
            SculkHorde.LOGGER.error("addNoRaidZoneToMemory | Cannot add, ServerLevel is null.");
            return;
        }

        if(positionIn == null)
        {
            SculkHorde.LOGGER.error("addNoRaidZoneToMemory | Cannot add, BlockPos is null.");
            return;
        }

        // If already exists in memory, dont add it again
        for(int i = 0; i < getNoRaidZoneEntries().size(); i++)
        {
            boolean areInSameDimension = BlockAlgorithms.areTheseDimensionsEqual(getNoRaidZoneEntries().get(i).dimension, dimension.dimension());
            boolean arePositionsEqual = getNoRaidZoneEntries().get(i).position.equals(positionIn);
            boolean isCloserThan100BlocksFromPosition = getNoRaidZoneEntries().get(i).position.closerThan(positionIn, 100);

            if((areInSameDimension && arePositionsEqual) || (areInSameDimension && isCloserThan100BlocksFromPosition))
            {
                if(isCloserThan100BlocksFromPosition) { SculkHorde.LOGGER.debug("Attempted to add a no raid zone to memory but it was too close to another no raid zone"); }
                else if(arePositionsEqual) { SculkHorde.LOGGER.debug("Attempted to add a no raid zone to memory but it already existed"); }

                return;
            }
        }

        SculkHorde.LOGGER.info("Adding No Raid Zone at " + positionIn + " in " + dimension.dimension() + " to memory");
        getNoRaidZoneEntries().add(new NoRaidZoneEntry(dimension, positionIn, 1000, ServerLifecycleHooks.getCurrentServer().overworld().getGameTime(), TickUnits.convertMinutesToTicks(ModConfig.SERVER.sculk_raid_no_raid_zone_duration_minutes.get())));
        setDirty();
    }

    private Optional<DeathAreaEntry> getDeathAreaWithinRange(BlockPos positionIn, int range)
    {
        if(getDeathAreaEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to get a death area from memory but the list was null");
            return Optional.empty();
        }

        for(int i = 0; i < getDeathAreaEntries().size(); i++)
        {
            if(getDeathAreaEntries().get(i).position.closerThan(positionIn, range))
            {
                return Optional.of(getDeathAreaEntries().get(i));
            }
        }
        return Optional.empty();
    }

    public Optional<DeathAreaEntry> getDeathAreaWithHighestDeaths()
    {
        if(getDeathAreaEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to get a death area from memory but the list was null");
            return Optional.empty();
        }

        int highestDeathCount = 0;
        DeathAreaEntry highestDeathArea = null;

        for(int i = 0; i < getDeathAreaEntries().size(); i++)
        {
            if(getDeathAreaEntries().get(i).deathCount > highestDeathCount)
            {
                highestDeathCount = getDeathAreaEntries().get(i).deathCount;
                highestDeathArea = getDeathAreaEntries().get(i);
            }
        }

        if(highestDeathArea == null)
        {
            return Optional.empty();
        }

        return Optional.of(highestDeathArea);
    }

    /**
     * Will try to return an Area of Interest Entry that is not in a no raid zone.
     * @return Optional<AreaofInterestEntry> - The area of interest entry that is not in a no raid zone
     */
    public Optional<AreaOfInterestEntry> getAreaOfInterestEntryNotInNoRaidZone()
    {
        if(getAreasOfInterestEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to get an area of interest from memory but the list was null");
            return null;
        }

        for(int i = 0; i < getAreasOfInterestEntries().size(); i++)
        {
            // If the area of interest is not in a no raid zone, return it
            if(!getAreasOfInterestEntries().get(i).isInNoRaidZone())
            {
                return Optional.of(getAreasOfInterestEntries().get(i));
            }
        }
        return Optional.empty();
    }

    /**
     * Will verify all enties to see if they exist in the world.
     * If not, they will be removed. <br>
     * Gets called in {@link com.github.sculkhorde.util.ForgeEventSubscriber#WorldTickEvent}
     */
    public void validateNodeEntries() {
        long startTime = System.nanoTime();
        Iterator<NodeEntry> iterator = getNodeEntries().iterator();
        while (iterator.hasNext()) {
            NodeEntry entry = iterator.next();
            if (!entry.isEntryValid()) {
                resetNoNodeSpawningTicksElapsed();
                iterator.remove();
                setDirty();
            }
        }
        long endTime = System.nanoTime();
        if (SculkHorde.isDebugMode()) {
            System.out.println("Node Validation Took " +
                    TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS) +
                    " milliseconds");
        }
    }


    /**
     * Will verify all enties to see if they exist in the world.
     * Will also reasses the parentNode for each one.
     * If not, they will be removed. <br>
     * Gets called in {@link com.github.sculkhorde.util.ForgeEventSubscriber#WorldTickEvent}
     */
    public void validateBeeNestEntries() {
        long startTime = System.nanoTime();
        List<BeeNestEntry> toRemove = new ArrayList<>();
        Iterator<BeeNestEntry> iterator = getBeeNestEntries().iterator();
        while (iterator.hasNext()) {
            BeeNestEntry entry = iterator.next();
            entry.setParentNodeToClosest();
            if (!entry.isEntryValid()) {
                toRemove.add(entry);
            }
        }
        getBeeNestEntries().removeAll(toRemove);
        setDirty();
        long endTime = System.nanoTime();
        if (SculkHorde.isDebugMode()) {
            System.out.println("Bee Nest Validation Took " + TimeUnit.MILLISECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS) + " milliseconds");
        }
    }

    public void validateAreasOfInterest() {
        long startTime = System.currentTimeMillis();
        Iterator<AreaOfInterestEntry> iterator = getAreasOfInterestEntries().iterator();
        while (iterator.hasNext()) {
            AreaOfInterestEntry entry = iterator.next();
            if (!entry.isInNoRaidZone()) {
                SculkHorde.LOGGER.info("Area of Interest at " + entry.position + " is on no raid zone. Removing from memory.");
                iterator.remove();
                setDirty();
            }
        }
        long endTime = System.currentTimeMillis();
        if (SculkHorde.isDebugMode()) {
            SculkHorde.LOGGER.info("Area Of Interest Validation Took " + (endTime - startTime) + " milliseconds");
        }
    }

    public void validateNoRaidZoneEntries() {
        long startTime = System.currentTimeMillis();
        Iterator<NoRaidZoneEntry> iterator = getNoRaidZoneEntries().iterator();
        while (iterator.hasNext()) {
            NoRaidZoneEntry entry = iterator.next();
            if (entry.isExpired(ServerLifecycleHooks.getCurrentServer().overworld().getGameTime())) {
                SculkHorde.LOGGER.info("No Raid Zone Entry at " + entry.position + " has expired. Removing from memory.");
                iterator.remove();
                setDirty();
            }
        }
        long endTime = System.currentTimeMillis();
        if (SculkHorde.isDebugMode()) {
            SculkHorde.LOGGER.info("No Raid Zone Validation Took " + (endTime - startTime) + " milliseconds");
        }
    }


    /**
     * Will check the positons of all entries to see
     * if they match the parameter.
     *
     * @param position The position to cross reference
     * @return true if in memory, false otherwise
     */
    public boolean isBeeNestPositionInMemory(BlockPos position) {
        for (BeeNestEntry entry : getBeeNestEntries()) {
            if (entry.position == position) {
                return true;
            }
        }
        return false;
    }


    /**
     * Will check the positons of all entries to see
     * if they match the parameter.
     *
     * @param position The position to cross-reference
     * @return true if in memory, false otherwise
     */
    public boolean isNodePositionInMemory(BlockPos position) {
        for (NodeEntry entry : getNodeEntries()) {
            if (entry.position.equals(position)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of known node positions
     *
     * @return The Closest TreeNode
     */
    public Optional<NodeEntry> getClosestNodeEntry(ServerLevel dimension, BlockPos pos) {
        Optional<NodeEntry> closestNode = Optional.empty();
        double closestDistance = Double.MAX_VALUE;

        for (NodeEntry node : getNodeEntries()) {
            if(!node.isEntryValid())
            {
                continue;
            }

            if (pos.distSqr(node.position) < closestDistance && node.getDimension().equals(dimension)) {
                closestNode = Optional.of(node);
                closestDistance = pos.distSqr(node.position);
            }
        }
        return closestNode;
    }

    public void removeNodeFromMemory(BlockPos positionIn)
    {
        if(getNodeEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to remove an area of interest from memory but the list was null");
            return;
        }

        for(int i = 0; i < getNodeEntries().size(); i++)
        {
            if(getNodeEntries().get(i).position.equals(positionIn))
            {
                getNodeEntries().remove(i);
                setDirty();
                resetNoNodeSpawningTicksElapsed();
                return;
            }
        }
        setDirty();
    }

    public void removeDeathAreaFromMemory(BlockPos positionIn)
    {
        if(getDeathAreaEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to remove a death area from memory but the list was null");
            return;
        }

        for(int i = 0; i < getDeathAreaEntries().size(); i++)
        {
            if(getDeathAreaEntries().get(i).position == positionIn)
            {
                getDeathAreaEntries().remove(i);
                setDirty();
                return;
            }
        }
        setDirty();
    }

    public void removeAreaOfInterestFromMemory(BlockPos positionIn)
    {
        if(getAreasOfInterestEntries() == null)
        {
            SculkHorde.LOGGER.warn("Attempted to remove an area of interest from memory but the list was null");
            return;
        }

        for(int i = 0; i < getAreasOfInterestEntries().size(); i++)
        {
            if(getAreasOfInterestEntries().get(i).position == positionIn)
            {
                getAreasOfInterestEntries().remove(i);
                setDirty();
                return;
            }
        }
        setDirty();
    }

    /**
     * This method gets called every time a sculk mob dies.
     * We check if the death happened in a death area.
     * If it did, we iterate the death count of that area.
     * If it did not, we create a new death area.
     *
     * @param deathPosition The position where the player died
     */
    public void reportDeath(ServerLevel level, BlockPos deathPosition)
    {
        // If a death area already exist close to this location, iterate the death count
        Optional<DeathAreaEntry> deathArea = getDeathAreaWithinRange(deathPosition, 100);
        if(deathArea.isPresent())
        {
            deathArea.get().iterateDeathCount();
            setDirty();
            return;
        }

        // If the death area does not exist, create a new one
        addDeathAreaToMemory(level, deathPosition);
    }

    /**
     * This class is a representation of the actual
     * Sculk Nodes in the world that the horde has access
     * to. It allows the gravemind to keep track of all.
     */
    public static class NodeEntry
    {
        private final BlockPos position; //The Location in the world where the node is
        private long lastTimeWasActive;
        private long activationTimeStamp;
        private boolean IsActive;
        private ResourceKey<Level> dimension;


        /**
         * Default Constructor
         * @param positionIn The physical location
         */
        public NodeEntry(ServerLevel level, BlockPos positionIn)
        {
            position = positionIn;
            this.dimension = level.dimension();
        }

        /**
         * Default Constructor
         * @param positionIn The physical location
         */
        public NodeEntry(ResourceKey<Level> dimensionResource, BlockPos positionIn)
        {
            position = positionIn;
            this.dimension = dimensionResource;
        }
        public ServerLevel getDimension()
        {
            return ServerLifecycleHooks.getCurrentServer().getLevel(dimension);
        }

        public BlockPos getPosition()
        {
            return position;
        }

        public boolean isActive()
        {
            return IsActive;
        }

        public void setActive(boolean activeIn)
        {
            if(getDimension() == null)
            {
                SculkHorde.LOGGER.error("Failed To Set Node Active. Dimension was null.");
                return;
            }
            else if(getDimension().getBlockEntity(position) == null)
            {
                SculkHorde.LOGGER.error("Failed To Set Node Active. Block Entity was null.");
                return;
            }
            else if(!(getDimension().getBlockEntity(position) instanceof SculkNodeBlockEntity))
            {
                SculkHorde.LOGGER.error("Failed To Set Node Active. Block Entity was not instance of Sculk Node Block Entity.");
                return;
            }

            IsActive = activeIn;
            SculkNodeBlockEntity sculkNodeBlockEntity = (SculkNodeBlockEntity) getDimension().getBlockEntity(position);
            sculkNodeBlockEntity.setActive(activeIn);
        }

        public long getLastTimeWasActive()
        {
            return lastTimeWasActive;
        }

        public void setLastTimeWasActive(long lastTimeWasActiveIn)
        {
            lastTimeWasActive = lastTimeWasActiveIn;
        }

        public void setActivationTimeStamp(long activationTimeStampIn)
        {
            activationTimeStamp = activationTimeStampIn;
        }

        public long getActivationTimeStamp()
        {
            return activationTimeStamp;
        }

        /**
         * Checks the world to see if the node is still there.
         * @return True if in the world at location, false otherwise
         */
        public boolean isEntryValid()
        {
            if(getDimension() == null || getPosition() == null) { return false; }
            return getDimension().getBlockState(position).getBlock().equals(ModBlocks.SCULK_NODE_BLOCK.get());
        }

        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            nbt.putLong("lastTimeWasActive", lastTimeWasActive);
            nbt.putLong("activationTimeStamp", activationTimeStamp);
            nbt.putBoolean("IsActive", IsActive);
            // Put Dimension ID
            nbt.putString("dimension", dimension.location().toString());
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static NodeEntry serialize(CompoundTag nbt)
        {
            ResourceKey<Level> dimensionResourceKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString("dimension")));
            NodeEntry entry = new NodeEntry(dimensionResourceKey, BlockPos.of(nbt.getLong("position")));
            entry.setLastTimeWasActive(nbt.getLong("lastTimeWasActive"));
            entry.setActivationTimeStamp(nbt.getLong("activationTimeStamp"));
            entry.setActive(nbt.getBoolean("IsActive"));
            // Get Dimension

            return entry;
        }

    }

    /**
     * This class is a representation of the actual
     * Bee Nests in the world that the horde has access
     * to. It allows the gravemind to keep track of all.
     */
    public static class BeeNestEntry
    {
        private final BlockPos position; //The location in the world where the node is
        private BlockPos parentNodePosition; //The location of the Sculk TreeNode that this Nest belongs to

        private ResourceKey<Level> dimension;

        /**
         * Default Constructor
         * @param positionIn The Position of this Nest
         */
        public BeeNestEntry(ServerLevel dimension, BlockPos positionIn)
        {
            this.dimension = dimension.dimension();
            position = positionIn;
        }

        public BeeNestEntry(ServerLevel dimension, BlockPos positionIn, BlockPos parentPositionIn)
        {
            position = positionIn;
            parentNodePosition = parentPositionIn;
            this.dimension = dimension.dimension();
        }

        public BeeNestEntry(ResourceKey<Level> dimension, BlockPos positionIn, BlockPos parentPositionIn)
        {
            position = positionIn;
            parentNodePosition = parentPositionIn;
            this.dimension = dimension;
        }

        public ServerLevel getDimension()
        {
            return ServerLifecycleHooks.getCurrentServer().overworld().getServer().getLevel(dimension);
        }

        public BlockPos getPosition() { return position; }

        /**
         * Checks if the block does still exist in the world.
         * @return True if valid, false otherwise.
         */
        public boolean isEntryValid()
        {
            ServerLevel dimension = getDimension();

            if(dimension == null)
            {
                SculkHorde.LOGGER.error("Failed To Validate Bee Nest Entry. Dimension was null.");
                return false;
            }
            else if(dimension.getBlockEntity(position) == null)
            {
                SculkHorde.LOGGER.error("Failed To Validate Bee Nest Entry. Block Entity was null.");
                return false;
            }

            return dimension.getBlockState(position).getBlock().equals(ModBlocks.SCULK_BEE_NEST_BLOCK.get());
        }


        /**
         * is Hive enabled?
         * @return True if enabled, false otherwise
         */
        public boolean isOccupantsExistingDisabled()
        {
            if(getDimension() == null) { return true; }
            return SculkBeeNestBlock.isNestClosed(getDimension().getBlockState(position));
        }

        /**
         * Sets Hive to deny bees leaving
         */
        public void disableOccupantsExiting()
        {
            if(getDimension() == null) { return; }
            SculkBeeNestBlock.setNestClosed(getDimension(), getDimension().getBlockState(position), position);
        }


        /**
         * Sets Hive to allow bees leaving
         */
        public void enableOccupantsExiting()
        {
            if(getDimension() == null) { return; }
            SculkBeeNestBlock.setNestOpen(getDimension(), getDimension().getBlockState(position), position);
        }


        public Optional<NodeEntry> getClosestNode(BlockPos pos)
        {
            Optional<NodeEntry> closestEntry = Optional.empty();
            for(NodeEntry entry : getSaveData().getNodeEntries())
            {
                // If we are not in the same dimension
                if(!entry.dimension.equals(dimension))
                {
                    continue;
                }

                if(closestEntry.isEmpty())
                {
                    closestEntry = Optional.of(entry);
                }
                //If entry is closer than our current closest entry
                else if(getBlockDistance(pos, entry.position) < getBlockDistance(pos, closestEntry.get().position))
                {
                    closestEntry = Optional.of(entry);
                }
            }
            return closestEntry;
        }

        /**
         * Checks list of node entries and finds the closest one.
         * It then sets the parentNodePosition to be the position of
         * the closest node.
         */
        public void setParentNodeToClosest()
        {
            //Make sure nodeEntries isn't null and nodeEntries isn't empty
            if(getSaveData().getNodeEntries() != null && !getSaveData().getNodeEntries().isEmpty())
            {
                Optional<NodeEntry> closestEntry = Optional.empty();
                for(NodeEntry entry : getSaveData().getNodeEntries())
                {
                    // If we are not in the same dimension
                    if(!entry.dimension.equals(dimension))
                    {
                        continue;
                    }

                    if(Optional.of(entry).isEmpty())
                    {
                        SculkHorde.LOGGER.error("Failed To Set Parent Node To Closest. Node Entry was null.");
                        continue;
                    }

                    if(closestEntry.isEmpty())
                    {
                        closestEntry = Optional.of(entry);
                    }
                    //If entry is closer than our current closest entry
                    // NOTE: We shouldnt need the isPresent() check here but it was throwing an exception
                    else if(closestEntry.isPresent() && getBlockDistance(position, entry.position) < getBlockDistance(position, closestEntry.get().position))
                    {
                        closestEntry = Optional.of(entry);
                    }
                }

                if(closestEntry.isPresent() && closestEntry.get().getPosition() != null) { parentNodePosition = closestEntry.get().getPosition(); }
            }
        }


        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            if(dimension != null) nbt.putString("dimension", dimension.location().toString());
            if(parentNodePosition != null) nbt.putLong("parentNodePosition", parentNodePosition.asLong());
            return nbt;
        }


        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static BeeNestEntry serialize(CompoundTag nbt)
        {
            ResourceKey<Level> dimensionResourceKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString("dimension")));

            return new BeeNestEntry(dimensionResourceKey, BlockPos.of(nbt.getLong("position")), BlockPos.of(nbt.getLong("parentNodePosition")));
        }
    }

    /**
     * This class is a representation of the actual
     * Sculk Nodes in the world that the horde has access
     * to. It allows the gravemind to keep track of all.
     */
    private static class HostileEntry
    {
        private final String identifier; //The String that is the class name identifier of the mob. Example: class net.minecraft.entity.monster.SpiderEntity

        /**
         * Default Constructor
         * @param identifierIn The String that is the class name identifier of the mob. <br>
         * Example: class net.minecraft.entity.monster.SpiderEntity
         */
        public HostileEntry(String identifierIn)
        {
            identifier = identifierIn;
        }


        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("identifier", identifier);
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static HostileEntry serialize(CompoundTag nbt)
        {
            return new HostileEntry(nbt.getString("identifier"));
        }

    }

    public static class DeathAreaEntry
    {
        private final BlockPos position; // The Location of the Death Area
        private int deathCount; // The number of deaths that have occurred in this area

        private ResourceKey<Level> dimension;

        public DeathAreaEntry(ServerLevel dimension, BlockPos positionIn)
        {
            position = positionIn;
            deathCount = 1;
            this.dimension = dimension.dimension();
        }

        public DeathAreaEntry(ResourceKey<Level> dimension, BlockPos positionIn, int deathCountIn)
        {
            position = positionIn;
            deathCount = deathCountIn;
            this.dimension = dimension;
        }

        public ServerLevel getDimension()
        {
            return ServerLifecycleHooks.getCurrentServer().overworld().getServer().getLevel(dimension);
        }

        public void setDeathCount(int deathCountIn)
        {
            deathCount = deathCountIn;
        }

        public int getDeathCount()
        {
            return deathCount;
        }

        public void iterateDeathCount()
        {
            deathCount++;
        }

        public BlockPos getPosition()
        {
            return position;
        }

        public boolean isValid()
        {
            boolean isDimensionValid = getDimension() != null;
            boolean isPositionValid = getPosition() != null;

            return isDimensionValid && isPositionValid;
        }

        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            nbt.putInt("deathCount", deathCount);
            if(dimension != null) nbt.putString("dimension", dimension.location().toString());
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static DeathAreaEntry serialize(CompoundTag nbt) {

            ResourceKey<Level> dimensionResourceKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString("dimension")));
            return new DeathAreaEntry(dimensionResourceKey, BlockPos.of(nbt.getLong("position")), nbt.getInt("deathCount"));
        }
    }

    public static class AreaOfInterestEntry
    {
        private final BlockPos position; // The Location of the Death Area
        private final ResourceKey<Level> dimension;
        private long ticksSinceLastRaid;

        public AreaOfInterestEntry(ServerLevel dimension, BlockPos positionIn)
        {
            this.dimension = dimension.dimension();
            position = positionIn;
        }

        public AreaOfInterestEntry(ResourceKey<Level> dimension, BlockPos positionIn, long ticksSinceLastRaidIn)
        {
            this.dimension = dimension;
            position = positionIn;
            ticksSinceLastRaid = ticksSinceLastRaidIn;
        }

        public BlockPos getPosition()
        {
            return position;
        }

        public ServerLevel getDimension()
        {
            return ServerLifecycleHooks.getCurrentServer().overworld().getServer().getLevel(dimension);
        }

        public boolean isInNoRaidZone()
        {
            for(NoRaidZoneEntry entry : getSaveData().getNoRaidZoneEntries())
            {
                if(entry.isBlockPosInRadius(entry.getDimension(), getPosition()))
                {
                    return true;
                }
            }
            return false;
        }

        public boolean isEntryValid()
        {
            boolean isDimensionValid = getDimension() != null;
            boolean isPositionValid = getPosition() != null;

            return isDimensionValid && isPositionValid;
        }

        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            nbt.putLong("ticksSinceLastRaid", ticksSinceLastRaid);
            nbt.putString("dimension", dimension.location().toString());
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static AreaOfInterestEntry serialize(CompoundTag nbt) {
            ResourceKey<Level> dimensionResourceKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString("dimension")));
            return new AreaOfInterestEntry(dimensionResourceKey, BlockPos.of(nbt.getLong("position")), nbt.getLong("ticksSinceLastRaid"));
        }
    }

    public static class NoRaidZoneEntry
    {
        private final BlockPos position; // The Location
        private final int radius;
        private final long timeOfCreation; // this.level.getGameTime();
        private long durationInTicksUntilExpiration;

        private final ResourceKey<Level> dimension;

        public NoRaidZoneEntry(ServerLevel dimension, BlockPos positionIn, int radiusIn, long gameTimeStampIn, long durationUntilExpirationIn)
        {
            this.dimension = dimension.dimension();
            position = positionIn;
            radius = radiusIn;
            timeOfCreation = gameTimeStampIn;
            durationInTicksUntilExpiration = durationUntilExpirationIn;
        }

        public NoRaidZoneEntry(ResourceKey<Level> dimension, BlockPos positionIn, int radiusIn, long gameTimeStampIn, long durationUntilExpirationIn)
        {
            this.dimension = dimension;
            position = positionIn;
            radius = radiusIn;
            timeOfCreation = gameTimeStampIn;
            durationInTicksUntilExpiration = durationUntilExpirationIn;
        }

        public ServerLevel getDimension()
        {
            return ServerLifecycleHooks.getCurrentServer().overworld().getServer().getLevel(dimension);
        }

        public BlockPos getPosition()
        {
            return position;
        }

        public int getRadius()
        {
            return radius;
        }

        public long getTimeOfCreation()
        {
            return timeOfCreation;
        }

        public long getDurationInTicksUntilExpiration()
        {
            return durationInTicksUntilExpiration;
        }

        public boolean isExpired(long currentTimeStamp)
        {
            long defaultTicksUntilExpiration = TickUnits.convertMinutesToTicks(ModConfig.SERVER.sculk_raid_no_raid_zone_duration_minutes.get());
            long ticksUntilThisNoRaidZoneExpires = getDurationInTicksUntilExpiration();
            // If the user has set a lower duration in the config, we will use that instead
            if(ticksUntilThisNoRaidZoneExpires > defaultTicksUntilExpiration)
            {
                durationInTicksUntilExpiration = defaultTicksUntilExpiration;
            }

            return (currentTimeStamp - getTimeOfCreation()) > getDurationInTicksUntilExpiration();
        }

        public boolean isBlockPosInRadius(ServerLevel level, BlockPos blockPosIn)
        {

            return position.closerThan(blockPosIn, radius) && BlockAlgorithms.areTheseDimensionsEqual(level, getDimension());
        }

        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("position", position.asLong());
            nbt.putInt("radius", radius);
            nbt.putLong("gameTimeStamp", timeOfCreation);
            nbt.putLong("durationUntilExpiration", durationInTicksUntilExpiration);
            nbt.putString("dimension", dimension.location().toString());
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static NoRaidZoneEntry serialize(CompoundTag nbt)
        {
            ResourceKey<Level> dimensionResourceKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString("dimension")));
            return new NoRaidZoneEntry(dimensionResourceKey, BlockPos.of(nbt.getLong("position")), nbt.getInt("radius"), nbt.getLong("gameTimeStamp"), nbt.getLong("durationUntilExpiration"));
        }
    }

    // ###### Player Profile Entries ######

    public ArrayList<PlayerProfileEntry> getPlayerProfileEntries() {
        return playerProfileEntries;
    }

    public static class PlayerProfileEntry
    {
        private final UUID playerUUID; // The Location
        private int relationshipToTheHorde;

        private boolean isVessel = false;
        private boolean isActiveVessel = false;
        private int nodesDestroyed = 0;
        private long timeOfLastHit = 0;

        private int difficultyOfNextHit = 1;

        private static final int MAX_RELATIONSHIP_VALUE = 1000;
        private static final int MIN_RELATIONSHIP_VALUE = -1000;

        public PlayerProfileEntry(Player playerIn)
        {
            this.playerUUID = playerIn.getUUID();
        }

        public PlayerProfileEntry(UUID uuid)
        {
            this.playerUUID = uuid;
        }

        public PlayerProfileEntry(UUID playerIn, int relationshipToTheHordeIn, boolean isVesselIn, boolean isActiveVesselIn, int nodesDestroyed, long timeOfLastHit, int difficultyOfNextHit)
        {
            this.playerUUID = playerIn;
            this.relationshipToTheHorde = relationshipToTheHordeIn;
            this.isVessel = isVesselIn;
            this.isActiveVessel = isActiveVesselIn;
            this.nodesDestroyed = nodesDestroyed;
            this.timeOfLastHit = timeOfLastHit;
            this.difficultyOfNextHit = difficultyOfNextHit;
        }

        public Optional<Player> getPlayer()
        {
            return Optional.ofNullable(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID));
        }

        public UUID getPlayerUUID()
        {
            return playerUUID;
        }

        public boolean isPlayerOnline()
        {
            return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID) != null;
        }

        public void setTimeOfLastHit(long value)
        {
            timeOfLastHit = value;
        }

        public long getTimeOfLastHit()
        {
            return timeOfLastHit;
        }

        public boolean isHitCooldownOver()
        {
            // Cooldown for hits is twice as long as it takes a node to spawn.
            return ServerLifecycleHooks.getCurrentServer().overworld().getGameTime() - getTimeOfLastHit() > TickUnits.convertMinutesToTicks(ModConfig.SERVER.sculk_node_spawn_cooldown_minutes.get() * 2);
        }

        public int getRelationshipToTheHorde()
        {
            return relationshipToTheHorde;
        }

        public void setRelationshipToTheHorde(int value)
        {
            if(value < 0)
            {
                relationshipToTheHorde = Math.max(MIN_RELATIONSHIP_VALUE, value);
            }
            else
            {
                relationshipToTheHorde = Math.min(MAX_RELATIONSHIP_VALUE, value);
            }
        }

        public void increaseOrDecreaseRelationshipToHorde(int value)
        {
            setRelationshipToTheHorde(getRelationshipToTheHorde() + value);
        }

        public void setVessel(boolean value)
        {
            isVessel = value;
        }

        public boolean isVessel()
        {
            return isVessel;
        }

        public void setActiveVessel(boolean value)
        {
            isActiveVessel = value;
        }

        public boolean isActiveVessel()
        {
            return isVessel && isActiveVessel;
        }

        public int getNodesDestroyed() { return nodesDestroyed; }

        public void setNodesDestroyed(int value) { nodesDestroyed = value; }

        public void incrementNodesDestroyed() { setNodesDestroyed(getNodesDestroyed() + 1);}

        public int getDifficultyOfNextHit()
        {
            return difficultyOfNextHit;
        }

        public void increaseDifficultyOfNextHit()
        {
            difficultyOfNextHit = Math.min(3, difficultyOfNextHit + 1);
        }

        public void decreaseDifficultyOfNextHit()
        {
            difficultyOfNextHit = Math.max(1, difficultyOfNextHit - 1);
        }

        /**
         * Making nbt to be stored in memory
         * @return The nbt with our data
         */
        public CompoundTag deserialize()
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putUUID("playerUUID", playerUUID);
            nbt.putInt("relationshipToTheHorde", relationshipToTheHorde);
            nbt.putBoolean("isVessel", isVessel);
            nbt.putBoolean("isActiveVessel", isActiveVessel);
            nbt.putInt("nodesDestroyed", nodesDestroyed);
            nbt.putLong("timeOfLastHit", timeOfLastHit);
            nbt.putInt("difficultyOfNextHit", difficultyOfNextHit);
            return nbt;
        }

        /**
         * Extracting our data from the nbt.
         * @return The nbt with our data
         */
        public static PlayerProfileEntry serialize(CompoundTag nbt)
        {
            return new PlayerProfileEntry(
                    nbt.getUUID("playerUUID"),
                    nbt.getInt("relationshipToTheHorde"),
                    nbt.getBoolean("isVessel"),
                    nbt.getBoolean("isActiveVessel"),
                    nbt.getInt("nodesDestroyed"),
                    nbt.getLong("timeOfLastHit"),
                    nbt.getInt("difficultyOfNextHit")
            );
        }

        @Override
        public String toString() {
            return "PlayerProfileEntry{" +
                    "playerUUID=" + playerUUID +
                    ", username=" + (getPlayer().isEmpty() ? "N/A" : getPlayer().get().getName().getString()) +
                    ", relationshipToTheHorde=" + relationshipToTheHorde +
                    ", isVessel=" + isVessel +
                    ", isActiveVessel=" + isActiveVessel +
                    ", nodesDestroyed=" + nodesDestroyed +
                    ", timeOfLastHit=" + timeOfLastHit +
                    ", difficultyOfNextHit=" + difficultyOfNextHit +
                    '}';
        }
    }
}
