package com.betterenchanting.mixin;

import com.betterenchanting.world.level.block.GrindstoneDisenchantEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
    @Inject(method = "createResult", at = @At("RETURN"))
    private void betterenchanting$clearRoutedEnchantments(CallbackInfo callbackInfo) {
        GrindstoneMenu menu = (GrindstoneMenu) (Object) this;
        if (!menu.getSlot(GrindstoneMenu.RESULT_SLOT).hasItem()) {
            return;
        }

        Player player = ((ItemCombinerMenuAccessor) this).betterenchanting$getPlayer();
        ItemStack result = menu.getSlot(GrindstoneMenu.RESULT_SLOT).getItem();
        GrindstoneDisenchantEvents.removeNonCurseEnchantments(player.level().registryAccess(), result);
    }
}
