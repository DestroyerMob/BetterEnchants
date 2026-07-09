package com.betterenchanting.integration.jei;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.compat.ApothicEnchantingCompat;
import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.data.EnchantmentGuideEntries;
import com.betterenchanting.data.EnchantmentLevelRules;
import com.betterenchanting.data.TagDisplayRules;
import com.betterenchanting.world.inventory.EnchantingPowerRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record EnchantmentInfoRecipe(
        ResourceLocation id,
        Holder<Enchantment> enchantment,
        EnchantmentGuideEntries.Entry guide,
        ItemStack book,
        List<PowerBand> vanillaBands,
        List<PowerBand> apothicBands,
        List<TagDisplayRules.TagLabel> targets,
        List<TagDisplayRules.TagLabel> affinities,
        boolean inEnchantingTable,
        boolean curse,
        boolean treasure
) {
    private static final ResourceLocation TREASURE_TAG = ResourceLocation.withDefaultNamespace("treasure");

    public EnchantmentInfoRecipe {
        book = book.copy();
        vanillaBands = List.copyOf(vanillaBands);
        apothicBands = List.copyOf(apothicBands);
        targets = List.copyOf(targets);
        affinities = List.copyOf(affinities);
    }

    public static Optional<EnchantmentInfoRecipe> create(Holder<Enchantment> enchantment) {
        Optional<ResourceLocation> id = enchantment.unwrapKey().map(ResourceKey::location);
        if (id.isEmpty()) {
            return Optional.empty();
        }

        EnchantmentGuideEntries.Entry guide = EnchantmentGuideEntries.get(id.get());
        if (guide.hidden()) {
            return Optional.empty();
        }

        int minLevel = Math.max(1, enchantment.value().getMinLevel());
        int vanillaMaxLevel = Math.max(minLevel, enchantment.value().getMaxLevel());
        int effectiveMaxLevel = Math.max(vanillaMaxLevel, EnchantmentLevelRules.maxLevel(enchantment));
        List<PowerBand> vanillaBands = new ArrayList<>();
        List<PowerBand> apothicBands = new ArrayList<>();
        for (int level = minLevel; level <= effectiveMaxLevel; level++) {
            if (level <= vanillaMaxLevel) {
                vanillaBands.add(new PowerBand(
                        level,
                        enchantment.value().getMinCost(level),
                        enchantment.value().getMaxCost(level)
                ));
            }
            int bandLevel = level;
            ApothicEnchantingCompat.powerRange(enchantment, level)
                    .ifPresent(range -> apothicBands.add(new PowerBand(bandLevel, range.minPower(), range.maxPower())));
        }

        return Optional.of(new EnchantmentInfoRecipe(
                id.get(),
                enchantment,
                guide,
                enchantedBook(enchantment, minLevel),
                vanillaBands,
                apothicBands,
                targetLabels(enchantment),
                affinityLabels(enchantment),
                enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE),
                enchantment.is(EnchantmentTags.CURSE),
                enchantment.is(TagKey.create(Registries.ENCHANTMENT, TREASURE_TAG))
        ));
    }

    public String descriptionKey() {
        return "enchantment." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public Component title() {
        return Component.translatable(descriptionKey());
    }

    public Component summary() {
        if (guide.summaryKey().isPresent()) {
            return Component.translatable(guide.summaryKey().get());
        }
        if (guide.summary().isPresent()) {
            return Component.literal(guide.summary().get());
        }

        String fallbackKey = descriptionKey() + ".desc";
        return I18n.exists(fallbackKey) ? Component.translatable(fallbackKey) : Component.empty();
    }

    public List<Component> notes() {
        return guide.notes().stream()
                .map(note -> (Component) Component.literal(note))
                .toList();
    }

    public String sortName() {
        String key = descriptionKey();
        return I18n.exists(key) ? I18n.get(key) : id.toString();
    }

    public int maxLevel() {
        return Math.max(enchantment.value().getMaxLevel(), EnchantmentLevelRules.maxLevel(enchantment));
    }

    public int weight() {
        return Math.max(1, enchantment.value().getWeight());
    }

    public OptionalInt minimumPower() {
        return StreamMinimum.of(vanillaBands, apothicBands);
    }

    public OptionalInt minimumBookshelfPower() {
        OptionalInt minimum = minimumPower();
        if (minimum.isEmpty()) {
            return OptionalInt.empty();
        }
        for (int power = 0; power <= EffectiveBalance.maxBookshelfPower(); power++) {
            if (EnchantingPowerRules.offerRequirementForBookshelfPower(power) >= minimum.getAsInt()) {
                return OptionalInt.of(power);
            }
        }
        return OptionalInt.empty();
    }

    private static ItemStack enchantedBook(Holder<Enchantment> enchantment, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(enchantment, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        return stack;
    }

    private static List<TagDisplayRules.TagLabel> targetLabels(Holder<Enchantment> enchantment) {
        return labels(enchantment, true);
    }

    private static List<TagDisplayRules.TagLabel> affinityLabels(Holder<Enchantment> enchantment) {
        return labels(enchantment, false);
    }

    private static List<TagDisplayRules.TagLabel> labels(Holder<Enchantment> enchantment, boolean targets) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        enchantment.tags()
                .map(TagKey::location)
                .filter(id -> id.getNamespace().equals(BetterEnchanting.MOD_ID))
                .filter(id -> id.getPath().startsWith("targets/") == targets)
                .filter(id -> !id.getPath().startsWith("exclusive_set/"))
                .forEach(ids::add);
        return ids.stream()
                .map(TagDisplayRules::labelFor)
                .sorted(Comparator.comparing(TagDisplayRules.TagLabel::text))
                .toList();
    }

    public record PowerBand(int level, int minPower, int maxPower) {
    }

    private static final class StreamMinimum {
        private StreamMinimum() {
        }

        private static OptionalInt of(List<PowerBand> first, List<PowerBand> second) {
            OptionalInt firstMinimum = minimum(first);
            OptionalInt secondMinimum = minimum(second);
            if (firstMinimum.isEmpty()) {
                return secondMinimum;
            }
            if (secondMinimum.isEmpty()) {
                return firstMinimum;
            }
            return OptionalInt.of(Math.min(firstMinimum.getAsInt(), secondMinimum.getAsInt()));
        }

        private static OptionalInt minimum(List<PowerBand> bands) {
            return bands.stream().mapToInt(PowerBand::minPower).min();
        }
    }
}
