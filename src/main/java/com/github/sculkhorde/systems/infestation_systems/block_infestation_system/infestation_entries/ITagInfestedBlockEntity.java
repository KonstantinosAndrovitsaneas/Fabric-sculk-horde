package com.github.sculkhorde.systems.infestation_systems.block_infestation_system.infestation_entries;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public interface ITagInfestedBlockEntity {

    void setNormalBlockState(BlockState blockState);

    @Nullable
    BlockState getNormalBlockState();

    default BlockState getNormalBlockState(Block defaultBlock) {
        BlockState result = getNormalBlockState();
        if (result == null) return defaultBlock.defaultBlockState();
        return result;
    }
}
