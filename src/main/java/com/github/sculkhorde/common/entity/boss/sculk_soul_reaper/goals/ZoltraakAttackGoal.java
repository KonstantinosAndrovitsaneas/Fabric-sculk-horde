package com.github.sculkhorde.common.entity.boss.sculk_soul_reaper.goals;

import com.github.sculkhorde.common.entity.boss.sculk_soul_reaper.SculkSoulReaperEntity;
import com.github.sculkhorde.common.entity.boss.sculk_soul_reaper.ZoltraakAttackEntity;
import com.github.sculkhorde.util.TickUnits;

public class ZoltraakAttackGoal extends ReaperCastSpellGoal
{


    public ZoltraakAttackGoal(SculkSoulReaperEntity mob) {
        super(mob);
    }

    @Override
    protected int getBaseCastingTime() {
        return TickUnits.convertSecondsToTicks(0.72F);
    }

    @Override
    protected int getCooldownTicksElapsed() {
        return TickUnits.convertSecondsToTicks(8);
    }

    @Override
    protected void playCastingAnimation()
    {
        mob.triggerAnim(SculkSoulReaperEntity.COMBAT_ATTACK_ANIMATION_CONTROLLER_ID, SculkSoulReaperEntity.ZOLTRAAK_SPELL_USE_ID);
    }

    @Override
    protected void doAttackTick() {
        //performTargetedZoltraakAttack(mob, mob.getEyePosition(), mob.getTarget(), DAMAGE);
        ZoltraakAttackEntity.castZoltraakOnEntity(mob, mob.getTarget(), mob.getEyePosition());
        setSpellCompleted();
    }
}