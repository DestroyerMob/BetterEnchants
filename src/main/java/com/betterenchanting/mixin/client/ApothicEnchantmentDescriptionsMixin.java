package com.betterenchanting.mixin.client;

import com.betterenchanting.compat.EnchantmentDescriptionsCompat;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_enchanting.util.TooltipUtil", remap = false)
public abstract class ApothicEnchantmentDescriptionsMixin {
    @Inject(method = "applyEnchTooltip", at = @At("RETURN"), require = 0, remap = false)
    private static void betterenchanting$appendEnchantmentDescription(
            Holder<Enchantment> enchantment,
            ItemEnchantments enchantments,
            ItemEnchantments effectiveEnchantments,
            Consumer<Component> tooltip,
            CallbackInfo callbackInfo
    ) {
        EnchantmentDescriptionsCompat.appendApothicDescription(
                enchantment,
                Math.max(
                        enchantments.getLevel(enchantment),
                        effectiveEnchantments.getLevel(enchantment)
                ),
                enchantments,
                tooltip
        );
    }
}
