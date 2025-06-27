package com.github.sculkhorde.systems.event_system.events.HitSquadEvent;

import com.github.sculkhorde.core.ModSavedData;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.event_system.events.HitSquadEvent.HitSquadEvent;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.PlayerProfileHandler;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;

public class HitSquadDispatcherSystem {

    protected static int CHECK_INTERVAL = TickUnits.convertSecondsToTicks(30);
    protected long timeOfLastCheckForDispatch = 0;
    public static int MIN_NODES_DESTROYED = 2;
    public static int MAX_RELATIONSHIP = -100;
    public static int DISTANCE_REQUIRED_FROM_NODE = 100;

    public HitSquadDispatcherSystem()
    {
    }

    protected Optional<Player> getNextTarget()
    {
        Optional<Player> target = Optional.empty();

        int worstReputationSoFar = MAX_RELATIONSHIP + 1;

        for(Player player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
        {
            ModSavedData.PlayerProfileEntry profile = PlayerProfileHandler.getOrCreatePlayerProfile(player);

            if(!profile.isPlayerOnline())
            {
                continue;
            }

            if(EntityAlgorithms.isLivingEntityExplicitDenyTarget(profile.getPlayer().get()))
            {
                continue;
            }

            if(ModSavedData.getSaveData().getNodeEntries().isEmpty())
            {
                continue;
            }

            if(!SculkHorde.gravemind.isEvolutionInMatureState())
            {
                continue;
            }

            boolean hasNotDestroyedEnoughNodes = profile.getNodesDestroyed() < MIN_NODES_DESTROYED;
            boolean hasGoodRelationshipWithHorde = profile.getRelationshipToTheHorde() > MAX_RELATIONSHIP;
            boolean isHitCooldownNotOver = !profile.isHitCooldownOver();

            Optional<ModSavedData.NodeEntry> entry = ModSavedData.getSaveData().getClosestNodeEntry((ServerLevel) player.level(), player.blockPosition());
            // If there is no cloest node, just move on.
            if(entry.isEmpty())
            {
                continue;
            }

            boolean isTooFarFromNode = BlockAlgorithms.getBlockDistanceXZ(player.blockPosition(), entry.get().getPosition()) > DISTANCE_REQUIRED_FROM_NODE;

            if(isTooFarFromNode || isHitCooldownNotOver || hasGoodRelationshipWithHorde || hasNotDestroyedEnoughNodes)
            {
                continue;
            }


            if(target.isEmpty() || profile.getRelationshipToTheHorde() < worstReputationSoFar)
            {
                target = profile.getPlayer();
                worstReputationSoFar = profile.getRelationshipToTheHorde();
            }
        }

        return target;
    }

    public void serverTick()
    {
        ServerLevel level = ServerLifecycleHooks.getCurrentServer().overworld();
        if(Math.abs(level.getGameTime() - timeOfLastCheckForDispatch) < CHECK_INTERVAL)
        {
            return;
        }

        if(SculkHorde.isDebugMode()) {
            SculkHorde.LOGGER.info("HitSquadDispatcherSystem | Checking To See if its time for hit event.");
        }

        Optional<Player> nextTarget = getNextTarget();

        if(nextTarget.isPresent())
        {
            SculkHorde.LOGGER.info("HitSquadDispatcherSystem | The Next Target is " + nextTarget.get().getScoreboardName());
            SculkHorde.eventSystem.addEvent(new HitSquadEvent(nextTarget.get().level().dimension(), nextTarget.get().getUUID()));
            PlayerProfileHandler.getOrCreatePlayerProfile(nextTarget.get()).setTimeOfLastHit(level.getGameTime());
        }

        timeOfLastCheckForDispatch = level.getGameTime();
    }

}
