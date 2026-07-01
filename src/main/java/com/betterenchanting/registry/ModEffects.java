package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.world.effect.BleedingMobEffect;
import com.betterenchanting.world.effect.CinderstepMobEffect;
import com.betterenchanting.world.effect.FrozenMobEffect;
import com.betterenchanting.world.effect.ShockedMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, BetterEnchanting.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> SHOCKED =
            EFFECTS.register("shocked", ShockedMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> FROZEN =
            EFFECTS.register("frozen", FrozenMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> CINDERSTEP =
            EFFECTS.register("cinderstep", CinderstepMobEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING =
            EFFECTS.register("bleeding", BleedingMobEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
