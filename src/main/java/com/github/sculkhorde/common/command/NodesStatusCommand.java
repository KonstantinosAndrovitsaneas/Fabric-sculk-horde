package com.github.sculkhorde.common.command;

import com.github.sculkhorde.common.block.SculkNodeBlock;
import com.github.sculkhorde.core.ModSavedData;
import com.github.sculkhorde.core.SculkHorde;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class NodesStatusCommand implements Command<CommandSourceStack> {

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {

        return Commands.literal("nodestatus")
                .executes(new NodesStatusCommand());

    }

    @Override
    public int run(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSuccess(()->Component.literal(
                "Sculk Nodes Present: " + ModSavedData.getSaveData().getNodeEntries().size()
                        + "\n"
                        + " Are there too many nodes? " + (ModSavedData.getSaveData().getNodeEntries().size() >= SculkHorde.gravemind.sculk_node_limit)
                        + "\n"
                        + "Is Node Cooldown Over: " + ModSavedData.getSaveData().isNodeSpawnCooldownOver()
                        + "\n"
                        + "Minutes Remaining on Cooldown: " + ModSavedData.getSaveData().getMinutesRemainingUntilNodeSpawn()
                        + "\n"
                        + "Mass Needed for Node Spawn: " + (SculkNodeBlock.SPAWN_NODE_COST + SculkNodeBlock.SPAWN_NODE_BUFFER)
                        + "\n"
                        + "Is Enough Mass Present for Node Spawn: " + (ModSavedData.getSaveData().getSculkAccumulatedMass() >= SculkNodeBlock.SPAWN_NODE_COST + SculkNodeBlock.SPAWN_NODE_BUFFER)
                        + "\n"
                        + "Is the Horde Defeated: " + ModSavedData.getSaveData().isHordeDefeated()
                ), false);
        return 0;
    }

}
