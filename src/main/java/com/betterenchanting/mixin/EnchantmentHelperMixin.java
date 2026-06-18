package com.betterenchanting.mixin;

import com.betterenchanting.config.EffectiveBalance;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "modifyDurabilityToRepairFromXp", at = @At("HEAD"), cancellable = true)
    private static void betterenchanting$rollMendingRepair(ServerLevel level, ItemStack stack, int repairBudget, CallbackInfoReturnable<Integer> callbackInfo) {
        int mendingLevel = mendingLevel(level, stack);
        if (mendingLevel <= 0 || repairBudget <= 0) {
            return;
        }

        int denominator = Math.max(
                EffectiveBalance.mendingMinChanceDenominator(),
                EffectiveBalance.mendingBaseChanceDenominator() - mendingLevel * EffectiveBalance.mendingDenominatorReductionPerLevel() + 1
        );
        int durabilityPerSuccess = mendingLevel * EffectiveBalance.mendingDurabilityRepairedPerLevel();
        int repaired = 0;
        for (int roll = 0; roll < repairBudget; roll++) {
            if (level.getRandom().nextInt(denominator) == 0) {
                repaired += durabilityPerSuccess;
            }
        }
        callbackInfo.setReturnValue(repaired);
    }

    private static int mendingLevel(ServerLevel level, ItemStack stack) {
        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.MENDING)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
