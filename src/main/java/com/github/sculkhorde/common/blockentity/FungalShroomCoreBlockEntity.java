package com.github.sculkhorde.common.blockentity;

import com.github.sculkhorde.core.ModBlockEntities;
import com.github.sculkhorde.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;


public class FungalShroomCoreBlockEntity extends StructureCoreBlockEntity
{

    /**
     * The Constructor that takes in properties
     * @param blockPos The Position
     * @param blockState The Properties
     */
    public FungalShroomCoreBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        super(ModBlockEntities.FUNGAL_SHROOM_CORE_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    @Override
    protected BlockState getBlockToConvertToAfterBuilding()
    {
        return ModBlocks.FUNGAL_SCULK_STEM_BLOCK.get().defaultBlockState();
    }

    @Override
    protected void loadStructureVariants()
    {
        addStructureVariant("sculkhorde:extra_small_fungal_shroom");
        addStructureVariant("sculkhorde:small_fungal_shroom");
        addStructureVariant("sculkhorde:medium_fungal_shroom");
        //addStructureVariant("sculkhorde:large_fungal_shroom");
        addStructureVariant("sculkhorde:extra_large_fungal_shroom");
    }

}
