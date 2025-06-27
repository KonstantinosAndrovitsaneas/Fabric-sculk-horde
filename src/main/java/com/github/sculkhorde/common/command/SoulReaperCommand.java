package com.github.sculkhorde.common.command;

import com.github.sculkhorde.common.entity.boss.sculk_soul_reaper.SculkSoulReaperEntity;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class SoulReaperCommand implements Command<CommandSourceStack> {

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {

        return Commands.literal("spawn_soul_reaper")

                .requires((commandStack) -> commandStack.hasPermission(2))
                .then(Commands.argument("difficulty", IntegerArgumentType.integer(1, 3))
                        .then(Commands.argument("spawn_with_squad", BoolArgumentType.bool())
                            .executes((commandStack) -> {
                                return spawnSoulReaper(
                                        commandStack.getSource(),
                                        IntegerArgumentType.getInteger(commandStack, "difficulty"),
                                        BoolArgumentType.getBool(commandStack, "spawn_with_squad")
                                );
                            })
                        )
                );


    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        return 0;
    }


    private static int spawnSoulReaper(CommandSourceStack context, int difficulty, boolean withSquad) throws CommandSyntaxException {

        SculkSoulReaperEntity.spawnWithDifficulty(context.getLevel(), context.getPosition(), difficulty, withSquad);

        return 1;
    }


}
