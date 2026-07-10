package com.betterenchanting.world;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.compat.ApothicEnchantingCompat;
import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.compat.ModularMaterialCompat;
import com.betterenchanting.data.EssenceDefinition;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.data.EnchantmentFusionRecipes;
import com.betterenchanting.data.EnchantmentLevelRules;
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
import java.util.OptionalInt;
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
    private static final int TAG_TARGET_FALLBACK_ENCHANTABILITY = 1;
    private static final ResourceLocation MINECRAFT_CURSE_TAG = ResourceLocation.withDefaultNamespace("curse");
    private static final ResourceLocation BETTER_ENCHANTING_CURSE_TAG = BetterEnchanting.id("curse");

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
        return preview(registryAccess, target, cost, option, seed, essences, books, Optional.empty());
    }

    public static RollPreview preview(
            RegistryAccess registryAccess,
            ItemStack target,
            int cost,
            int option,
            int seed,
            List<ItemStack> essences,
            List<ItemStack> books,
            Optional<ApothicEnchantingCompat.TableStats> apothicStats
    ) {
        return preview(registryAccess, target, cost, option, seed, essences, books, apothicStats, Set.of());
    }

    public static RollPreview preview(
            RegistryAccess registryAccess,
            ItemStack target,
            int cost,
            int option,
            int seed,
            List<ItemStack> essences,
            List<ItemStack> books,
            Optional<ApothicEnchantingCompat.TableStats> apothicStats,
            Set<Holder<Enchantment>> excludedEnchantments
    ) {
        RandomSource random = RandomSource.create();
        random.setSeed((long) seed + option);
        return select(random, registryAccess, target, cost, profile(target, essences, books), apothicStats, excludedEnchantments);
    }

    public static RollPreview preview(
            RegistryAccess registryAccess,
            ItemStack target,
            int cost,
            int option,
            int seed,
            ItemStack reagent,
            List<ItemStack> refiningEssences,
            List<ItemStack> books,
            Optional<ApothicEnchantingCompat.TableStats> apothicStats,
            Set<Holder<Enchantment>> excludedEnchantments
    ) {
        RandomSource random = RandomSource.create();
        random.setSeed((long) seed + option);
        return select(random, registryAccess, target, cost, profile(target, reagent, refiningEssences, books), apothicStats, excludedEnchantments);
    }

    public static RollPreview select(
            RandomSource random,
            RegistryAccess registryAccess,
            ItemStack target,
            int cost,
            List<ItemStack> essences,
            List<ItemStack> books
    ) {
        return select(random, registryAccess, target, cost, essences, books, Optional.empty());
    }

    public static RollPreview select(
            RandomSource random,
            RegistryAccess registryAccess,
            ItemStack target,
            int cost,
            List<ItemStack> essences,
            List<ItemStack> books,
            Optional<ApothicEnchantingCompat.TableStats> apothicStats
    ) {
        return select(random, registryAccess, target, cost, profile(target, essences, books), apothicStats, Set.of());
    }

    private static RollPreview select(
            RandomSource random,
            RegistryAccess registryAccess,
            ItemStack target,
            int cost,
            InputProfile profile,
            Optional<ApothicEnchantingCompat.TableStats> apothicStats,
            Set<Holder<Enchantment>> excludedEnchantments
    ) {
        Set<Holder<Enchantment>> existingEnchantments = existingEnchantments(target);
        int maxEnchantments = EnchantmentLimitRules.maxEnchantments(target);
        int selectionLimit = selectionLimit(maxEnchantments);
        int level = apothicStats.isPresent() ? cost : adjustedLevel(random, target, cost);
        if (profile.reagentTags().isEmpty()) {
            return new RollPreview(List.of(), 0, profile, EmptyReason.NO_REAGENT);
        }
        if (level <= 0) {
            return new RollPreview(List.of(), 0, profile, EmptyReason.NO_ROLL_POWER);
        }

        List<WeightedCandidate> candidates = buildCandidates(registryAccess, target, level, profile, apothicStats);
        int poolSize = candidates.size();
        List<EnchantmentInstance> selected = new ArrayList<>();
        candidates.removeIf(candidate -> excludedEnchantments.contains(candidate.instance().enchantment));
        filterOverLimitCandidates(registryAccess, candidates, target, existingEnchantments, Set.of());
        if (candidates.isEmpty()) {
            return new RollPreview(List.of(), poolSize, profile, excludedEnchantments.isEmpty() || poolSize <= 0
                    ? emptyReason(profile, poolSize)
                    : EmptyReason.DUPLICATE_OFFERS_EXHAUSTED);
        }

        Set<ResourceLocation> representedEssenceTags = new LinkedHashSet<>();
        WeightedCandidate first = pick(random, candidates, representedEssenceTags, profile);
        if (first == null) {
            return new RollPreview(List.of(), candidates.size(), profile, EmptyReason.WEIGHT_SELECTION_FAILED);
        }

        selected.add(first.instance());
        representedEssenceTags.addAll(first.matchingEssenceTags());
        int guaranteedSelectionCount = apothicStats
                .map(ApothicEnchantingCompat::guaranteedSelectionCount)
                .orElse(1);
        Set<Holder<Enchantment>> selectedEnchantments = new LinkedHashSet<>();
        selectedEnchantments.add(first.instance().enchantment);
        while (selected.size() < selectionLimit
                && (selected.size() < guaranteedSelectionCount
                || random.nextInt(EffectiveBalance.rollerMultiEnchantRollBound()) <= level)) {
            candidates.removeIf(candidate -> selectedEnchantments.contains(candidate.instance().enchantment)
                    || EnchantmentFusionRecipes.conflictsWithFusionIngredient(registryAccess, candidate.instance().enchantment, selectedEnchantments)
                    || conflictsWithExisting(registryAccess, candidate.instance().enchantment, selectedEnchantments));
            filterOverLimitCandidates(registryAccess, candidates, target, existingEnchantments, selectedEnchantments);
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
            level /= EffectiveBalance.rollerMultiEnchantLevelDivisor();
        }

        return new RollPreview(List.copyOf(selected), poolSize, profile, EmptyReason.NONE);
    }

    public static InputProfile profile(ItemStack target, List<ItemStack> essences, List<ItemStack> books) {
        ItemStack reagent = essences.isEmpty() ? ItemStack.EMPTY : essences.get(0);
        List<ItemStack> refiningEssences = essences.isEmpty() ? List.of() : essences.subList(1, essences.size());
        return profile(target, reagent, refiningEssences, books);
    }

    public static InputProfile profile(ItemStack target, ItemStack reagent, List<ItemStack> refiningEssences, List<ItemStack> books) {
        Set<ResourceLocation> reagentTags = new LinkedHashSet<>();
        Set<ResourceLocation> refinementTags = new LinkedHashSet<>();
        Set<ResourceLocation> activeTags = new LinkedHashSet<>();
        Set<ResourceLocation> removedTags = new LinkedHashSet<>();
        double essenceMultiplier = 1.0D;

        Optional<EssenceDefinition> reagentDefinition = EssenceDefinitions.get(reagent);
        if (reagentDefinition.filter(EssenceDefinition::restrictsPool).isPresent()) {
            reagentTags.addAll(reagentDefinition.get().tags());
            activeTags.addAll(reagentDefinition.get().tags());
            removedTags.addAll(reagentDefinition.get().removedTags());
            essenceMultiplier = Math.max(essenceMultiplier, reagentDefinition.get().weightMultiplier());
        }

        for (ItemStack essence : refiningEssences) {
            Optional<EssenceDefinition> definition = EssenceDefinitions.get(essence);
            if (definition.isEmpty()) {
                continue;
            }
            if (definition.get().restrictsPool()) {
                refinementTags.addAll(definition.get().tags());
                activeTags.addAll(definition.get().tags());
            }
            removedTags.addAll(definition.get().removedTags());
            essenceMultiplier = Math.max(essenceMultiplier, definition.get().weightMultiplier());
        }

        Map<Holder<Enchantment>, BookBoost> bookBoosts = collectBookBoosts(books);
        return new InputProfile(
                List.copyOf(reagentTags),
                List.copyOf(refinementTags),
                List.copyOf(activeTags),
                List.copyOf(removedTags),
                EnchantmentTargetTags.resolve(target),
                !reagentTags.isEmpty(),
                essenceMultiplier,
                Map.copyOf(bookBoosts)
        );
    }

    private static int adjustedLevel(RandomSource random, ItemStack target, int cost) {
        ItemStack enchantabilityTarget = target.is(Items.ENCHANTED_BOOK) ? new ItemStack(Items.BOOK) : target;
        int enchantability = enchantabilityTarget.getItem().getEnchantmentValue(enchantabilityTarget);
        if (enchantability <= 0 && !EnchantmentTargetTags.resolve(target).isEmpty()) {
            enchantability = TAG_TARGET_FALLBACK_ENCHANTABILITY;
        }
        if (enchantability <= 0) {
            return 0;
        }
        int enchantabilityDivisor = EffectiveBalance.rollerEnchantabilityDivisor();
        int enchantabilityBonusBound = enchantability / enchantabilityDivisor + 1;
        int level = cost + 1 + random.nextInt(enchantabilityBonusBound) + random.nextInt(enchantabilityBonusBound);
        float variance = (random.nextFloat() + random.nextFloat() - 1.0F) * EffectiveBalance.rollerLevelVariance();
        return Mth.clamp(Math.round((float) level + (float) level * variance), 1, Integer.MAX_VALUE);
    }

    private static EmptyReason emptyReason(InputProfile profile, int poolSizeBeforeCapacity) {
        if (poolSizeBeforeCapacity > 0) {
            return EmptyReason.ENCHANTMENT_LIMIT;
        }
        if (profile.restricted()) {
            return EmptyReason.RESTRICTED_POOL_EMPTY;
        }
        if (!profile.removedTags().isEmpty()) {
            return EmptyReason.REMOVED_TAGS_EMPTY;
        }
        return EmptyReason.NO_COMPATIBLE_ENCHANTMENTS;
    }

    private static List<WeightedCandidate> buildCandidates(RegistryAccess registryAccess, ItemStack target, int level, InputProfile profile, Optional<ApothicEnchantingCompat.TableStats> apothicStats) {
        Registry<Enchantment> enchantments = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        Optional<HolderSet.Named<Enchantment>> vanillaPool = enchantments.getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
        Set<Holder<Enchantment>> existingEnchantments = existingEnchantments(target);
        List<WeightedCandidate> candidates = new ArrayList<>();

        enchantments.holders().forEach(holder -> {
            if (matchesAnyTag(holder, profile.removedTags())
                    || apothicStats.map(stats -> stats.isBlacklisted(holder)).orElse(false)) {
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

            Set<ResourceLocation> reagentMatches = matchingTags(holder, profile.reagentTags());
            Set<ResourceLocation> refinementMatches = matchingTags(holder, profile.refinementTags());
            Set<ResourceLocation> essenceMatches = matchingTags(holder, profile.essenceTags());
            Set<ResourceLocation> targetMatches = matchingTags(holder, profile.targetTags());
            boolean matchesNonCurseEssence = essenceMatches.stream().anyMatch(EnchantingRoller::isNonCurseEssenceTag);

            if (!isBookTarget(target) && targetMatches.isEmpty()) {
                return;
            }
            if (holder.is(EnchantmentTags.CURSE) && !matchesNonCurseEssence && bookBoost == null) {
                return;
            }
            if (reagentMatches.isEmpty()) {
                return;
            }
            if (!profile.refinementTags().isEmpty() && refinementMatches.isEmpty()) {
                return;
            }
            if (!profile.restricted() && !inVanillaPool && bookBoost == null) {
                return;
            }

            int rolledLevel = bestLevelForCost(holder, level, apothicStats);
            if (rolledLevel <= 0 && bookBoost == null) {
                return;
            }

            int finalLevel = EnchantmentLevelRules.clampLevel(holder, bookBoost == null ? rolledLevel : Math.max(rolledLevel, bookBoost.level()));
            if (finalLevel <= 0) {
                return;
            }

            double weight = Math.max(1, apothicStats
                    .map(stats -> ApothicEnchantingCompat.adjustedWeight(holder, stats))
                    .orElse(holder.value().getWeight()));
            if (!essenceMatches.isEmpty()) {
                weight *= profile.essenceWeightMultiplier();
            }
            if (bookBoost != null) {
                weight *= Math.pow(EffectiveBalance.bookWeightMultiplier(), bookBoost.count());
            }

            candidates.add(new WeightedCandidate(
                    new EnchantmentInstance(holder, finalLevel),
                    List.copyOf(essenceMatches),
                    WeightedSelection.cappedWeight(weight, EffectiveBalance.maxCandidateWeight())
            ));
        });

        return candidates;
    }

    private static Set<Holder<Enchantment>> existingEnchantments(ItemStack stack) {
        if (ModularMaterialCompat.hasRoutedParts(stack)) {
            return ModularMaterialCompat.storedRoutedEnchantments(stack);
        }

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

    private static int selectionLimit(int maxEnchantments) {
        if (!EnchantmentLimitRules.overridesVanillaLimits() || maxEnchantments == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, maxEnchantments + 1);
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
            ItemStack target,
            Set<Holder<Enchantment>> existingEnchantments,
            Set<Holder<Enchantment>> selectedEnchantments
    ) {
        candidates.removeIf(candidate -> !fitsLimit(
                registryAccess,
                target,
                existingEnchantments,
                selectedEnchantments,
                candidate.instance().enchantment
        ));
    }

    private static boolean fitsLimit(
            RegistryAccess registryAccess,
            ItemStack target,
            Set<Holder<Enchantment>> existingEnchantments,
            Set<Holder<Enchantment>> selectedEnchantments,
            Holder<Enchantment> candidate
    ) {
        if (ModularMaterialCompat.hasRoutedParts(target)) {
            List<EnchantmentInstance> additions = new ArrayList<>();
            for (Holder<Enchantment> selected : selectedEnchantments) {
                additions.add(new EnchantmentInstance(selected, 1));
            }
            additions.add(new EnchantmentInstance(candidate, 1));
            return ModularMaterialCompat.applyRoutedEnchantments(registryAccess, target, additions)
                    .map(EnchantmentLimitRules::isWithinLimits)
                    .orElse(false);
        }

        Set<Holder<Enchantment>> enchantments = new LinkedHashSet<>(existingEnchantments);
        enchantments.addAll(selectedEnchantments);
        enchantments.add(candidate);
        EnchantmentFusionRecipes.applyToEnchantmentSet(registryAccess, enchantments);
        return EnchantmentLimitRules.canFitAll(target, enchantments);
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

    private static int bestLevelForCost(Holder<Enchantment> enchantment, int level, Optional<ApothicEnchantingCompat.TableStats> apothicStats) {
        if (apothicStats.isPresent()) {
            OptionalInt apothicLevel = ApothicEnchantingCompat.bestLevelForPower(enchantment, level);
            if (apothicLevel.isPresent()) {
                return apothicLevel.getAsInt();
            }
        }
        for (int enchantmentLevel = EnchantmentLevelRules.maxLevel(enchantment); enchantmentLevel >= enchantment.value().getMinLevel(); enchantmentLevel--) {
            if (level >= enchantment.value().getMinCost(enchantmentLevel) && level <= enchantment.value().getMaxCost(enchantmentLevel)) {
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

    private static boolean matchesAnyTag(Holder<Enchantment> enchantment, List<ResourceLocation> tagIds) {
        for (ResourceLocation tagId : tagIds) {
            if (enchantment.is(TagKey.create(Registries.ENCHANTMENT, tagId))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNonCurseEssenceTag(ResourceLocation tagId) {
        return !MINECRAFT_CURSE_TAG.equals(tagId) && !BETTER_ENCHANTING_CURSE_TAG.equals(tagId);
    }

    private static WeightedCandidate pick(
            RandomSource random,
            List<WeightedCandidate> candidates,
            Set<ResourceLocation> representedEssenceTags,
            InputProfile profile
    ) {
        List<Integer> baseWeights = new ArrayList<>(candidates.size());
        List<Boolean> receivesComboBonus = new ArrayList<>(candidates.size());
        for (WeightedCandidate candidate : candidates) {
            baseWeights.add(candidate.baseWeight());
            receivesComboBonus.add(profile.essenceTags().size() > 1 && contributesNewTag(candidate, representedEssenceTags));
        }

        int pickedIndex = WeightedSelection.pickIndex(
                baseWeights,
                receivesComboBonus,
                EffectiveBalance.maxCandidateWeight(),
                EffectiveBalance.newTagComboMultiplier(),
                bound -> nextLong(random, bound)
        );
        if (pickedIndex < 0) {
            return null;
        }
        return candidates.get(pickedIndex);
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

    private static long nextLong(RandomSource random, long bound) {
        long mask = bound - 1L;
        if ((bound & mask) == 0L) {
            return random.nextLong() & mask;
        }

        long bits;
        long value;
        do {
            bits = random.nextLong() >>> 1;
            value = bits % bound;
        } while (bits - value + mask < 0L);
        return value;
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
            List<ResourceLocation> reagentTags,
            List<ResourceLocation> refinementTags,
            List<ResourceLocation> essenceTags,
            List<ResourceLocation> removedTags,
            List<ResourceLocation> targetTags,
            boolean restricted,
            double essenceWeightMultiplier,
            Map<Holder<Enchantment>, BookBoost> bookBoosts
    ) {
        public int bookBoostCount() {
            return bookBoosts.values().stream().mapToInt(BookBoost::count).sum();
        }
    }

    public enum EmptyReason {
        NONE,
        NO_REAGENT,
        NO_ROLL_POWER,
        NO_COMPATIBLE_ENCHANTMENTS,
        RESTRICTED_POOL_EMPTY,
        REMOVED_TAGS_EMPTY,
        ENCHANTMENT_LIMIT,
        WEIGHT_SELECTION_FAILED,
        DUPLICATE_OFFERS_EXHAUSTED
    }

    public record RollPreview(List<EnchantmentInstance> enchantments, int poolSize, InputProfile profile, EmptyReason emptyReason) {
        public RollPreview(List<EnchantmentInstance> enchantments, int poolSize, InputProfile profile) {
            this(enchantments, poolSize, profile, EmptyReason.NONE);
        }
    }
}
