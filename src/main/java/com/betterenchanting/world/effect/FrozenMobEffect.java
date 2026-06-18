package com.betterenchanting.world.effect;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class FrozenMobEffect extends MobEffect {
    public FrozenMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x6EE7FF);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                BetterEnchanting.id("effect/frozen_movement_speed"),
                -1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
                Attributes.FLYING_SPEED,
                BetterEnchanting.id("effect/frozen_flying_speed"),
                -1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
                Attributes.JUMP_STRENGTH,
                BetterEnchanting.id("effect/frozen_jump_strength"),
                -1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
