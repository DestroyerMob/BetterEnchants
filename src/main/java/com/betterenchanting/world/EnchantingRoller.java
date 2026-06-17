package com.betterenchanting.world;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.data.EssenceDefinition;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.data.EnchantmentFusionRecipes;
import com.betterenchanting.data.EnchantmentLimitRules;
import com.betterenchanting.data.TagSimplifier;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class EnchantingRoller {
    private EnchantingRoller() {
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
        Set<Holder<Enchantment>> existingEnchantments = existingEnchantments(target);
        int maxEnchantments = EnchantmentLimitRules.maxEnchantments(target);
        int level = adjustedLevel(random, target, cost);
        List<WeightedCandidate> candidates = buildCandidates(registryAccess, target, level, profile);
        List<EnchantmentInstance> selected = new ArrayList<>();
        filterOverLimitCandidates(registryAccess, candidates, existingEnchantments, Set.of(), maxEnchantments);
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
        Set<Holder<Enchantment>> selectedEnchantments = new LinkedHashSet<>();
        selectedEnchantments.add(first.instance().enchantment);
        int poolSize = candidates.size();

        while (selected.size() < maxEnchantments + 1 && random.nextInt(BetterEnchantingConfig.rollerMultiEnchantRollBound()) <= level) {
            candidates.removeIf(candidate -> selectedEnchantments.contains(candidate.instance().enchantment)
                    || EnchantmentFusionRecipes.conflictsWithFusionIngredient(registryAccess, candidate.instance().enchantment, selectedEnchantments)
                    || conflictsWithExisting(registryAccess, candidate.instance().enchantment, selectedEnchantments));
            filterOverLimitCandidates(registryAccess, candidates, existingEnchantments, selectedEnchantments, maxEnchantments);
            if (candidates.isEmpty()) {
                break;
            }

            WeightedCandidate next = pick(random, candidates, representedEssenceTags, profile);
            if (next == null) {
                break;
            }
            selected.add(next.instance());
            selectedEnchantments.add(next.instance().enchantment);
            representedEssenceTags.addAll(next.matchingEssenceTags());
            level /= BetterEnchantingConfig.rollerMultiEnchantLevelDivisor();
        }

        return new RollPreview(List.copyOf(selected), poolSize, profile);
    }

    public static InputProfile profile(ItemStack target, List<ItemStack> essences, List<ItemStack> books) {
        Set<ResourceLocation> essenceTags = new LinkedHashSet<>();
        boolean restricted = false;
        boolean removesCurses = false;
        double essenceMultiplier = 1.0D;

        for (ItemStack essence : essences) {
            Optional<EssenceDefinition> definition = EssenceDefinitions.get(essence);
            if (definition.isEmpty()) {
                continue;
            }
            if (definition.get().restrictsPool()) {
                essenceTags.addAll(definition.get().tags());
            }
            restricted |= definition.get().restrictsPool();
            removesCurses |= definition.get().removesCurses();
            essenceMultiplier = Math.max(essenceMultiplier, definition.get().weightMultiplier());
        }

        Map<Holder<Enchantment>, BookBoost> bookBoosts = collectBookBoosts(books);
        return new InputProfile(
                List.copyOf(essenceTags),
                EnchantmentTargetTags.resolve(target),
                restricted && !essenceTags.isEmpty(),
                removesCurses,
                essenceMultiplier,
                Map.copyOf(bookBoosts)
        );
    }

    private static int adjustedLevel(RandomSource random, ItemStack target, int cost) {
        ItemStack enchantabilityTarget = target.is(Items.ENCHANTED_BOOK) ? new ItemStack(Items.BOOK) : target;
        int enchantability = enchantabilityTarget.getItem().getEnchantmentValue(enchantabilityTarget);
        if (enchantability <= 0) {
            return 0;
        }
        int enchantabilityDivisor = BetterEnchantingConfig.rollerEnchantabilityDivisor();
        int enchantabilityBonusBound = enchantability / enchantabilityDivisor + 1;
        int level = cost + 1 + random.nextInt(enchantabilityBonusBound) + random.nextInt(enchantabilityBonusBound);
        float variance = (random.nextFloat() + random.nextFloat() - 1.0F) * BetterEnchantingConfig.rollerLevelVariance();
        return Mth.clamp(Math.round((float) level + (float) level * variance), 1, Integer.MAX_VALUE);
    }

    private static List<WeightedCandidate> buildCandidates(RegistryAccess registryAccess, ItemStack target, int level, InputProfile profile) {
        Registry<Enchantment> enchantments = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        Optional<HolderSet.Named<Enchantment>> vanillaPool = enchantments.getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
        Set<Holder<Enchantment>> existingEnchantments = existingEnchantments(target);
        List<WeightedCandidate> candidates = new ArrayList<>();

        enchantments.holders().forEach(holder -> {
            if (profile.removesCurses() && holder.is(EnchantmentTags.CURSE)) {
                return;
            }
            if (existingEnchantments.contains(holder)
                    || EnchantmentFusionRecipes.conflictsWithFusionIngredient(registryAccess, holder, existingEnchantments)
                    || conflictsWithExisting(registryAccess, holder, existingEnchantments)) {
                return;
            }

            boolean inVanillaPool = vanillaPool.map(named -> named.contains(holder)).orElse(false);
            BookBoost bookBoost = profile.bookBoosts().get(holder);
            if (EnchantmentFusionRecipes.isFusionResult(registryAccess, holder) && bookBoost == null) {
                return;
            }

            Set<ResourceLocation> essenceMatches = matchingTags(holder, profile.essenceTags());
            Set<ResourceLocation> targetMatches = matchingTags(holder, profile.targetTags());
            boolean matchesEssence = !essenceMatches.isEmpty();

            if (!isBookTarget(target) && targetMatches.isEmpty()) {
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
                weight *= Math.pow(BetterEnchantingConfig.bookWeightMultiplier(), bookBoost.count());
            }

            candidates.add(new WeightedCandidate(
                    new EnchantmentInstance(holder, finalLevel),
                    List.copyOf(essenceMatches),
                    Math.max(1, Math.min(BetterEnchantingConfig.maxCandidateWeight(), (int) Math.round(weight)))
            ));
        });

        return candidates;
    }

    private static Set<Holder<Enchantment>> existingEnchantments(ItemStack stack) {
        Set<Holder<Enchantment>> enchantments = new LinkedHashSet<>();
        addExistingEnchantments(enchantments, stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        addExistingEnchantments(enchantments, stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        return enchantments;
    }

    private static void addExistingEnchantments(Set<Holder<Enchantment>> enchantments, ItemEnchantments source) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : source.entrySet()) {
            enchantments.add(entry.getKey());
        }
    }

    private static boolean conflictsWithExisting(
            RegistryAccess registryAccess,
            Holder<Enchantment> candidate,
            Set<Holder<Enchantment>> existingEnchantments
    ) {
        for (Holder<Enchantment> existing : existingEnchantments) {
            if (!Enchantment.areCompatible(candidate, existing)
                    && !EnchantmentFusionRecipes.areRecipeIngredients(registryAccess, candidate, existing)) {
                return true;
            }
        }
        return false;
    }

    private static void filterOverLimitCandidates(
            RegistryAccess registryAccess,
            List<WeightedCandidate> candidates,
            Set<Holder<Enchantment>> existingEnchantments,
            Set<Holder<Enchantment>> selectedEnchantments,
            int maxEnchantments
    ) {
        candidates.removeIf(candidate -> !fitsLimit(
                registryAccess,
                existingEnchantments,
                selectedEnchantments,
                candidate.instance().enchantment,
                maxEnchantments
        ));
    }

    private static boolean fitsLimit(
            RegistryAccess registryAccess,
            Set<Holder<Enchantment>> existingEnchantments,
            Set<Holder<Enchantment>> selectedEnchantments,
            Holder<Enchantment> candidate,
            int maxEnchantments
    ) {
        Set<Holder<Enchantment>> enchantments = new LinkedHashSet<>(existingEnchantments);
        enchantments.addAll(selectedEnchantments);
        enchantments.add(candidate);
        EnchantmentFusionRecipes.applyToEnchantmentSet(registryAccess, enchantments);
        return enchantments.size() <= maxEnchantments;
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

    private static int bestLevelForCost(Enchantment enchantment, int level) {
        for (int enchantmentLevel = enchantment.getMaxLevel(); enchantmentLevel >= enchantment.getMinLevel(); enchantmentLevel--) {
            if (level >= enchantment.getMinCost(enchantmentLevel) && level <= enchantment.getMaxCost(enchantmentLevel)) {
                return enchantmentLevel;
            }
        }
        return 0;
    }

    private static boolean isBookTarget(ItemStack target) {
        return target.is(Items.BOOK) || target.is(Items.ENCHANTED_BOOK);
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
                adjusted = Math.min(
                        BetterEnchantingConfig.maxCandidateWeight(),
                        (int) Math.round(adjusted * BetterEnchantingConfig.newTagComboMultiplier())
                );
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
        return TagSimplifier.simplify(tags).stream()
                .map(EssenceDefinitions::compactTagName)
                .map(EnchantingRoller::titleCase)
                .collect(Collectors.joining(", "));
    }

    private static String titleCase(String value) {
        if (value.isEmpty()) {
            return value;
        }
        String readable = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }

    private record WeightedCandidate(EnchantmentInstance instance, List<ResourceLocation> matchingEssenceTags, int baseWeight) {
    }

    private record BookBoost(int count, int level) {
    }

    public record InputProfile(
            List<ResourceLocation> essenceTags,
            List<ResourceLocation> targetTags,
            boolean restricted,
            boolean removesCurses,
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
