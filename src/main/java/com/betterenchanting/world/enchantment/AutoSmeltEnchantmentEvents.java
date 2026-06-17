package com.betterenchanting.world.enchantment;

import com.betterenchanting.registry.ModEnchantments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class AutoSmeltEnchantmentEvents {
    private AutoSmeltEnchantmentEvents() {
    }

    public static void autoSmeltBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player) || !hasAutoSmelt(event.getLevel(), event.getTool())) {
            return;
        }

        List<ItemEntity> extraDrops = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack smelted = smelt(event.getLevel(), drop.getItem());
            if (smelted.isEmpty()) {
                continue;
            }

            ItemStack firstStack = smelted.split(Math.min(smelted.getMaxStackSize(), smelted.getCount()));
            drop.setItem(firstStack);
            while (!smelted.isEmpty()) {
                ItemStack split = smelted.split(Math.min(smelted.getMaxStackSize(), smelted.getCount()));
                ItemEntity extraDrop = new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), split);
                extraDrop.setDeltaMovement(drop.getDeltaMovement());
                extraDrops.add(extraDrop);
            }
        }
        event.getDrops().addAll(extraDrops);
    }

    private static boolean hasAutoSmelt(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.AUTO_SMELT)
                .map(holder -> stack.getEnchantmentLevel(holder) > 0)
                .orElse(false);
    }

    private static ItemStack smelt(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, input, level)
                .map(RecipeHolder::value)
                .map(recipe -> resultFor(level, recipe, input, stack.getCount()))
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack resultFor(ServerLevel level, SmeltingRecipe recipe, SingleRecipeInput input, int inputCount) {
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return result.copyWithCount(result.getCount() * inputCount);
    }
}
