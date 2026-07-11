package com.betterenchanting.integration.jei;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModItems;
import java.math.BigDecimal;
import java.math.RoundingMode;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

final class EssenceAcquisitionInfo {
    private static final String KEY_PREFIX = "jei.betterenchanting.essence_acquisition.";

    private EssenceAcquisitionInfo() {
    }

    static void register(IRecipeRegistration registration) {
        String directChance = percent(EffectiveBalance.essenceDirectDropChance());
        String miningChance = percent(EffectiveBalance.miningEssenceDropChance());

        add(registration, ModItems.FIRE_ESSENCE.get(), "fire", directChance);
        add(registration, ModItems.FROST_ESSENCE.get(), "frost");
        add(registration, ModItems.LIGHTNING_ESSENCE.get(), "lightning", directChance);
        add(registration, ModItems.PHYSICAL_ESSENCE.get(), "physical");
        add(registration, ModItems.MINING_ESSENCE.get(), "mining", miningChance);
        add(registration, ModItems.DEFENSIVE_ESSENCE.get(), "defensive");
        add(registration, ModItems.VITALITY_ESSENCE.get(), "vitality", directChance);
        add(registration, ModItems.MOBILITY_ESSENCE.get(), "mobility");
        add(registration, ModItems.VOID_ESSENCE.get(), "void");
        add(registration, ModItems.PURIFICATION_ESSENCE.get(), "purification", directChance);
    }

    private static void add(IRecipeRegistration registration, ItemLike essence, String key, Object... arguments) {
        registration.addIngredientInfo(
                essence,
                Component.translatable(KEY_PREFIX + key, arguments)
        );
    }

    private static String percent(float chance) {
        BigDecimal value = BigDecimal.valueOf(chance)
                .multiply(BigDecimal.valueOf(100L))
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return value.toPlainString() + "%";
    }
}
