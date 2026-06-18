package com.betterenchanting.world.effect;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.config.EffectiveBalance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class CinderstepMobEffect extends MobEffect {
    public CinderstepMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF8A2A);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                BetterEnchanting.id("effect/cinderstep_movement_speed"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> EffectiveBalance.cinderstepSpeedBonusPerLevel() * (double) (amplifier + 1)
        );
    }
}
