package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.data.EnchantmentFusionRecipes;
import com.betterenchanting.data.EnchantmentLevelRules;
import com.betterenchanting.registry.ModEnchantments;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class FortunesTouchEnchantmentEvents {
    private FortunesTouchEnchantmentEvents() {
    }

    public static void fuseAnvilOutput(AnvilRepairEvent event) {
        fuseFortunesTouch(event.getEntity().level().registryAccess(), event.getOutput());
    }

    public static boolean fuseFortunesTouch(RegistryAccess registryAccess, ItemStack stack) {
        return EnchantmentFusionRecipes.apply(registryAccess, stack);
    }

    public static void fortunesTouchBlockDrops(BlockDropsEvent event) {
        int fortunesTouchLevel = fortunesTouchLevel(event.getLevel().registryAccess(), event.getTool());
        if (event.isCanceled() || fortunesTouchLevel <= 0) {
            return;
        }

        ServerLevel level = event.getLevel();
        List<ItemEntity> ordinaryDrops = copyDrops(level, event.getDrops());
        List<ItemStack> silkDrops = Block.getDrops(
                event.getState(),
                level,
                event.getPos(),
                event.getBlockEntity(),
                event.getBreaker(),
                silkTouchTool(level.registryAccess(), event.getTool())
        );

        event.getDrops().clear();
        addStacksAtBlockCenter(event, silkDrops);
        if (!ordinaryDrops.isEmpty() && level.random.nextFloat() < secondaryDropChance(fortunesTouchLevel)) {
            event.getDrops().addAll(ordinaryDrops);
        }
    }

    private static int fortunesTouchLevel(RegistryAccess registryAccess, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return registryAccess.registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.FORTUNES_TOUCH)
                .map(holder -> EnchantmentLevelRules.clampLevel(holder, stack.getEnchantmentLevel(holder)))
                .orElse(0);
    }

    private static float secondaryDropChance(int fortunesTouchLevel) {
        return Math.min(
                EffectiveBalance.fortunesTouchSecondaryDropMaxChance(),
                fortunesTouchLevel * EffectiveBalance.fortunesTouchSecondaryDropChancePerLevel()
        );
    }

    private static ItemStack silkTouchTool(RegistryAccess registryAccess, ItemStack tool) {
        ItemStack silkTool = tool.copy();
        Registry<Enchantment> enchantments = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> fortunesTouch = enchantments.getHolder(ModEnchantments.FORTUNES_TOUCH);
        Optional<Holder.Reference<Enchantment>> silkTouch = enchantments.getHolder(Enchantments.SILK_TOUCH);
        if (silkTouch.isEmpty()) {
            return silkTool;
        }

        EnchantmentHelper.updateEnchantments(silkTool, mutable -> {
            fortunesTouch.ifPresent(holder -> mutable.removeIf(enchantment -> enchantment.equals(holder)));
            mutable.set(silkTouch.get(), 1);
        });
        return silkTool;
    }

    private static List<ItemEntity> copyDrops(ServerLevel level, Collection<ItemEntity> drops) {
        List<ItemEntity> copies = new ArrayList<>();
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            ItemEntity copy = new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), stack.copy());
            copy.setDeltaMovement(drop.getDeltaMovement());
            copies.add(copy);
        }
        return copies;
    }

    private static void addStacksAtBlockCenter(BlockDropsEvent event, List<ItemStack> stacks) {
        double x = event.getPos().getX() + 0.5D;
        double y = event.getPos().getY() + 0.5D;
        double z = event.getPos().getZ() + 0.5D;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                event.getDrops().add(new ItemEntity(event.getLevel(), x, y, z, stack.copy()));
            }
        }
    }
}
