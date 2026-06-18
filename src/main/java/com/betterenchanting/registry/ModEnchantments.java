package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {
    public static final ResourceKey<Enchantment> AUTO_SMELT = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("auto_smelt"));
    public static final ResourceKey<Enchantment> CURSE_OF_REBOUND = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("curse_of_rebound"));
    public static final ResourceKey<Enchantment> FORTUNES_TOUCH = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("fortunes_touch"));
    public static final ResourceKey<Enchantment> GELBOUND = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("gelbound"));
    public static final ResourceKey<Enchantment> HARVEST = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("harvest"));
    public static final ResourceKey<Enchantment> PERFECT_STRIKE = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("perfect_strike"));
    public static final ResourceKey<Enchantment> SEISMIC_CUSHION = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("seismic_cushion"));
    public static final ResourceKey<Enchantment> SHOCKING = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("shocking"));
    public static final ResourceKey<Enchantment> TREE_CAPITATOR = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("tree_capitator"));
    public static final ResourceKey<Enchantment> VACUUM = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("vacuum"));
    public static final ResourceKey<Enchantment> VEIN_MINER = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("vein_miner"));
    public static final ResourceKey<Enchantment> VERDANT_REGROWTH = ResourceKey.create(Registries.ENCHANTMENT, BetterEnchanting.id("verdant_regrowth"));

    private ModEnchantments() {
    }
}
