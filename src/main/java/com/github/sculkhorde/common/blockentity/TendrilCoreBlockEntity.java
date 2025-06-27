package com.github.sculkhorde.common.blockentity;

import com.github.sculkhorde.core.ModBlockEntities;
import com.github.sculkhorde.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;


public class TendrilCoreBlockEntity extends StructureCoreBlockEntity
{

    /**
     * The Constructor that takes in properties
     * @param blockPos The Position
     * @param blockState The Properties
     */
    public TendrilCoreBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        super(ModBlockEntities.TENDRIL_CORE_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    @Override
    protected BlockState getBlockToConvertToAfterBuilding()
    {
        return ModBlocks.SCULK_LIVING_ROCK_BLOCK.get().defaultBlockState();
    }

    @Override
    protected void loadStructureVariants()
    {
        addStructureVariant("sculkhorde:tendril_small_1");
        addStructureVariant("sculkhorde:tendril_small_2");
        addStructureVariant("sculkhorde:tendril_small_3");
        addStructureVariant("sculkhorde:tendril_medium_1");
        addStructureVariant("sculkhorde:tendril_medium_2");
        addStructureVariant("sculkhorde:tendril_medium_3");
    }

}
