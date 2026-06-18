package com.betterenchanting.world.effect;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.config.EffectiveBalance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class CinderstepMobEffect extends MobEffect {
    private static final double DEFAULT_SPEED_BONUS_PER_LEVEL = 0.06D;

    public CinderstepMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF8A2A);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                BetterEnchanting.id("effect/cinderstep_movement_speed"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                CinderstepMobEffect::movementSpeedBonus
        );
    }

    private static double movementSpeedBonus(int amplifier) {
        try {
            return EffectiveBalance.cinderstepSpeedBonusPerLevel() * (double) (amplifier + 1);
        } catch (IllegalStateException ignored) {
            return DEFAULT_SPEED_BONUS_PER_LEVEL * (double) (amplifier + 1);
        }
    }
}
