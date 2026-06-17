package com.betterenchanting.mixin;

import com.betterenchanting.config.BetterEnchantingConfig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerExperienceMixin {
    @Inject(method = "getXpNeededForNextLevel", at = @At("HEAD"), cancellable = true)
    private void betterenchanting$useConfiguredExperienceCurve(CallbackInfoReturnable<Integer> callbackInfo) {
        if (BetterEnchantingConfig.usesLinearExperienceCurve()) {
            callbackInfo.setReturnValue(BetterEnchantingConfig.xpNeededForNextLevel());
        }
    }
}
