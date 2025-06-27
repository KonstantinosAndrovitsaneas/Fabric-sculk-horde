package com.github.sculkhorde.common.blockentity;

import com.github.sculkhorde.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;


public class SouliteCoreBlockEntity extends StructureCoreBlockEntity
{

    /**
     * The Constructor that takes in properties
     * @param blockPos The Position
     * @param blockState The Properties
     */
    public SouliteCoreBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        super(ModBlockEntities.SOULITE_CORE_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    @Override
    protected void loadStructureVariants()
    {
        addStructureVariant("sculkhorde:soulite_crystal_structure1");
        addStructureVariant("sculkhorde:soulite_crystal_structure2");
        addStructureVariant("sculkhorde:soulite_crystal_structure3");
        addStructureVariant("sculkhorde:soulite_crystal_structure4");
        addStructureVariant("sculkhorde:soulite_crystal_structure5");
    }

}
