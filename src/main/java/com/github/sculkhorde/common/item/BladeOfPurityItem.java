package com.github.sculkhorde.common.item;

import com.github.sculkhorde.common.entity.infection.CursorSurfacePurifierEntity;
import com.github.sculkhorde.core.ModMobEffects;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.TickUnits;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeItem;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BladeOfPurityItem extends SwordItem implements IForgeItem {

    public BladeOfPurityItem() {
        this(Tiers.DIAMOND, 4, -3F, new Properties().rarity(Rarity.EPIC).setNoRepair().durability(1561));
    }
    public BladeOfPurityItem(Tier tier, int baseDamage, float baseAttackSpeed, Properties prop) {
        super(tier, baseDamage, baseAttackSpeed, prop);
    }



    @Override
    public boolean hurtEnemy(ItemStack itemStack, LivingEntity targetEntity, LivingEntity ownerEntity) {
        if(!ownerEntity.level().isClientSide())
        {
            AABB hitbox = targetEntity.getBoundingBox().inflate(5);
            for(LivingEntity hitEntity: EntityAlgorithms.getEntitiesExceptOwnerInBoundingBox(ownerEntity, (ServerLevel) ownerEntity.level(), hitbox))
            {
                boolean isSculkLivingEntity = EntityAlgorithms.isSculkLivingEntity.test(hitEntity);
                if(isSculkLivingEntity)
                {
                    // Purify
                    CursorSurfacePurifierEntity purifier = new CursorSurfacePurifierEntity(ownerEntity.level());
                    purifier.setPos(hitEntity.position());
                    purifier.setMaxTransformations(10);
                    purifier.setTickIntervalMilliseconds(10);
                    purifier.setMaxLifeTimeMillis(TimeUnit.SECONDS.toMillis(30));
                    purifier.setMaxRange(32);
                    ownerEntity.level().addFreshEntity(purifier);

                    // Add Effect
                    hitEntity.addEffect(new MobEffectInstance(ModMobEffects.PURITY.get(), TickUnits.convertSecondsToTicks(10), 0));
                }
            }
        }

        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack itemstack = player.getItemInHand(hand);
        if(!itemstack.isDamaged() && !level.isClientSide())
        {
            level.playSound(player, player.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, player.getSoundSource());
            return InteractionResultHolder.success(itemstack);
        }
        return InteractionResultHolder.pass(itemstack);
    }

    @Override
    public @NotNull AABB getSweepHitBox(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity target)
    {
        return target.getBoundingBox().inflate(3.0D, 0.25D, 3.0D);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT))
        {
            tooltip.add(Component.translatable("tooltip.sculkhorde.blade_of_purity.functionality"));
        }
        else if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL))
        {
            tooltip.add(Component.translatable("tooltip.sculkhorde.blade_of_purity.lore"));
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.sculkhorde.default"));
        }
    }
}
