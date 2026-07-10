package com.betterenchanting.mixin;

import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_enchanting.EnchantmentInfo", remap = false)
public abstract class ApothicEnchantmentInfoMixin {
    @Shadow(remap = false)
    @Final
    private Holder<Enchantment> ench;

    @Inject(method = {"getMaxLevel", "maxLevel"}, at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void betterenchanting$preserveRegisteredMaxLevel(CallbackInfoReturnable<Integer> callbackInfo) {
        preserveTreeCapitatorLevel(callbackInfo);
    }

    @Inject(method = {"getMaxLootLevel", "maxLootLevel"}, at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void betterenchanting$preserveRegisteredMaxLootLevel(CallbackInfoReturnable<Integer> callbackInfo) {
        preserveTreeCapitatorLevel(callbackInfo);
    }

    private void preserveTreeCapitatorLevel(CallbackInfoReturnable<Integer> callbackInfo) {
        if (!ench.is(ModEnchantments.TREE_CAPITATOR)) {
            return;
        }

        int registeredMaxLevel = ench.value().getMaxLevel();
        if (callbackInfo.getReturnValueI() < registeredMaxLevel) {
            callbackInfo.setReturnValue(registeredMaxLevel);
        }
    }
}
