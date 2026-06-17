package com.betterenchanting.world.loot;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.data.EssenceTradeDefinitions;
import com.betterenchanting.data.EssenceTradeDefinitions.EssenceTrade;
import com.betterenchanting.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

public final class EssenceAcquisitionEvents {
    private EssenceAcquisitionEvents() {
    }

    public static void addMiningEssenceFromFortune(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player) || event.getBlockEntity() != null || !event.getState().is(Tags.Blocks.ORES)) {
            return;
        }
        if (enchantmentLevel(event.getLevel(), event.getTool(), Enchantments.FORTUNE) <= 0 || !roll(event.getLevel().getRandom())) {
            return;
        }

        event.getDrops().add(itemEntity(event.getLevel(), event.getPos().getX() + 0.5D, event.getPos().getY() + 0.5D, event.getPos().getZ() + 0.5D, ModItems.MINING_ESSENCE.get()));
    }

    public static void dropLightningEssenceFromChargedCreeper(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper) || !creeper.isPowered() || !creeper.level().isThundering() || !event.isRecentlyHit()) {
            return;
        }
        if (!(creeper.level() instanceof ServerLevel level) || !roll(level.getRandom())) {
            return;
        }

        event.getDrops().add(itemEntity(level, creeper.getX(), creeper.getY() + 0.5D, creeper.getZ(), ModItems.LIGHTNING_ESSENCE.get()));
    }

    public static void addFishingEssences(ItemFishedEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (event.getHookEntity().level().getFluidState(event.getHookEntity().blockPosition()).is(FluidTags.LAVA) && roll(level.getRandom())) {
            event.getDrops().add(new ItemStack(ModItems.FIRE_ESSENCE.get()));
        }

        ItemStack rod = fishingRod(player);
        if (enchantmentLevel(level, rod, Enchantments.LUCK_OF_THE_SEA) > 0 && roll(level.getRandom())) {
            event.getDrops().add(new ItemStack(ModItems.TREASURE_ESSENCE.get()));
        }
    }

    public static void addVillagerTrades(VillagerTradesEvent event) {
        for (EssenceTrade trade : EssenceTradeDefinitions.tradesFor(event.getType())) {
            addTrade(event, trade.level(), new EssenceForEmeralds(trade));
        }
    }

    public static void dropEssencesFromCuring(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof ZombieVillager) || !(event.getOutcome() instanceof Villager villager) || !(villager.level() instanceof ServerLevel level)) {
            return;
        }

        if (roll(level.getRandom())) {
            level.addFreshEntity(itemEntity(level, villager.getX(), villager.getY() + 0.5D, villager.getZ(), ModItems.VITALITY_ESSENCE.get()));
        }
        if (roll(level.getRandom())) {
            level.addFreshEntity(itemEntity(level, villager.getX(), villager.getY() + 0.5D, villager.getZ(), ModItems.PURIFICATION_ESSENCE.get()));
        }
    }

    private static void addTrade(VillagerTradesEvent event, int level, VillagerTrades.ItemListing listing) {
        List<VillagerTrades.ItemListing> trades = event.getTrades().computeIfAbsent(level, ignored -> new ArrayList<>());
        trades.add(listing);
    }

    private static ItemStack fishingRod(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Items.FISHING_ROD)) {
            return mainHand;
        }

        ItemStack offhand = player.getOffhandItem();
        return offhand.is(Items.FISHING_ROD) ? offhand : ItemStack.EMPTY;
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

    private static boolean roll(RandomSource random) {
        return random.nextFloat() < BetterEnchantingConfig.essenceDirectDropChance();
    }

    private static ItemEntity itemEntity(ServerLevel level, double x, double y, double z, Item item) {
        ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(item));
        entity.setDefaultPickUpDelay();
        return entity;
    }

    private record EssenceForEmeralds(EssenceTrade trade) implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, trade.emeraldCost()),
                    new ItemStack(trade.essence(), trade.count()),
                    trade.maxUses(),
                    trade.xp(),
                    trade.priceMultiplier()
            );
        }
    }
}
