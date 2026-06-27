package com.betterenchanting.mixin;

import com.betterenchanting.data.EnchantmentLevelRules;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void betterenchanting$overleveledItemsHaveFoil(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (EnchantmentLevelRules.hasOverleveledEnchantment((ItemStack) (Object) this)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
