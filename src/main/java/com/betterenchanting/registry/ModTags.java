package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModTags {
    private ModTags() {
    }

    public static final class Items {
        public static final TagKey<Item> ESSENCES = item("essences");

        public static final TagKey<Item> ARMOR = item("armor");
        public static final TagKey<Item> ARMOUR = item("armour");
        public static final TagKey<Item> ARMOR_ALL = item("armor/all");
        public static final TagKey<Item> ARMOR_HELMETS = item("armor/helmets");
        public static final TagKey<Item> ARMOR_BODY = item("armor/body_armor");
        public static final TagKey<Item> ARMOR_LEGGINGS = item("armor/leggings");
        public static final TagKey<Item> ARMOR_BOOTS = item("armor/boots");

        public static final TagKey<Item> TOOLS = item("tools");
        public static final TagKey<Item> TOOLS_ALL = item("tools/all");
        public static final TagKey<Item> HARVESTERS = item("harvesters");
        public static final TagKey<Item> TOOL_HARVESTERS = item("tools/harvesters");
        public static final TagKey<Item> TOOL_PICKAXES = item("tools/pickaxes");
        public static final TagKey<Item> TOOL_AXES = item("tools/axes");
        public static final TagKey<Item> TOOL_SHOVELS = item("tools/shovels");
        public static final TagKey<Item> TOOL_HOES = item("tools/hoes");
        public static final TagKey<Item> TOOL_SHEARS = item("tools/shears");
        public static final TagKey<Item> TOOL_FISHING_RODS = item("tools/fishing_rods");
        public static final TagKey<Item> TOOL_BRUSHES = item("tools/brushes");
        public static final TagKey<Item> TOOL_FLINT_AND_STEEL = item("tools/flint_and_steel");

        public static final TagKey<Item> WEAPONS = item("weapons");
        public static final TagKey<Item> WEAPONS_ALL = item("weapons/all");
        public static final TagKey<Item> WEAPON_MELEE = item("weapons/melee");
        public static final TagKey<Item> WEAPON_RANGED = item("weapons/ranged");
        public static final TagKey<Item> WEAPON_SWORDS = item("weapons/swords");
        public static final TagKey<Item> WEAPON_AXES = item("weapons/axes");
        public static final TagKey<Item> WEAPON_MACES = item("weapons/maces");
        public static final TagKey<Item> WEAPON_BOWS = item("weapons/bows");
        public static final TagKey<Item> WEAPON_CROSSBOWS = item("weapons/crossbows");
        public static final TagKey<Item> WEAPON_TRIDENTS = item("weapons/tridents");

        private Items() {
        }

        private static TagKey<Item> item(String path) {
            return TagKey.create(Registries.ITEM, BetterEnchanting.id(path));
        }
    }

    public static final class Enchantments {
        public static final TagKey<Enchantment> FIRE = enchantment("fire");
        public static final TagKey<Enchantment> FROST = enchantment("frost");
        public static final TagKey<Enchantment> LIGHTNING = enchantment("lightning");
        public static final TagKey<Enchantment> PHYSICAL = enchantment("physical");
        public static final TagKey<Enchantment> MINING = enchantment("mining");
        public static final TagKey<Enchantment> DEFENSIVE = enchantment("defensive");
        public static final TagKey<Enchantment> VITALITY = enchantment("vitality");
        public static final TagKey<Enchantment> MOBILITY = enchantment("mobility");
        public static final TagKey<Enchantment> VOID = enchantment("void");
        public static final TagKey<Enchantment> TREASURE = enchantment("treasure");

        public static final TagKey<Enchantment> TARGET_ARMOR = enchantment("targets/armor");
        public static final TagKey<Enchantment> TARGET_ARMOR_HELMETS = enchantment("targets/armor/helmets");
        public static final TagKey<Enchantment> TARGET_ARMOR_BODY = enchantment("targets/armor/body_armor");
        public static final TagKey<Enchantment> TARGET_ARMOR_LEGGINGS = enchantment("targets/armor/leggings");
        public static final TagKey<Enchantment> TARGET_ARMOR_BOOTS = enchantment("targets/armor/boots");

        public static final TagKey<Enchantment> TARGET_TOOLS = enchantment("targets/tools");
        public static final TagKey<Enchantment> TARGET_TOOL_HARVESTERS = enchantment("targets/tools/harvesters");
        public static final TagKey<Enchantment> TARGET_TOOL_PICKAXES = enchantment("targets/tools/pickaxes");
        public static final TagKey<Enchantment> TARGET_TOOL_AXES = enchantment("targets/tools/axes");
        public static final TagKey<Enchantment> TARGET_TOOL_SHOVELS = enchantment("targets/tools/shovels");
        public static final TagKey<Enchantment> TARGET_TOOL_HOES = enchantment("targets/tools/hoes");
        public static final TagKey<Enchantment> TARGET_TOOL_SHEARS = enchantment("targets/tools/shears");
        public static final TagKey<Enchantment> TARGET_TOOL_FISHING_RODS = enchantment("targets/tools/fishing_rods");
        public static final TagKey<Enchantment> TARGET_TOOL_BRUSHES = enchantment("targets/tools/brushes");
        public static final TagKey<Enchantment> TARGET_TOOL_FLINT_AND_STEEL = enchantment("targets/tools/flint_and_steel");

        public static final TagKey<Enchantment> TARGET_WEAPONS = enchantment("targets/weapons");
        public static final TagKey<Enchantment> TARGET_WEAPON_MELEE = enchantment("targets/weapons/melee");
        public static final TagKey<Enchantment> TARGET_WEAPON_RANGED = enchantment("targets/weapons/ranged");
        public static final TagKey<Enchantment> TARGET_WEAPON_SWORDS = enchantment("targets/weapons/swords");
        public static final TagKey<Enchantment> TARGET_WEAPON_AXES = enchantment("targets/weapons/axes");
        public static final TagKey<Enchantment> TARGET_WEAPON_MACES = enchantment("targets/weapons/maces");
        public static final TagKey<Enchantment> TARGET_WEAPON_BOWS = enchantment("targets/weapons/bows");
        public static final TagKey<Enchantment> TARGET_WEAPON_CROSSBOWS = enchantment("targets/weapons/crossbows");
        public static final TagKey<Enchantment> TARGET_WEAPON_TRIDENTS = enchantment("targets/weapons/tridents");

        private Enchantments() {
        }

        private static TagKey<Enchantment> enchantment(String path) {
            return TagKey.create(Registries.ENCHANTMENT, BetterEnchanting.id(path));
        }
    }
}
