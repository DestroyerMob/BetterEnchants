package com.betterenchanting;

import com.betterenchanting.client.ClientModEvents;
import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.data.EnchantmentFusionRecipes;
import com.betterenchanting.data.EnchantmentLimitRules;
import com.betterenchanting.data.EssenceTradeDefinitions;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.data.TagDisplayRules;
import com.betterenchanting.data.TagSimplifier;
import com.betterenchanting.registry.CreativeTabEvents;
import com.betterenchanting.registry.ModEffects;
import com.betterenchanting.registry.ModBlocks;
import com.betterenchanting.registry.ModGameRules;
import com.betterenchanting.registry.ModItems;
import com.betterenchanting.registry.ModMenus;
import com.betterenchanting.world.EnchantmentActivationEvents;
import com.betterenchanting.world.EnchantmentTargetTags;
import com.betterenchanting.world.ItemTagsCommand;
import com.betterenchanting.world.enchantment.AutoSmeltEnchantmentEvents;
import com.betterenchanting.world.enchantment.CurseOfReboundEnchantmentEvents;
import com.betterenchanting.world.enchantment.FortunesTouchEnchantmentEvents;
import com.betterenchanting.world.enchantment.GelboundEnchantmentEvents;
import com.betterenchanting.world.enchantment.HarvestEnchantmentEvents;
import com.betterenchanting.world.enchantment.PerfectStrikeEnchantmentEvents;
import com.betterenchanting.world.enchantment.SeismicCushionEnchantmentEvents;
import com.betterenchanting.world.enchantment.ShockingEnchantmentEvents;
import com.betterenchanting.world.enchantment.TreeCapitatorEnchantmentEvents;
import com.betterenchanting.world.enchantment.VacuumEnchantmentEvents;
import com.betterenchanting.world.enchantment.VeinMinerEnchantmentEvents;
import com.betterenchanting.world.enchantment.VerdantRegrowthEnchantmentEvents;
import com.betterenchanting.world.level.block.EnchantingTableEvents;
import com.betterenchanting.world.loot.EssenceAcquisitionEvents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod(BetterEnchanting.MOD_ID)
public final class BetterEnchanting {
    public static final String MOD_ID = "betterenchanting";

    public BetterEnchanting(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, BetterEnchantingConfig.SPEC);
        ModBlocks.register(modBus);
        ModEffects.register(modBus);
        ModGameRules.init();
        ModItems.register(modBus);
        ModMenus.register(modBus);
        modBus.addListener(CreativeTabEvents::addContents);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModEvents.register(modBus);
        }
        NeoForge.EVENT_BUS.addListener(EnchantingTableEvents::openEssenceEnchantingTable);
        NeoForge.EVENT_BUS.addListener(FortunesTouchEnchantmentEvents::fuseAnvilOutput);
        NeoForge.EVENT_BUS.addListener(CurseOfReboundEnchantmentEvents::reflectPlayerDamage);
        NeoForge.EVENT_BUS.addListener(ShockingEnchantmentEvents::increaseShockedDamage);
        NeoForge.EVENT_BUS.addListener(ShockingEnchantmentEvents::applyShocked);
        NeoForge.EVENT_BUS.addListener(ShockingEnchantmentEvents::emitShockedParticles);
        NeoForge.EVENT_BUS.addListener(PerfectStrikeEnchantmentEvents::trackAttackTarget);
        NeoForge.EVENT_BUS.addListener(PerfectStrikeEnchantmentEvents::openPerfectStrikeWindow);
        NeoForge.EVENT_BUS.addListener(PerfectStrikeEnchantmentEvents::applyPerfectStrikeDamage);
        NeoForge.EVENT_BUS.addListener(PerfectStrikeEnchantmentEvents::randomizeNextCooldown);
        NeoForge.EVENT_BUS.addListener(VerdantRegrowthEnchantmentEvents::repairNearGrowth);
        NeoForge.EVENT_BUS.addListener(HarvestEnchantmentEvents::harvestCrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SeismicCushionEnchantmentEvents::explodeOnCrouchLanding);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GelboundEnchantmentEvents::negateFallDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TreeCapitatorEnchantmentEvents::chopTree);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, VeinMinerEnchantmentEvents::veinMineConnectedBlocks);
        NeoForge.EVENT_BUS.addListener(EssenceAcquisitionEvents::addMiningEssenceFromFortune);
        NeoForge.EVENT_BUS.addListener(EssenceAcquisitionEvents::addFishingEssences);
        NeoForge.EVENT_BUS.addListener(EssenceAcquisitionEvents::addVillagerTrades);
        NeoForge.EVENT_BUS.addListener(EssenceAcquisitionEvents::dropEssencesFromCuring);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, EssenceAcquisitionEvents::dropLightningEssenceFromChargedCreeper);
        NeoForge.EVENT_BUS.addListener(ItemTagsCommand::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EnchantmentActivationEvents::suppressInactiveEnchantments);
        NeoForge.EVENT_BUS.addListener(FortunesTouchEnchantmentEvents::fortunesTouchBlockDrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, AutoSmeltEnchantmentEvents::autoSmeltBlockDrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, VacuumEnchantmentEvents::vacuumBlockDrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, VacuumEnchantmentEvents::vacuumLivingDrops);
        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EssenceDefinitions.ReloadListener());
        event.addListener(new EssenceTradeDefinitions.ReloadListener());
        event.addListener(new EnchantmentLimitRules.ReloadListener());
        event.addListener(new EnchantmentFusionRecipes.ReloadListener());
        event.addListener(new TagDisplayRules.ReloadListener());
        event.addListener(new TagSimplifier.ReloadListener());
        event.addListener(new EnchantmentTargetTags.ReloadListener());
    }
}
