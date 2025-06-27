package com.github.sculkhorde.systems.infestation_systems.block_infestation_system.infestation_entries;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface ITagInfestedBlock {
    ITagInfestedBlockEntity getTagInfestedBlockEntity(Level level, BlockPos blockPos);

}
