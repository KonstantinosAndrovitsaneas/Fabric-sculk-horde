package com.github.sculkhorde.common.blockentity;

import com.github.sculkhorde.common.entity.SculkBroodlingEntity;
import com.github.sculkhorde.core.ModBlockEntities;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.gravemind_system.Gravemind;
import com.github.sculkhorde.systems.infestation_systems.block_infestation_system.BlockInfestationSystem;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Random;

public class BroodNestBlockEntity extends BlockEntity {
    protected long lastTickTime = 0;
    protected int tickInterval = TickUnits.convertSecondsToTicks(15);

    public ArrayList<LivingEntity> spawnedEntities = new ArrayList<>();
    public final int MAX_ENTITIES = 6;


    /**
     * The Constructor that takes in properties
     */
    public BroodNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BROOD_NEST_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * Called when loading block entity from world.
     * @param compoundNBT Where NBT data is stored.
     */
    @Override
    public void load(CompoundTag compoundNBT) {
        super.load(compoundNBT);
        //this.storedSculkMass = compoundNBT.getInt(storedSculkMassIdentifier);
    }

    /**
     * ???
     * @param compoundNBT Where NBT data is stored??
     * @return ???
     */
    @Override
    public void saveAdditional(CompoundTag compoundNBT) {

        //compoundNBT.putInt(storedSculkMassIdentifier, this.storedSculkMass);
        super.saveAdditional(compoundNBT);
    }

    /*
    public int getStoredSculkMass()
    {
        return storedSculkMass;
    }

    public void setStoredSculkMass(int value)
    {
        storedSculkMass = Math.max(0, value);
    }

    public void addStoredSculkMass(int value)
    {
        storedSculkMass = Math.max(0, storedSculkMass + value);
    }

     */


    public static void tick(Level level, BlockPos blockPos, BlockState blockState, BroodNestBlockEntity blockEntity)
    {
        // If world is not a server world, return
        if(level.isClientSide)
        {
            return;
        }

        // Delay the spawning of mobs for balance, and so that the mass block can recognise if it is under water.
        // When they are first placed, they are not waterlogged. This will result in spawning a surface unit
        // for the first tick, instead of a aquatic unit.
        if(blockEntity.lastTickTime == 0)
        {
            blockEntity.lastTickTime = level.getGameTime();
        }


        // Tick every 10 seconds
        if(level.getGameTime() - blockEntity.lastTickTime < blockEntity.tickInterval)
        {
            return;
        }

        blockEntity.lastTickTime = level.getGameTime();

        if(!Gravemind.isGravemindActive())
        {
            return;
        }


        if(SculkHorde.populationHandler.isPopulationAtMax())
        {
            return;
        }

        if(blockEntity.spawnedEntities.size() >= blockEntity.MAX_ENTITIES)
        {
            return;
        }

        // Spawn Spiders
        if(EntityAlgorithms.getNonSculkEntitiesAtBlockPos((ServerLevel) level, blockPos, 16).isEmpty())
        {
            return;
        }

        blockEntity.spawnBroodlings(3);
    }

    public void spawnBroodlings(int amount)
    {
        ArrayList<BlockPos> spawnPos = getSpawnPositionsInCube((ServerLevel) level, getBlockPos(), 2, amount);

        if(spawnPos.isEmpty()) { return; }

        for(BlockPos pos : spawnPos)
        {
            if(spawnedEntities.size() >= MAX_ENTITIES)
            {
                break;
            }

            SculkBroodlingEntity broodling = new SculkBroodlingEntity(level, pos);
            level.addFreshEntity(broodling);
            spawnedEntities.add(broodling);
        }
    }

    /**
     * Gets a list of all possible spawns, chooses a specified amount of them.
     * @param worldIn The World
     * @param origin The Origin Position
     * @param length The Length of the cube
     * @param amountOfPositions The amount of positions to get
     * @return A list of the spawn positions
     */
    public ArrayList<BlockPos> getSpawnPositionsInCube(ServerLevel worldIn, BlockPos origin, int length, int amountOfPositions)
    {
        //TODO Can potentially be optimized by not getting all the possible positions
        ArrayList<BlockPos> listOfPossibleSpawns = getSpawnPositions(worldIn, origin, length);
        ArrayList<BlockPos> finalList = new ArrayList<>();
        Random rng = new Random();
        for(int count = 0; count < amountOfPositions && !listOfPossibleSpawns.isEmpty(); count++)
        {
            int randomIndex = rng.nextInt(listOfPossibleSpawns.size());
            //Get random position between 0 and size of list
            finalList.add(listOfPossibleSpawns.get(randomIndex));
            listOfPossibleSpawns.remove(randomIndex);
        }
        return finalList;
    }

    /**
     * Returns true if the block below is a sculk block,
     * and if the two blocks above it are free.
     * @param worldIn The World
     * @param pos The Position to spawn the entity
     * @return True/False
     */
    public boolean isValidSpawnPosition(ServerLevel worldIn, BlockPos pos)
    {
        boolean isBlockBelowCurable = BlockInfestationSystem.isCurable(worldIn, pos.below());
        boolean isBaseBlockReplaceable = worldIn.getBlockState(pos).canBeReplaced(Fluids.WATER);
        boolean isBlockAboveReplaceable = worldIn.getBlockState(pos.above()).canBeReplaced(Fluids.WATER);

        return isBlockBelowCurable && isBaseBlockReplaceable && isBlockAboveReplaceable;

    }

    /**
     * Finds the location of the nearest block given a BlockPos predicate.
     * @param worldIn The world
     * @param origin The origin of the search location
     * @param pDistance The search distance
     * @return The position of the block
     */
    public ArrayList<BlockPos> getSpawnPositions(ServerLevel worldIn, BlockPos origin, double pDistance)
    {
        ArrayList<BlockPos> list = new ArrayList<>();

        //Search area for block
        for(int i = 0; (double)i <= pDistance; i = i > 0 ? -i : 1 - i)
        {
            for(int j = 0; (double)j < pDistance; ++j)
            {
                for(int k = 0; k <= j; k = k > 0 ? -k : 1 - k)
                {
                    for(int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l)
                    {
                        //blockpos$mutable.setWithOffset(origin, k, i - 1, l);
                        BlockPos temp = new BlockPos(origin.getX() + k, origin.getY() + i-1, origin.getZ() + l);

                        //If the block is close enough and is the right blockstate
                        if (origin.closerThan(temp, pDistance) && isValidSpawnPosition(worldIn, temp))
                        {
                            list.add(temp); //add position
                        }
                    }
                }
            }
        }
        //else return empty
        return list;
    }
}
