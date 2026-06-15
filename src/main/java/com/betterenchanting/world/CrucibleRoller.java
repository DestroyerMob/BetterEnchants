package com.betterenchanting.world;

import com.betterenchanting.data.EssenceDefinition;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.registry.ModTags;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class CrucibleRoller {
    private static final double BOOK_MULTIPLIER = 8.0D;
    private static final double NEW_TAG_COMBO_MULTIPLIER = 3.0D;
    private static final int MAX_WEIGHT = 1_000_000;
    private static final List<TargetTagRule> TARGET_TAG_RULES = List.of(
            target(ModTags.Items.ARMOR, ModTags.Enchantments.TARGET_ARMOR),
            target(ModTags.Items.ARMOR_HELMETS, ModTags.Enchantments.TARGET_ARMOR_HELMETS),
            target(ModTags.Items.ARMOR_BODY, ModTags.Enchantments.TARGET_ARMOR_BODY),
            target(ModTags.Items.ARMOR_LEGGINGS, ModTags.Enchantments.TARGET_ARMOR_LEGGINGS),
            target(ModTags.Items.ARMOR_BOOTS, ModTags.Enchantments.TARGET_ARMOR_BOOTS),
            target(ModTags.Items.TOOLS, ModTags.Enchantments.TARGET_TOOLS),
            target(ModTags.Items.HARVESTERS, ModTags.Enchantments.TARGET_TOOL_HARVESTERS),
            target(ModTags.Items.TOOL_HARVESTERS, ModTags.Enchantments.TARGET_TOOL_HARVESTERS),
            target(ModTags.Items.TOOL_PICKAXES, ModTags.Enchantments.TARGET_TOOL_PICKAXES),
            target(ModTags.Items.TOOL_AXES, ModTags.Enchantments.TARGET_TOOL_AXES),
            target(ModTags.Items.TOOL_SHOVELS, ModTags.Enchantments.TARGET_TOOL_SHOVELS),
            target(ModTags.Items.TOOL_HOES, ModTags.Enchantments.TARGET_TOOL_HOES),
            target(ModTags.Items.TOOL_SHEARS, ModTags.Enchantments.TARGET_TOOL_SHEARS),
            target(ModTags.Items.TOOL_FISHING_RODS, ModTags.Enchantments.TARGET_TOOL_FISHING_RODS),
            target(ModTags.Items.TOOL_BRUSHES, ModTags.Enchantments.TARGET_TOOL_BRUSHES),
            target(ModTags.Items.TOOL_FLINT_AND_STEEL, ModTags.Enchantments.TARGET_TOOL_FLINT_AND_STEEL),
            target(ModTags.Items.WEAPONS, ModTags.Enchantments.TARGET_WEAPONS),
            target(ModTags.Items.WEAPON_MELEE, ModTags.Enchantments.TARGET_WEAPON_MELEE),
            target(ModTags.Items.WEAPON_RANGED, ModTags.Enchantments.TARGET_WEAPON_RANGED),
            target(ModTags.Items.WEAPON_SWORDS, ModTags.Enchantments.TARGET_WEAPON_SWORDS),
            target(ModTags.Items.WEAPON_AXES, ModTags.Enchantments.TARGET_WEAPON_AXES),
            target(ModTags.Items.WEAPON_MACES, ModTags.Enchantments.TARGET_WEAPON_MACES),
            target(ModTags.Items.WEAPON_BOWS, ModTags.Enchantments.TARGET_WEAPON_BOWS),
            target(ModTags.Items.WEAPON_CROSSBOWS, ModTags.Enchantments.TARGET_WEAPON_CROSSBOWS),
            target(ModTags.Items.WEAPON_TRIDENTS, ModTags.Enchantments.TARGET_WEAPON_TRIDENTS)
    );

    private CrucibleRoller() {
    }

    public static RollPreview preview(
            RegistryAccess registryAccess,
            ItemStack target,
            int cost,
            int option,
            int seed,
            List<ItemStack> essences,
            List<ItemStack> books
    ) {
        RandomSource random = RandomSource.create();
        random.setSeed((long) seed + option);
        return select(random, registryAccess, target, cost, essences, books);
    }

    public static RollPreview select(
            RandomSource random,
            RegistryAccess registryAccess,
            ItemStack target,
            int cost,
            List<ItemStack> essences,
            List<ItemStack> books
    ) {
        InputProfile profile = profile(target, essences, books);
        int level = adjustedLevel(random, target, cost);
        List<WeightedCandidate> candidates = buildCandidates(registryAccess, target, level, profile);
        List<EnchantmentInstance> selected = new ArrayList<>();
        if (candidates.isEmpty()) {
            return new RollPreview(List.of(), 0, profile);
        }

        Set<ResourceLocation> representedEssenceTags = new LinkedHashSet<>();
        WeightedCandidate first = pick(random, candidates, representedEssenceTags, profile);
        if (first == null) {
            return new RollPreview(List.of(), candidates.size(), profile);
        }

        selected.add(first.instance());
        representedEssenceTags.addAll(first.matchingEssenceTags());
        int poolSize = candidates.size();

        while (random.nextInt(50) <= level) {
            WeightedCandidate last = candidates.stream()
                    .filter(candidate -> candidate.instance() == selected.get(selected.size() - 1))
                    .findFirst()
                    .orElse(null);
            if (last != null) {
                candidates.removeIf(candidate -> !Enchantment.areCompatible(last.instance().enchantment, candidate.instance().enchantment));
            }
            if (candidates.isEmpty()) {
                break;
            }

            WeightedCandidate next = pick(random, candidates, representedEssenceTags, profile);
            if (next == null) {
                break;
            }
            selected.add(next.instance());
            representedEssenceTags.addAll(next.matchingEssenceTags());
            level /= 2;
        }

        return new RollPreview(List.copyOf(selected), poolSize, profile);
    }

    public static InputProfile profile(ItemStack target, List<ItemStack> essences, List<ItemStack> books) {
        Set<ResourceLocation> essenceTags = new LinkedHashSet<>();
        boolean restricted = false;
        double essenceMultiplier = 1.0D;

        for (ItemStack essence : essences) {
            Optional<EssenceDefinition> definition = EssenceDefinitions.get(essence);
            if (definition.isEmpty()) {
                continue;
            }
            essenceTags.addAll(definition.get().tags());
            restricted |= definition.get().restrictsPool();
            essenceMultiplier = Math.max(essenceMultiplier, definition.get().weightMultiplier());
        }

        Map<Holder<Enchantment>, BookBoost> bookBoosts = collectBookBoosts(books);
        return new InputProfile(
                List.copyOf(essenceTags),
                resolveTargetTags(target),
                restricted && !essenceTags.isEmpty(),
                essenceMultiplier,
                Map.copyOf(bookBoosts)
        );
    }

    private static int adjustedLevel(RandomSource random, ItemStack target, int cost) {
        int enchantability = target.getEnchantmentValue();
        if (enchantability <= 0) {
            return 0;
        }
        int level = cost + 1 + random.nextInt(enchantability / 4 + 1) + random.nextInt(enchantability / 4 + 1);
        float variance = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
        return Mth.clamp(Math.round((float) level + (float) level * variance), 1, Integer.MAX_VALUE);
    }

    private static List<WeightedCandidate> buildCandidates(RegistryAccess registryAccess, ItemStack target, int level, InputProfile profile) {
        Registry<Enchantment> enchantments = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        Optional<HolderSet.Named<Enchantment>> vanillaPool = enchantments.getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
        List<WeightedCandidate> candidates = new ArrayList<>();

        enchantments.holders().forEach(holder -> {
            boolean inVanillaPool = vanillaPool.map(named -> named.contains(holder)).orElse(false);
            BookBoost bookBoost = profile.bookBoosts().get(holder);
            Set<ResourceLocation> essenceMatches = matchingTags(holder, profile.essenceTags());
            Set<ResourceLocation> targetMatches = matchingTags(holder, profile.targetTags());
            boolean matchesEssence = !essenceMatches.isEmpty();

            if (!target.is(Items.BOOK) && targetMatches.isEmpty()) {
                return;
            }
            if (profile.restricted() && !matchesEssence && bookBoost == null) {
                return;
            }
            if (!profile.restricted() && !inVanillaPool && bookBoost == null) {
                return;
            }

            int rolledLevel = bestLevelForCost(holder.value(), level);
            if (rolledLevel <= 0 && bookBoost == null) {
                return;
            }

            int finalLevel = bookBoost == null ? rolledLevel : Math.max(rolledLevel, bookBoost.level());
            if (finalLevel <= 0) {
                return;
            }

            double weight = Math.max(1, holder.value().getWeight());
            if (matchesEssence) {
                weight *= profile.essenceWeightMultiplier();
            }
            if (bookBoost != null) {
                weight *= Math.pow(BOOK_MULTIPLIER, bookBoost.count());
            }

            candidates.add(new WeightedCandidate(
                    new EnchantmentInstance(holder, finalLevel),
                    List.copyOf(essenceMatches),
                    Math.max(1, Math.min(MAX_WEIGHT, (int) Math.round(weight)))
            ));
        });

        return candidates;
    }

    private static Map<Holder<Enchantment>, BookBoost> collectBookBoosts(List<ItemStack> books) {
        Map<Holder<Enchantment>, BookBoost> boosts = new HashMap<>();
        for (ItemStack book : books) {
            ItemEnchantments storedEnchantments = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : storedEnchantments.entrySet()) {
                boosts.compute(entry.getKey(), (holder, current) -> {
                    if (current == null) {
                        return new BookBoost(1, entry.getIntValue());
                    }
                    return new BookBoost(current.count() + 1, Math.max(current.level(), entry.getIntValue()));
                });
            }
        }
        return boosts;
    }

    private static List<ResourceLocation> resolveTargetTags(ItemStack target) {
        if (target.isEmpty() || target.is(Items.BOOK)) {
            return List.of();
        }

        Set<ResourceLocation> tags = new LinkedHashSet<>();
        for (TargetTagRule rule : TARGET_TAG_RULES) {
            if (target.is(rule.itemTag())) {
                tags.add(rule.enchantmentTag().location());
            }
        }
        return List.copyOf(tags);
    }

    private static int bestLevelForCost(Enchantment enchantment, int level) {
        for (int enchantmentLevel = enchantment.getMaxLevel(); enchantmentLevel >= enchantment.getMinLevel(); enchantmentLevel--) {
            if (level >= enchantment.getMinCost(enchantmentLevel) && level <= enchantment.getMaxCost(enchantmentLevel)) {
                return enchantmentLevel;
            }
        }
        return 0;
    }

    private static Set<ResourceLocation> matchingTags(Holder<Enchantment> enchantment, List<ResourceLocation> tagIds) {
        Set<ResourceLocation> matches = new LinkedHashSet<>();
        for (ResourceLocation tagId : tagIds) {
            if (enchantment.is(TagKey.create(Registries.ENCHANTMENT, tagId))) {
                matches.add(tagId);
            }
        }
        return matches;
    }

    private static WeightedCandidate pick(
            RandomSource random,
            List<WeightedCandidate> candidates,
            Set<ResourceLocation> representedEssenceTags,
            InputProfile profile
    ) {
        int totalWeight = 0;
        List<Integer> adjustedWeights = new ArrayList<>(candidates.size());
        for (WeightedCandidate candidate : candidates) {
            int adjusted = candidate.baseWeight();
            if (profile.essenceTags().size() > 1 && contributesNewTag(candidate, representedEssenceTags)) {
                adjusted = Math.min(MAX_WEIGHT, (int) Math.round(adjusted * NEW_TAG_COMBO_MULTIPLIER));
            }
            adjustedWeights.add(adjusted);
            totalWeight += adjusted;
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        for (int index = 0; index < candidates.size(); index++) {
            roll -= adjustedWeights.get(index);
            if (roll < 0) {
                return candidates.get(index);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private static boolean contributesNewTag(WeightedCandidate candidate, Set<ResourceLocation> representedEssenceTags) {
        if (representedEssenceTags.isEmpty()) {
            return false;
        }
        for (ResourceLocation tag : candidate.matchingEssenceTags()) {
            if (!representedEssenceTags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public static String tagSummary(List<ResourceLocation> tags) {
        if (tags.isEmpty()) {
            return "None";
        }
        return tags.stream()
                .map(EssenceDefinitions::compactTagName)
                .map(CrucibleRoller::titleCase)
                .collect(Collectors.joining(", "));
    }

    private static String titleCase(String value) {
        if (value.isEmpty()) {
            return value;
        }
        String readable = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }

    private static TargetTagRule target(TagKey<Item> itemTag, TagKey<Enchantment> enchantmentTag) {
        return new TargetTagRule(itemTag, enchantmentTag);
    }

    private record WeightedCandidate(EnchantmentInstance instance, List<ResourceLocation> matchingEssenceTags, int baseWeight) {
    }

    private record BookBoost(int count, int level) {
    }

    private record TargetTagRule(TagKey<Item> itemTag, TagKey<Enchantment> enchantmentTag) {
    }

    public record InputProfile(
            List<ResourceLocation> essenceTags,
            List<ResourceLocation> targetTags,
            boolean restricted,
            double essenceWeightMultiplier,
            Map<Holder<Enchantment>, BookBoost> bookBoosts
    ) {
        public int bookBoostCount() {
            return bookBoosts.values().stream().mapToInt(BookBoost::count).sum();
        }
    }

    public record RollPreview(List<EnchantmentInstance> enchantments, int poolSize, InputProfile profile) {
    }
}
