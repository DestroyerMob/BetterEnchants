package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class BeheadingEnchantmentEvents {
    private BeheadingEnchantmentEvents() {
    }

    public static void dropHead(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        if (event.getSource().getDirectEntity() != attacker) {
            return;
        }

        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || weapon.isEmpty()) {
            weapon = attacker.getMainHandItem();
        }
        if (!isSwordOrAxe(weapon) || enchantmentLevel(level, weapon, ModEnchantments.BEHEADING) <= 0) {
            return;
        }
        if (!isMeleeHeadshot(attacker, victim)) {
            return;
        }

        ItemStack head = headFor(victim);
        if (head.isEmpty() || alreadyHasHeadDrop(event, head.getItem())) {
            return;
        }

        int lootingLevel = enchantmentLevel(level, weapon, Enchantments.LOOTING);
        float chance = Mth.clamp(
                EffectiveBalance.beheadingBaseHeadDropChance() + lootingLevel * EffectiveBalance.beheadingHeadDropChancePerLootingLevel(),
                0.0F,
                1.0F
        );
        if (chance <= 0.0F || level.getRandom().nextFloat() >= chance) {
            return;
        }

        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.5D, victim.getZ(), head);
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    private static boolean isSwordOrAxe(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
    }

    private static boolean isMeleeHeadshot(Player attacker, LivingEntity victim) {
        return HeadshotDetector.isMeleeHeadshot(
                attacker,
                victim,
                EffectiveBalance.beheadingHeadshotLowerEyeBand(),
                EffectiveBalance.beheadingHeadshotUpperEyeBand()
        );
    }

    private static ItemStack headFor(LivingEntity victim) {
        if (victim instanceof Player player) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
            return head;
        }

        Item item = headItemFor(victim.getType());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Item headItemFor(EntityType<?> entityType) {
        if (entityType == EntityType.CREEPER) {
            return Items.CREEPER_HEAD;
        }
        if (entityType == EntityType.SKELETON || entityType == EntityType.STRAY || entityType == EntityType.BOGGED) {
            return Items.SKELETON_SKULL;
        }
        if (entityType == EntityType.WITHER_SKELETON) {
            return Items.WITHER_SKELETON_SKULL;
        }
        if (entityType == EntityType.ZOMBIE
                || entityType == EntityType.HUSK
                || entityType == EntityType.DROWNED
                || entityType == EntityType.ZOMBIE_VILLAGER) {
            return Items.ZOMBIE_HEAD;
        }
        if (entityType == EntityType.PIGLIN || entityType == EntityType.PIGLIN_BRUTE || entityType == EntityType.ZOMBIFIED_PIGLIN) {
            return Items.PIGLIN_HEAD;
        }
        if (entityType == EntityType.ENDER_DRAGON) {
            return Items.DRAGON_HEAD;
        }
        return null;
    }

    private static boolean alreadyHasHeadDrop(LivingDropsEvent event, Item headItem) {
        return event.getDrops().stream().map(ItemEntity::getItem).anyMatch(stack -> stack.is(headItem));
    }

    private static int enchantmentLevel(ServerLevel level, ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty()) {
            return 0;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(enchantment)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
