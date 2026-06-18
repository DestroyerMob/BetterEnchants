package com.betterenchanting.mixin;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.data.EnchantmentFusionRecipes;
import com.betterenchanting.data.EnchantmentLevelRules;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Shadow
    @Final
    private DataSlot cost;

    @Shadow
    public abstract int getCost();

    @Inject(method = "createResult", at = @At("RETURN"))
    private void betterenchanting$adjustAnvilCost(CallbackInfo callbackInfo) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        if (!menu.getSlot(AnvilMenu.RESULT_SLOT).hasItem()) {
            return;
        }

        if (EffectiveBalance.usesAdditiveAnvilLevelMerging()) {
            betterenchanting$applyAdditiveAnvilLevels(menu);
        }
        EnchantmentFusionRecipes.apply(betterenchanting$getPlayer().level().registryAccess(), menu.getSlot(AnvilMenu.RESULT_SLOT).getItem());
        EnchantmentLevelRules.clampEnchantments(menu.getSlot(AnvilMenu.RESULT_SLOT).getItem());

        if (betterenchanting$isMaterialRepair(menu)) {
            this.cost.set(0);
            betterenchanting$keepOriginalRepairCost(menu);
            return;
        }

        this.cost.set(Math.min(this.getCost(), EffectiveBalance.anvilMaxCost()));
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void betterenchanting$allowFreeMaterialRepair(Player player, boolean hasItem, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (hasItem && betterenchanting$isMaterialRepair((AnvilMenu) (Object) this)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40), require = 0)
    private int betterenchanting$removeTooExpensiveCreateResultCutoff(int original) {
        return Integer.MAX_VALUE;
    }

    @Redirect(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z"
            )
    )
    private boolean betterenchanting$allowFusionIngredientsInAnvil(Holder<Enchantment> first, Holder<Enchantment> second) {
        return Enchantment.areCompatible(first, second)
                || EnchantmentFusionRecipes.areRecipeIngredients(betterenchanting$getPlayer().level().registryAccess(), first, second);
    }

    private Player betterenchanting$getPlayer() {
        return ((ItemCombinerMenuAccessor) this).betterenchanting$getPlayer();
    }

    private static boolean betterenchanting$isMaterialRepair(AnvilMenu menu) {
        ItemStack left = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        return !left.isEmpty()
                && !right.isEmpty()
                && !result.isEmpty()
                && left.isDamageableItem()
                && left.getItem().isValidRepairItem(left, right)
                && result.getDamageValue() < left.getDamageValue();
    }

    private static void betterenchanting$applyAdditiveAnvilLevels(AnvilMenu menu) {
        ItemStack left = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        if (left.isEmpty() || right.isEmpty() || result.isEmpty()) {
            return;
        }

        ItemEnchantments leftEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(left);
        ItemEnchantments rightEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(right);
        if (leftEnchantments.isEmpty() || rightEnchantments.isEmpty()) {
            return;
        }

        EnchantmentHelper.updateEnchantments(result, mutable -> {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : rightEnchantments.entrySet()) {
                Holder<Enchantment> enchantment = entry.getKey();
                int leftLevel = leftEnchantments.getLevel(enchantment);
                int rightLevel = entry.getIntValue();
                if (leftLevel > 0 && rightLevel > 0 && mutable.getLevel(enchantment) > 0) {
                    mutable.set(enchantment, EnchantmentLevelRules.clampLevel(enchantment, leftLevel + rightLevel));
                }
            }
        });
    }

    private static void betterenchanting$keepOriginalRepairCost(AnvilMenu menu) {
        ItemStack left = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        int originalRepairCost = left.getOrDefault(DataComponents.REPAIR_COST, 0);
        if (originalRepairCost > 0) {
            result.set(DataComponents.REPAIR_COST, originalRepairCost);
        } else {
            result.remove(DataComponents.REPAIR_COST);
        }
    }
}
