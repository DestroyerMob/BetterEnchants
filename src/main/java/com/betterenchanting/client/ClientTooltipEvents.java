package com.betterenchanting.client;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.registry.ModTags;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = BetterEnchanting.MOD_ID, value = Dist.CLIENT)
public final class ClientTooltipEvents {
    private static final List<DisplayedItemTag> ITEM_TAGS = List.of(
            tag(ModTags.Items.ESSENCES, "Essence", ChatFormatting.LIGHT_PURPLE),
            tag(ModTags.Items.ARMOR, "Armor", ChatFormatting.BLUE),
            tag(ModTags.Items.ARMOR_HELMETS, "Helmet", ChatFormatting.BLUE),
            tag(ModTags.Items.ARMOR_BODY, "Body Armor", ChatFormatting.BLUE),
            tag(ModTags.Items.ARMOR_LEGGINGS, "Leggings", ChatFormatting.BLUE),
            tag(ModTags.Items.ARMOR_BOOTS, "Boots", ChatFormatting.BLUE),
            tag(ModTags.Items.TOOLS, "Tool", ChatFormatting.GREEN),
            tag(ModTags.Items.TOOL_HARVESTERS, "Harvester", ChatFormatting.GREEN),
            tag(ModTags.Items.TOOL_PICKAXES, "Pickaxe", ChatFormatting.GREEN),
            tag(ModTags.Items.TOOL_AXES, "Axe", ChatFormatting.GREEN),
            tag(ModTags.Items.TOOL_SHOVELS, "Shovel", ChatFormatting.GREEN),
            tag(ModTags.Items.TOOL_HOES, "Hoe", ChatFormatting.GREEN),
            tag(ModTags.Items.TOOL_SHEARS, "Shears", ChatFormatting.GREEN),
            tag(ModTags.Items.TOOL_FISHING_RODS, "Fishing Rod", ChatFormatting.GREEN),
            tag(ModTags.Items.TOOL_BRUSHES, "Brush", ChatFormatting.GREEN),
            tag(ModTags.Items.TOOL_FLINT_AND_STEEL, "Flint and Steel", ChatFormatting.GREEN),
            tag(ModTags.Items.WEAPONS, "Weapon", ChatFormatting.RED),
            tag(ModTags.Items.WEAPON_MELEE, "Melee", ChatFormatting.RED),
            tag(ModTags.Items.WEAPON_RANGED, "Ranged", ChatFormatting.RED),
            tag(ModTags.Items.WEAPON_SWORDS, "Sword", ChatFormatting.RED),
            tag(ModTags.Items.WEAPON_AXES, "Axe Weapon", ChatFormatting.RED),
            tag(ModTags.Items.WEAPON_MACES, "Mace", ChatFormatting.RED),
            tag(ModTags.Items.WEAPON_BOWS, "Bow", ChatFormatting.RED),
            tag(ModTags.Items.WEAPON_CROSSBOWS, "Crossbow", ChatFormatting.RED),
            tag(ModTags.Items.WEAPON_TRIDENTS, "Trident", ChatFormatting.RED)
    );

    private static final List<DisplayedEnchantmentTag> ENCHANTMENT_TAGS = List.of(
            enchantmentTag(ModTags.Enchantments.FIRE, "Fire", ChatFormatting.GOLD),
            enchantmentTag(ModTags.Enchantments.FROST, "Frost", ChatFormatting.AQUA),
            enchantmentTag(ModTags.Enchantments.LIGHTNING, "Lightning", ChatFormatting.YELLOW),
            enchantmentTag(ModTags.Enchantments.PHYSICAL, "Physical", ChatFormatting.RED),
            enchantmentTag(ModTags.Enchantments.MINING, "Mining", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.DEFENSIVE, "Defensive", ChatFormatting.BLUE),
            enchantmentTag(ModTags.Enchantments.VITALITY, "Vitality", ChatFormatting.LIGHT_PURPLE),
            enchantmentTag(ModTags.Enchantments.MOBILITY, "Mobility", ChatFormatting.WHITE),
            enchantmentTag(ModTags.Enchantments.VOID, "Void", ChatFormatting.DARK_PURPLE),
            enchantmentTag(ModTags.Enchantments.TREASURE, "Treasure", ChatFormatting.GOLD)
    );

    private static final List<DisplayedEnchantmentTag> TARGET_ENCHANTMENT_TAGS = List.of(
            enchantmentTag(ModTags.Enchantments.TARGET_ARMOR, "Armor", ChatFormatting.BLUE),
            enchantmentTag(ModTags.Enchantments.TARGET_ARMOR_HELMETS, "Helmet", ChatFormatting.BLUE),
            enchantmentTag(ModTags.Enchantments.TARGET_ARMOR_BODY, "Body Armor", ChatFormatting.BLUE),
            enchantmentTag(ModTags.Enchantments.TARGET_ARMOR_LEGGINGS, "Leggings", ChatFormatting.BLUE),
            enchantmentTag(ModTags.Enchantments.TARGET_ARMOR_BOOTS, "Boots", ChatFormatting.BLUE),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOLS, "Tool", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOL_HARVESTERS, "Harvester", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOL_PICKAXES, "Pickaxe", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOL_AXES, "Axe Tool", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOL_SHOVELS, "Shovel", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOL_HOES, "Hoe", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOL_SHEARS, "Shears", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOL_FISHING_RODS, "Fishing Rod", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOL_BRUSHES, "Brush", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_TOOL_FLINT_AND_STEEL, "Flint and Steel", ChatFormatting.GREEN),
            enchantmentTag(ModTags.Enchantments.TARGET_WEAPONS, "Weapon", ChatFormatting.RED),
            enchantmentTag(ModTags.Enchantments.TARGET_WEAPON_MELEE, "Melee", ChatFormatting.RED),
            enchantmentTag(ModTags.Enchantments.TARGET_WEAPON_RANGED, "Ranged", ChatFormatting.RED),
            enchantmentTag(ModTags.Enchantments.TARGET_WEAPON_SWORDS, "Sword", ChatFormatting.RED),
            enchantmentTag(ModTags.Enchantments.TARGET_WEAPON_AXES, "Axe Weapon", ChatFormatting.RED),
            enchantmentTag(ModTags.Enchantments.TARGET_WEAPON_MACES, "Mace", ChatFormatting.RED),
            enchantmentTag(ModTags.Enchantments.TARGET_WEAPON_BOWS, "Bow", ChatFormatting.RED),
            enchantmentTag(ModTags.Enchantments.TARGET_WEAPON_CROSSBOWS, "Crossbow", ChatFormatting.RED),
            enchantmentTag(ModTags.Enchantments.TARGET_WEAPON_TRIDENTS, "Trident", ChatFormatting.RED)
    );

    private ClientTooltipEvents() {
    }

    @SubscribeEvent
    public static void addTagTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        List<TagLabel> essenceTags = essenceTags(stack);
        List<TagLabel> itemTags = matchingItemTags(stack, ITEM_TAGS);
        List<TagLabel> enchantmentTags = matchingEnchantmentTags(stack, ENCHANTMENT_TAGS);
        List<TagLabel> targetTags = matchingEnchantmentTags(stack, TARGET_ENCHANTMENT_TAGS);

        if (!essenceTags.isEmpty()) {
            tooltip.add(tagLine("Essence Tags", essenceTags));
        }
        if (!itemTags.isEmpty()) {
            tooltip.add(tagLine("Item Tags", itemTags));
        }
        if (!enchantmentTags.isEmpty()) {
            tooltip.add(tagLine("Enchantment Tags", enchantmentTags));
        }
        if (!targetTags.isEmpty()) {
            tooltip.add(tagLine("Target Tags", targetTags));
        }
    }

    private static List<TagLabel> essenceTags(ItemStack stack) {
        return EssenceDefinitions.get(stack)
                .map(definition -> definition.tags().stream()
                        .map(tag -> new TagLabel(titleCase(EssenceDefinitions.compactTagName(tag)), ChatFormatting.LIGHT_PURPLE))
                        .toList())
                .orElse(List.of());
    }

    private static List<TagLabel> matchingItemTags(ItemStack stack, List<DisplayedItemTag> candidates) {
        List<TagLabel> labels = new ArrayList<>();
        for (DisplayedItemTag candidate : candidates) {
            if (stack.is(candidate.tag())) {
                labels.add(candidate.label());
            }
        }
        return labels;
    }

    private static List<TagLabel> matchingEnchantmentTags(ItemStack stack, List<DisplayedEnchantmentTag> candidates) {
        Set<TagLabel> labels = new LinkedHashSet<>();
        addEnchantmentTags(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), candidates, labels);
        addEnchantmentTags(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY), candidates, labels);
        return List.copyOf(labels);
    }

    private static void addEnchantmentTags(ItemEnchantments enchantments, List<DisplayedEnchantmentTag> candidates, Set<TagLabel> labels) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            for (DisplayedEnchantmentTag candidate : candidates) {
                if (enchantment.is(candidate.tag())) {
                    labels.add(candidate.label());
                }
            }
        }
    }

    private static Component tagLine(String prefix, List<TagLabel> labels) {
        MutableComponent line = Component.literal(prefix + ": ").withStyle(ChatFormatting.DARK_GRAY);
        for (int index = 0; index < labels.size(); index++) {
            if (index > 0) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            TagLabel label = labels.get(index);
            line.append(Component.literal(label.text()).withStyle(style -> style
                    .withColor(label.color())
                    .withUnderlined(true)));
        }
        return line;
    }

    private static DisplayedItemTag tag(TagKey<Item> tag, String label, ChatFormatting color) {
        return new DisplayedItemTag(tag, new TagLabel(label, color));
    }

    private static DisplayedEnchantmentTag enchantmentTag(TagKey<Enchantment> tag, String label, ChatFormatting color) {
        return new DisplayedEnchantmentTag(tag, new TagLabel(label, color));
    }

    private static String titleCase(String value) {
        if (value.isEmpty()) {
            return value;
        }
        String[] words = value.replace('_', ' ').split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private record DisplayedItemTag(TagKey<Item> tag, TagLabel label) {
    }

    private record DisplayedEnchantmentTag(TagKey<Enchantment> tag, TagLabel label) {
    }

    private record TagLabel(String text, ChatFormatting color) {
    }
}
