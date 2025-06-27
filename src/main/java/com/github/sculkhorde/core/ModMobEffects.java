package com.github.sculkhorde.core;

import com.github.sculkhorde.common.effect.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMobEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SculkHorde.MOD_ID);
    public static final RegistryObject<SculkBurrowedEffect> SCULK_INFECTION = EFFECTS.register("sculk_infected", SculkBurrowedEffect::new);
    public static final RegistryObject<SculkLureEffect> SCULK_LURE = EFFECTS.register("sculk_lure", SculkLureEffect::new);
    public static final RegistryObject<PurityEffect> PURITY = EFFECTS.register("purity", PurityEffect::new);
    public static final RegistryObject<DiseasedCystsEffect> DISEASED_CYSTS = EFFECTS.register("diseased_cysts", DiseasedCystsEffect::new);
    public static final RegistryObject<SculkVesselEffect> SCULK_VESSEL = EFFECTS.register("sculk_vessel", SculkVesselEffect::new);
    public static final RegistryObject<CorrodingEffect> CORRODED = EFFECTS.register("corroded", CorrodingEffect::new);
    public static final RegistryObject<DenseEffect> DENSE = EFFECTS.register("dense", DenseEffect::new);
    public static final RegistryObject<SoulDisruptionEffect> SOUL_DISRUPTION = EFFECTS.register("soul_disruption", SoulDisruptionEffect::new);
    public static final RegistryObject<DiseasedAtmosphereEffect> DISEASED_ATMOSPHERE = EFFECTS.register("diseased_atmosphere", DiseasedAtmosphereEffect::new);
    public static final RegistryObject<SculkFogEffect> SCULK_FOG = EFFECTS.register("sculk_fog", SculkFogEffect::new);
    public static final RegistryObject<NeurotoxinStage1Effect> NEUROTOXIN_STAGE1 = EFFECTS.register("neurotoxin_stage1", NeurotoxinStage1Effect::new);
    public static final RegistryObject<NeurotoxinStage2Effect> NEUROTOXIN_STAGE2 = EFFECTS.register("neurotoxin_stage2", NeurotoxinStage2Effect::new);
    public static final RegistryObject<NeurotoxinStage3Effect> NEUROTOXIN_STAGE3 = EFFECTS.register("neurotoxin_stage3", NeurotoxinStage3Effect::new);

}
