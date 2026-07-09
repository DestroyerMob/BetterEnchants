package com.betterenchanting.world.inventory;

import com.mojang.datafixers.util.Pair;
import com.betterenchanting.compat.ApothicEnchantingCompat;
import com.betterenchanting.compat.ModularMaterialCompat;
import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.data.ApothicInfusionModifierRules;
import com.betterenchanting.data.EnchantmentLevelRules;
import com.betterenchanting.data.EnchantmentLevelRules.OverlevelTarget;
import com.betterenchanting.data.EnchantmentLimitRules;
import com.betterenchanting.registry.ModBlocks;
import com.betterenchanting.registry.ModMenus;
import com.betterenchanting.world.EnchantingRoller;
import com.betterenchanting.world.EnchantmentTargetTags;
import com.betterenchanting.world.enchantment.FortunesTouchEnchantmentEvents;
import com.betterenchanting.world.level.block.EnchantingTablePower;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class EnhancedEnchantingMenu extends AbstractContainerMenu {
    public static final int TARGET_SLOT = 0;
    public static final int LAPIS_SLOT = 1;
    public static final int FIRST_MODIFIER_SLOT = 2;
    public static final int MODIFIER_SLOT_COUNT = 3;
    public static final int ENCHANTING_SLOT_COUNT = 5;
    public static final int DISABLED_BLOCKED_BY_MODIFIER = 1;
    public static final int DISABLED_NO_ROLL_POWER = 1 << 1;
    public static final int DISABLED_NO_COMPATIBLE_ENCHANTMENTS = 1 << 2;
    public static final int DISABLED_RESTRICTED_POOL_EMPTY = 1 << 3;
    public static final int DISABLED_REMOVED_TAGS_EMPTY = 1 << 4;
    public static final int DISABLED_ENCHANTMENT_LIMIT = 1 << 5;
    public static final int DISABLED_WEIGHT_SELECTION_FAILED = 1 << 6;
    public static final int DISABLED_NO_OFFER_POWER = 1 << 7;
    public static final int DISABLED_FUSION_LIMIT = 1 << 8;
    public static final int DISABLED_APOTHIC_INFUSION_UNMET = 1 << 9;
    public static final int DISABLED_APOTHIC_INFUSION_MODIFIER = 1 << 10;

    private static final int PLAYER_INVENTORY_START = ENCHANTING_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int MODIFIER_SLOT_X = 176;
    private static final int MODIFIER_SLOT_Y = 15;
    private static final int MODIFIER_SLOT_GAP = 19;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int APOTHIC_PLAYER_INVENTORY_Y = 115;
    private static final int APOTHIC_HOTBAR_Y = 173;
    private static final int APOTHIC_STATS_SCALE = 100;
    private static final int APOTHIC_FLAGS_STABLE = 1;
    private static final int APOTHIC_FLAGS_TREASURE = 1 << 1;
    private static final int PLAYER_LEVEL_COST = 0;
    private static final int MAX_REVEALED_CLUES_PER_OPTION = 6;
    private static final ResourceLocation EMPTY_LAPIS_SLOT = ResourceLocation.withDefaultNamespace("item/empty_slot_lapis_lazuli");
    private static final int APOTHIC_INFUSION_OPTION = 2;
    private static final ResourceKey<Enchantment> APOTHIC_INFUSION = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath("apothic_enchanting", "infusion")
    );

    private final Container enchantingSlots = new SimpleContainer(ENCHANTING_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnhancedEnchantingMenu.this.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;
    private final net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
    private final DataSlot enchantmentSeed = DataSlot.standalone();
    private final boolean apothicLayout = ApothicEnchantingCompat.isLoaded();

    public final int[] requirements = new int[]{0, 0, 0};
    public final int[] costs = new int[]{0, 0, 0};
    public final int[] enchantClue = new int[]{-1, -1, -1};
    public final int[] levelClue = new int[]{-1, -1, -1};
    private final int[] poolSizes = new int[]{0, 0, 0};
    private final int[] activeTagCounts = new int[]{0, 0, 0};
    private final int[] bookBoostCounts = new int[]{0, 0, 0};
    private final int[] disabledReasonFlags = new int[]{0, 0, 0};
    private final int[] overlevelOffers = new int[]{0, 0, 0};
    private final int[] apothicInfusionOffers = new int[]{0, 0, 0};
    private final int[] revealedClueCounts = new int[]{0, 0, 0};
    private final int[] allCluesRevealed = new int[]{0, 0, 0};
    private final int[] revealedClueIds = filledArray(MODIFIER_SLOT_COUNT * MAX_REVEALED_CLUES_PER_OPTION, -1);
    private final int[] revealedClueLevels = filledArray(MODIFIER_SLOT_COUNT * MAX_REVEALED_CLUES_PER_OPTION, -1);
    private final int[] apothicStatsData = new int[]{0, 0, 0, 0};

    public EnhancedEnchantingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, BlockPos.ZERO);
    }

    public EnhancedEnchantingMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, buffer == null ? BlockPos.ZERO : buffer.readBlockPos());
    }

    public EnhancedEnchantingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, BlockPos pos) {
        super(ModMenus.ENHANCED_ENCHANTING.get(), containerId);
        this.access = access;

        this.addSlot(new Slot(this.enchantingSlots, TARGET_SLOT, 15, 47) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return isEnchantingTarget(stack) || isPotentialApothicInfusionInput(stack);
            }
        });
        this.addSlot(new Slot(this.enchantingSlots, LAPIS_SLOT, 35, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAPIS_LAZULI);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_LAPIS_SLOT);
            }
        });

        for (int slot = 0; slot < MODIFIER_SLOT_COUNT; slot++) {
            this.addSlot(new Slot(this.enchantingSlots, FIRST_MODIFIER_SLOT + slot, MODIFIER_SLOT_X, MODIFIER_SLOT_Y + slot * MODIFIER_SLOT_GAP) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return PoolModifierRules.isPoolModifier(stack);
                }
            });
        }

        int playerInventoryY = this.apothicLayout ? APOTHIC_PLAYER_INVENTORY_Y : PLAYER_INVENTORY_Y;
        int hotbarY = this.apothicLayout ? APOTHIC_HOTBAR_Y : HOTBAR_Y;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, playerInventoryY + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, hotbarY));
        }

        this.addDataSlot(DataSlot.shared(this.requirements, 0));
        this.addDataSlot(DataSlot.shared(this.requirements, 1));
        this.addDataSlot(DataSlot.shared(this.requirements, 2));
        this.addDataSlot(DataSlot.shared(this.costs, 0));
        this.addDataSlot(DataSlot.shared(this.costs, 1));
        this.addDataSlot(DataSlot.shared(this.costs, 2));
        this.addDataSlot(this.enchantmentSeed).set(playerInventory.player.getEnchantmentSeed());
        this.addDataSlot(DataSlot.shared(this.enchantClue, 0));
        this.addDataSlot(DataSlot.shared(this.enchantClue, 1));
        this.addDataSlot(DataSlot.shared(this.enchantClue, 2));
        this.addDataSlot(DataSlot.shared(this.levelClue, 0));
        this.addDataSlot(DataSlot.shared(this.levelClue, 1));
        this.addDataSlot(DataSlot.shared(this.levelClue, 2));
        this.addDataSlot(DataSlot.shared(this.poolSizes, 0));
        this.addDataSlot(DataSlot.shared(this.poolSizes, 1));
        this.addDataSlot(DataSlot.shared(this.poolSizes, 2));
        this.addDataSlot(DataSlot.shared(this.activeTagCounts, 0));
        this.addDataSlot(DataSlot.shared(this.activeTagCounts, 1));
        this.addDataSlot(DataSlot.shared(this.activeTagCounts, 2));
        this.addDataSlot(DataSlot.shared(this.bookBoostCounts, 0));
        this.addDataSlot(DataSlot.shared(this.bookBoostCounts, 1));
        this.addDataSlot(DataSlot.shared(this.bookBoostCounts, 2));
        this.addDataSlot(DataSlot.shared(this.disabledReasonFlags, 0));
        this.addDataSlot(DataSlot.shared(this.disabledReasonFlags, 1));
        this.addDataSlot(DataSlot.shared(this.disabledReasonFlags, 2));
        this.addDataSlot(DataSlot.shared(this.overlevelOffers, 0));
        this.addDataSlot(DataSlot.shared(this.overlevelOffers, 1));
        this.addDataSlot(DataSlot.shared(this.overlevelOffers, 2));
        this.addDataSlot(DataSlot.shared(this.apothicInfusionOffers, 0));
        this.addDataSlot(DataSlot.shared(this.apothicInfusionOffers, 1));
        this.addDataSlot(DataSlot.shared(this.apothicInfusionOffers, 2));
        this.addDataSlot(DataSlot.shared(this.revealedClueCounts, 0));
        this.addDataSlot(DataSlot.shared(this.revealedClueCounts, 1));
        this.addDataSlot(DataSlot.shared(this.revealedClueCounts, 2));
        this.addDataSlot(DataSlot.shared(this.allCluesRevealed, 0));
        this.addDataSlot(DataSlot.shared(this.allCluesRevealed, 1));
        this.addDataSlot(DataSlot.shared(this.allCluesRevealed, 2));
        for (int index = 0; index < this.revealedClueIds.length; index++) {
            this.addDataSlot(DataSlot.shared(this.revealedClueIds, index));
            this.addDataSlot(DataSlot.shared(this.revealedClueLevels, index));
        }
        this.addDataSlot(DataSlot.shared(this.apothicStatsData, 0));
        this.addDataSlot(DataSlot.shared(this.apothicStatsData, 1));
        this.addDataSlot(DataSlot.shared(this.apothicStatsData, 2));
        this.addDataSlot(DataSlot.shared(this.apothicStatsData, 3));
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (inventory != this.enchantingSlots) {
            return;
        }

        ItemStack target = inventory.getItem(TARGET_SLOT);
        if (target.isEmpty()) {
            clearPreviews();
            return;
        }

        this.access.execute((level, blockPos) -> {
            boolean infusionInput = ApothicEnchantingCompat.hasInfusionItemMatch(level, target);
            if (!isEnchantingTarget(target) && !infusionInput) {
                clearPreviews();
                return;
            }

            IdMap<Holder<Enchantment>> ids = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            ItemStack costTarget = costTarget(target);
            Optional<ApothicEnchantingCompat.TableStats> apothicStats = ApothicEnchantingCompat.gatherTableStats(level, blockPos, costTarget);
            this.setApothicStats(apothicStats);
            int bookshelfPower = apothicStats
                    .map(ApothicEnchantingCompat.TableStats::bookshelfPower)
                    .orElseGet(() -> EnchantingPowerRules.clampBookshelfPower(EnchantingTablePower.bookshelfPower(level, blockPos)));
            PoolModifierRules.ModifierPlan modifierPlan = modifierPlan();
            this.random.setSeed((long) this.enchantmentSeed.get());

            for (int option = 0; option < 3; option++) {
                this.enchantClue[option] = -1;
                this.levelClue[option] = -1;
                this.clearRevealedClues(option);
                this.poolSizes[option] = 0;
                this.activeTagCounts[option] = 0;
                this.bookBoostCounts[option] = 0;
                this.disabledReasonFlags[option] = 0;
                this.overlevelOffers[option] = 0;
                this.apothicInfusionOffers[option] = 0;
                this.requirements[option] = 0;
                this.costs[option] = 0;

                ItemStack modifier = modifierStack(modifierPlan, option);

                int baseRequirement = EnchantingPowerRules.offerRequirementForBookshelfPower(
                        bookshelfPower,
                        option,
                        this.enchantmentSeed.get(),
                        costTarget
                );
                if (apothicStats.isPresent()) {
                    baseRequirement = ApothicEnchantingCompat.offerRequirement(apothicStats.get(), option, this.enchantmentSeed.get(), costTarget);
                }
                int requiredLevel = net.neoforged.neoforge.event.EventHooks.onEnchantmentLevelSet(
                        level,
                        blockPos,
                        option,
                        bookshelfPower,
                        target,
                        baseRequirement
                );
                this.requirements[option] = Math.max(0, requiredLevel);
                this.costs[option] = option + 1;
                if (this.requirements[option] <= 0) {
                    this.costs[option] = 0;
                    this.disabledReasonFlags[option] = DISABLED_NO_OFFER_POWER;
                    continue;
                }

                if (option == APOTHIC_INFUSION_OPTION && apothicStats.isPresent()) {
                    Optional<ApothicEnchantingCompat.InfusionMatch> infusionMatch = ApothicEnchantingCompat.findInfusion(level, target, apothicStats.get());
                    if (infusionMatch.isPresent()) {
                        this.poolSizes[option] = 1;
                        this.apothicInfusionOffers[option] = 1;
                        Optional<Holder.Reference<Enchantment>> infusion = level.registryAccess()
                                .registryOrThrow(Registries.ENCHANTMENT)
                                .getHolder(APOTHIC_INFUSION);
                        if (infusion.isPresent()) {
                            this.setSingleRevealedClue(option, ids.getId(infusion.get()), 1);
                        }
                        ApothicInfusionModifierRules.Match modifierMatch = infusionModifierMatch(infusionMatch.get());
                        if (!modifierMatch.matches()) {
                            this.disabledReasonFlags[option] = DISABLED_APOTHIC_INFUSION_MODIFIER;
                        }
                        continue;
                    }
                    if (infusionInput && !isEnchantingTarget(target)) {
                        this.disabledReasonFlags[option] = DISABLED_APOTHIC_INFUSION_UNMET;
                        continue;
                    }
                }

                if (PoolModifierRules.blocksOffer(modifierPlan, option)) {
                    this.disabledReasonFlags[option] = DISABLED_BLOCKED_BY_MODIFIER;
                    continue;
                }

                List<ItemStack> essences = optionEssenceStacks(modifierPlan, option);
                List<ItemStack> books = optionBookStacks(modifierPlan, option);
                EnchantingRoller.InputProfile profile = EnchantingRoller.profile(target, essences, books);
                Optional<OverlevelTarget> overlevelTarget = overlevelTarget(target, modifierPlan, option);
                if (overlevelTarget.isPresent()) {
                    OverlevelTarget targetOverlevel = overlevelTarget.get();
                    this.poolSizes[option] = 1;
                    this.activeTagCounts[option] = profile.essenceTags().size();
                    this.bookBoostCounts[option] = profile.bookBoostCount();
                    this.setSingleRevealedClue(option, ids.getId(targetOverlevel.enchantment()), targetOverlevel.overleveledLevel());
                    this.overlevelOffers[option] = 1;
                    continue;
                }

                int rollPower = EnchantingPowerRules.rollPower(this.requirements[option], costTarget, modifier);
                rollPower = ApothicEnchantingCompat.adjustRollPower(apothicStats, option, this.enchantmentSeed.get(), rollPower);
                EnchantingRoller.RollPreview preview = EnchantingRoller.preview(
                        level.registryAccess(),
                        target,
                        rollPower,
                        option,
                        this.enchantmentSeed.get(),
                        essences,
                        books,
                        apothicStats
                );
                this.poolSizes[option] = preview.poolSize();
                this.activeTagCounts[option] = preview.profile().essenceTags().size();
                this.bookBoostCounts[option] = preview.profile().bookBoostCount();
                this.disabledReasonFlags[option] = disabledReasonFlags(preview);
                if (!preview.enchantments().isEmpty()) {
                    if (!canApplyWithFusion(level.registryAccess(), target, preview.enchantments())) {
                        this.disabledReasonFlags[option] |= DISABLED_FUSION_LIMIT;
                        continue;
                    }
                    List<EnchantmentInstance> clues = new ArrayList<>(preview.enchantments());
                    EnchantmentInstance clue = clues.remove(this.random.nextInt(clues.size()));
                    int clueBudget = apothicStats.map(ApothicEnchantingCompat.TableStats::clues).orElse(1);
                    this.setRevealedClues(option, ids, clue, clues, clueBudget);
                }
            }

            this.broadcastChanges();
        });
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= this.requirements.length) {
            Util.logAndPauseIfInIde(player.getName() + " pressed invalid enhanced enchanting button id: " + id);
            return false;
        }
        if (this.disabledReasonFlags[id] != 0) {
            return false;
        }

        ItemStack target = this.enchantingSlots.getItem(TARGET_SLOT);
        ItemStack lapis = this.enchantingSlots.getItem(LAPIS_SLOT);
        int lapisCost = EffectiveBalance.enchantingLapisCost();
        if (lapisCost > 0 && (lapis.isEmpty() || lapis.getCount() < lapisCost) && !player.hasInfiniteMaterials()) {
            return false;
        }
        int requiredLevel = this.requirements[id];
        int displayCost = this.costs[id];
        boolean apothicInfusionOffer = isApothicInfusionOffer(id);
        if (requiredLevel <= 0
                || displayCost <= 0
                || (!isEnchantingTarget(target) && !apothicInfusionOffer)) {
            return false;
        }

        this.access.execute((level, blockPos) -> {
            ItemStack costTarget = costTarget(target);
            Optional<ApothicEnchantingCompat.TableStats> apothicStats = ApothicEnchantingCompat.gatherTableStats(level, blockPos, costTarget);
            PoolModifierRules.ModifierPlan modifierPlan = modifierPlan();
            if (id == APOTHIC_INFUSION_OPTION && apothicStats.isPresent()) {
                Optional<ApothicEnchantingCompat.InfusionMatch> infusionMatch = ApothicEnchantingCompat.findInfusion(level, target, apothicStats.get());
                if (infusionMatch.isPresent()) {
                    ApothicInfusionModifierRules.Match modifierMatch = infusionModifierMatch(infusionMatch.get());
                    if (!modifierMatch.matches()) {
                        return;
                    }

                    player.onEnchantmentPerformed(target, PLAYER_LEVEL_COST);
                    ItemStack infused = infusionMatch.get().result().copy();
                    this.enchantingSlots.setItem(TARGET_SLOT, infused);

                    if (!player.hasInfiniteMaterials() && lapisCost > 0) {
                        lapis.consume(lapisCost, player);
                        if (lapis.isEmpty()) {
                            this.enchantingSlots.setItem(LAPIS_SLOT, ItemStack.EMPTY);
                        }
                    }
                    if (!player.hasInfiniteMaterials()) {
                        ApothicInfusionModifierRules.consume(this.enchantingSlots, modifierMatch, player);
                    }

                    player.awardStat(Stats.ENCHANT_ITEM);
                    if (player instanceof ServerPlayer serverPlayer) {
                        CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, infused, PLAYER_LEVEL_COST);
                    }

                    this.enchantingSlots.setChanged();
                    this.enchantmentSeed.set(player.getEnchantmentSeed());
                    this.slotsChanged(this.enchantingSlots);
                    level.playSound(null, blockPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
                    return;
                }
                if (!isEnchantingTarget(target)) {
                    return;
                }
            }

            Optional<OverlevelTarget> overlevelTarget = overlevelTarget(target, modifierPlan, id);
            if (overlevelTarget.isPresent()) {
                OverlevelTarget targetOverlevel = overlevelTarget.get();
                player.onEnchantmentPerformed(target, PLAYER_LEVEL_COST);
                Optional<ItemStack> routedOverlevel = ModularMaterialCompat.overlevelRoutedEnchantment(level.registryAccess(), target, targetOverlevel.enchantment());
                if (routedOverlevel.isEmpty() && ModularMaterialCompat.hasRoutedParts(target)) {
                    return;
                }
                ItemStack enchanted = routedOverlevel.orElseGet(() -> {
                            ItemStack copy = target.copy();
                            EnchantmentLevelRules.overlevel(copy, targetOverlevel.enchantment());
                            return copy;
                        });
                EnchantmentLevelRules.clampEnchantments(enchanted);
                this.enchantingSlots.setItem(TARGET_SLOT, enchanted);
                List<EnchantmentInstance> overlevelEnchantments = List.of(new EnchantmentInstance(
                        targetOverlevel.enchantment(),
                        targetOverlevel.overleveledLevel()
                ));
                net.neoforged.neoforge.common.CommonHooks.onPlayerEnchantItem(player, enchanted, overlevelEnchantments);

                if (!player.hasInfiniteMaterials()) {
                    if (lapisCost > 0) {
                        lapis.consume(lapisCost, player);
                        if (lapis.isEmpty()) {
                            this.enchantingSlots.setItem(LAPIS_SLOT, ItemStack.EMPTY);
                        }
                    }
                    PoolModifierRules.consumeForOption(this.enchantingSlots, modifierPlan, id, player, true);
                }

                player.awardStat(Stats.ENCHANT_ITEM);
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, enchanted, PLAYER_LEVEL_COST);
                }

                this.enchantingSlots.setChanged();
                this.enchantmentSeed.set(player.getEnchantmentSeed());
                this.slotsChanged(this.enchantingSlots);
                level.playSound(null, blockPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
                return;
            }

            int rollPower = EnchantingPowerRules.rollPower(this.requirements[id], costTarget, modifierStack(modifierPlan, id));
            rollPower = ApothicEnchantingCompat.adjustRollPower(apothicStats, id, this.enchantmentSeed.get(), rollPower);
            List<EnchantmentInstance> enchantments = EnchantingRoller.preview(
                    level.registryAccess(),
                    target,
                    rollPower,
                    id,
                    this.enchantmentSeed.get(),
                    optionEssenceStacks(modifierPlan, id),
                    optionBookStacks(modifierPlan, id),
                    apothicStats
            ).enchantments();

            if (enchantments.isEmpty()) {
                return;
            }
            if (!canApplyWithFusion(level.registryAccess(), target, enchantments)) {
                return;
            }

            player.onEnchantmentPerformed(target, PLAYER_LEVEL_COST);
            Optional<ItemStack> routedEnchanted = ModularMaterialCompat.applyRoutedEnchantments(level.registryAccess(), target, enchantments);
            if (routedEnchanted.isEmpty() && ModularMaterialCompat.hasRoutedParts(target)) {
                return;
            }
            ItemStack enchanted = routedEnchanted.orElseGet(() -> target.getItem().applyEnchantments(target, enchantments));
            FortunesTouchEnchantmentEvents.fuseFortunesTouch(level.registryAccess(), enchanted);
            ModularMaterialCompat.reconcileRoutedEnchantments(level.registryAccess(), enchanted);
            EnchantmentLevelRules.clampEnchantments(enchanted);
            this.enchantingSlots.setItem(TARGET_SLOT, enchanted);
            net.neoforged.neoforge.common.CommonHooks.onPlayerEnchantItem(player, enchanted, enchantments);

            if (!player.hasInfiniteMaterials()) {
                if (lapisCost > 0) {
                    lapis.consume(lapisCost, player);
                    if (lapis.isEmpty()) {
                        this.enchantingSlots.setItem(LAPIS_SLOT, ItemStack.EMPTY);
                    }
                }
                PoolModifierRules.consumeForOption(this.enchantingSlots, modifierPlan, id, player, false);
            }

            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, enchanted, PLAYER_LEVEL_COST);
            }

            this.enchantingSlots.setChanged();
            this.enchantmentSeed.set(player.getEnchantmentSeed());
            this.slotsChanged(this.enchantingSlots);
            level.playSound(null, blockPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
        });
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.enchantingSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, blockPos) -> {
            BlockState state = level.getBlockState(blockPos);
            boolean validStation = state.is(ModBlocks.ARCANE_CRUCIBLE.get())
                    || state.is(Blocks.ENCHANTING_TABLE) && EffectiveBalance.takesOverEnchantingTable();
            return validStation && player.canInteractWithBlock(blockPos, 4.0D);
        }, true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < ENCHANTING_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.LAPIS_LAZULI)) {
            if (!this.moveItemStackTo(stack, LAPIS_SLOT, LAPIS_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (PoolModifierRules.isPoolModifier(stack)) {
            if (!this.moveItemStackTo(stack, FIRST_MODIFIER_SLOT, FIRST_MODIFIER_SLOT + MODIFIER_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isEnchantingTarget(stack) || ApothicEnchantingCompat.hasInfusionItemMatch(player.level(), stack)) {
            if (!this.moveItemStackTo(stack, TARGET_SLOT, TARGET_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < HOTBAR_END && !this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return moved;
    }

    public int getLapisCount() {
        ItemStack lapis = this.enchantingSlots.getItem(LAPIS_SLOT);
        return lapis.isEmpty() ? 0 : lapis.getCount();
    }

    public int getLapisCost() {
        return EffectiveBalance.enchantingLapisCost();
    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed.get();
    }

    public int getPoolSize(int option) {
        return option < 0 || option >= this.poolSizes.length ? 0 : this.poolSizes[option];
    }

    public int getActiveTagCount(int option) {
        return option < 0 || option >= this.activeTagCounts.length ? 0 : this.activeTagCounts[option];
    }

    public int getBookBoostCount(int option) {
        return option < 0 || option >= this.bookBoostCounts.length ? 0 : this.bookBoostCounts[option];
    }

    public int getDisabledReasonFlags(int option) {
        return option < 0 || option >= this.disabledReasonFlags.length ? 0 : this.disabledReasonFlags[option];
    }

    public boolean isOverlevelOffer(int option) {
        return option >= 0 && option < this.overlevelOffers.length && this.overlevelOffers[option] != 0;
    }

    public boolean isApothicInfusionOffer(int option) {
        return option >= 0 && option < this.apothicInfusionOffers.length && this.apothicInfusionOffers[option] != 0;
    }

    public int getRevealedClueCount(int option) {
        if (option < 0 || option >= this.revealedClueCounts.length) {
            return 0;
        }
        return Math.min(this.revealedClueCounts[option], MAX_REVEALED_CLUES_PER_OPTION);
    }

    public int getRevealedClueId(int option, int clueIndex) {
        if (option < 0 || option >= MODIFIER_SLOT_COUNT || clueIndex < 0 || clueIndex >= MAX_REVEALED_CLUES_PER_OPTION) {
            return -1;
        }
        return this.revealedClueIds[revealedClueIndex(option, clueIndex)];
    }

    public int getRevealedClueLevel(int option, int clueIndex) {
        if (option < 0 || option >= MODIFIER_SLOT_COUNT || clueIndex < 0 || clueIndex >= MAX_REVEALED_CLUES_PER_OPTION) {
            return -1;
        }
        return this.revealedClueLevels[revealedClueIndex(option, clueIndex)];
    }

    public boolean areAllCluesRevealed(int option) {
        return option >= 0 && option < this.allCluesRevealed.length && this.allCluesRevealed[option] != 0;
    }

    public boolean usesApothicLayout() {
        return this.apothicLayout;
    }

    public float getApothicEterna() {
        return this.apothicStatsData[0] / (float) APOTHIC_STATS_SCALE;
    }

    public float getApothicQuanta() {
        return this.apothicStatsData[1] / (float) APOTHIC_STATS_SCALE;
    }

    public float getApothicArcana() {
        return this.apothicStatsData[2] / (float) APOTHIC_STATS_SCALE;
    }

    public boolean isApothicStable() {
        return (this.apothicStatsData[3] & APOTHIC_FLAGS_STABLE) != 0;
    }

    public boolean allowsApothicTreasure() {
        return (this.apothicStatsData[3] & APOTHIC_FLAGS_TREASURE) != 0;
    }

    public OptionDetails getOptionDetails(int option) {
        PoolModifierRules.ModifierPlan plan = modifierPlan();
        ItemStack target = this.enchantingSlots.getItem(TARGET_SLOT);
        List<ItemStack> essences = optionEssenceStacks(plan, option);
        List<ItemStack> books = optionBookStacks(plan, option);
        return new OptionDetails(
                EnchantingRoller.profile(target, essences, books),
                modifierStack(plan, option).copy(),
                PoolModifierRules.blockingModifierStack(plan, option).copy(),
                PoolModifierRules.globalModifierStacks(plan).stream().map(ItemStack::copy).toList(),
                books.stream().map(ItemStack::copy).toList()
        );
    }

    private PoolModifierRules.ModifierPlan modifierPlan() {
        return PoolModifierRules.plan(this.enchantingSlots, FIRST_MODIFIER_SLOT, MODIFIER_SLOT_COUNT, this.enchantmentSeed.get());
    }

    private ApothicInfusionModifierRules.Match infusionModifierMatch(ApothicEnchantingCompat.InfusionMatch infusionMatch) {
        return infusionMatch.recipeId()
                .map(recipeId -> ApothicInfusionModifierRules.match(
                        recipeId,
                        this.enchantingSlots,
                        FIRST_MODIFIER_SLOT,
                        MODIFIER_SLOT_COUNT
                ))
                .orElseGet(ApothicInfusionModifierRules::unrestricted);
    }

    private ItemStack modifierStack(PoolModifierRules.ModifierPlan plan, int option) {
        return PoolModifierRules.modifierStack(plan, option);
    }

    private List<ItemStack> optionEssenceStacks(PoolModifierRules.ModifierPlan plan, int option) {
        return PoolModifierRules.optionEssences(plan, option);
    }

    private List<ItemStack> optionBookStacks(PoolModifierRules.ModifierPlan plan, int option) {
        return PoolModifierRules.optionBooks(plan, option);
    }

    private static int disabledReasonFlags(EnchantingRoller.RollPreview preview) {
        if (!preview.enchantments().isEmpty()) {
            return 0;
        }
        return switch (preview.emptyReason()) {
            case NONE -> DISABLED_NO_COMPATIBLE_ENCHANTMENTS;
            case NO_ROLL_POWER -> DISABLED_NO_ROLL_POWER;
            case NO_COMPATIBLE_ENCHANTMENTS -> DISABLED_NO_COMPATIBLE_ENCHANTMENTS;
            case RESTRICTED_POOL_EMPTY -> DISABLED_RESTRICTED_POOL_EMPTY;
            case REMOVED_TAGS_EMPTY -> DISABLED_REMOVED_TAGS_EMPTY;
            case ENCHANTMENT_LIMIT -> DISABLED_ENCHANTMENT_LIMIT;
            case WEIGHT_SELECTION_FAILED -> DISABLED_WEIGHT_SELECTION_FAILED;
        };
    }

    private static boolean canApplyWithFusion(RegistryAccess registryAccess, ItemStack target, List<EnchantmentInstance> enchantments) {
        ItemStack simulatedTarget = target.copy();
        Optional<ItemStack> routedResult = ModularMaterialCompat.applyRoutedEnchantments(registryAccess, simulatedTarget, enchantments);
        if (routedResult.isEmpty() && ModularMaterialCompat.hasRoutedParts(target)) {
            return false;
        }
        ItemStack simulatedResult = routedResult.orElseGet(() -> simulatedTarget.getItem().applyEnchantments(simulatedTarget, enchantments));
        FortunesTouchEnchantmentEvents.fuseFortunesTouch(registryAccess, simulatedResult);
        ModularMaterialCompat.reconcileRoutedEnchantments(registryAccess, simulatedResult);
        EnchantmentLevelRules.clampEnchantments(simulatedResult);
        return EnchantmentLimitRules.isWithinLimits(simulatedResult);
    }

    private static Optional<OverlevelTarget> overlevelTarget(ItemStack target, PoolModifierRules.ModifierPlan plan, int option) {
        if (!PoolModifierRules.hasOverlevelCatalyst(plan)) {
            return Optional.empty();
        }

        for (ItemStack essence : PoolModifierRules.optionEssences(plan, option)) {
            Optional<OverlevelTarget> overlevelTarget = EnchantmentLevelRules.overlevelTarget(target, essence);
            if (overlevelTarget.isPresent()) {
                return overlevelTarget;
            }
        }
        return Optional.empty();
    }

    private static boolean isEnchantingTarget(ItemStack stack) {
        return !stack.isEmpty()
                && !ModularMaterialCompat.blocksDirectPartEnchanting(stack)
                && (stack.getItem().isEnchantable(stack)
                || stack.is(Items.ENCHANTED_BOOK)
                || !EnchantmentTargetTags.resolve(stack).isEmpty());
    }

    private static boolean isPotentialApothicInfusionInput(ItemStack stack) {
        return !stack.isEmpty() && ApothicEnchantingCompat.isLoaded();
    }

    private static ItemStack costTarget(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) ? new ItemStack(Items.BOOK) : stack;
    }

    private void setSingleRevealedClue(int option, int enchantmentId, int enchantmentLevel) {
        this.enchantClue[option] = enchantmentId;
        this.levelClue[option] = enchantmentLevel;
        this.clearRevealedClues(option);
        this.setRevealedClue(option, 0, enchantmentId, enchantmentLevel);
        this.revealedClueCounts[option] = 1;
        this.allCluesRevealed[option] = 1;
    }

    private void setRevealedClues(
            int option,
            IdMap<Holder<Enchantment>> ids,
            EnchantmentInstance primaryClue,
            List<EnchantmentInstance> remainingClues,
            int clueBudget
    ) {
        int primaryId = ids.getId(primaryClue.enchantment);
        this.enchantClue[option] = primaryId;
        this.levelClue[option] = primaryClue.level;
        this.clearRevealedClues(option);

        int totalClues = 1 + remainingClues.size();
        int revealedCount = Math.min(Math.max(0, clueBudget), Math.min(MAX_REVEALED_CLUES_PER_OPTION, totalClues));
        if (revealedCount <= 0) {
            return;
        }

        this.setRevealedClue(option, 0, primaryId, primaryClue.level);
        List<EnchantmentInstance> pool = new ArrayList<>(remainingClues);
        for (int clueIndex = 1; clueIndex < revealedCount && !pool.isEmpty(); clueIndex++) {
            EnchantmentInstance clue = pool.remove(this.random.nextInt(pool.size()));
            this.setRevealedClue(option, clueIndex, ids.getId(clue.enchantment), clue.level);
        }
        this.revealedClueCounts[option] = revealedCount;
        this.allCluesRevealed[option] = revealedCount >= totalClues ? 1 : 0;
    }

    private void setRevealedClue(int option, int clueIndex, int enchantmentId, int enchantmentLevel) {
        int index = revealedClueIndex(option, clueIndex);
        this.revealedClueIds[index] = enchantmentId;
        this.revealedClueLevels[index] = enchantmentLevel;
    }

    private void clearRevealedClues(int option) {
        if (option < 0 || option >= MODIFIER_SLOT_COUNT) {
            return;
        }
        this.revealedClueCounts[option] = 0;
        this.allCluesRevealed[option] = 0;
        for (int clueIndex = 0; clueIndex < MAX_REVEALED_CLUES_PER_OPTION; clueIndex++) {
            int index = revealedClueIndex(option, clueIndex);
            this.revealedClueIds[index] = -1;
            this.revealedClueLevels[index] = -1;
        }
    }

    private static int revealedClueIndex(int option, int clueIndex) {
        return option * MAX_REVEALED_CLUES_PER_OPTION + clueIndex;
    }

    private void setApothicStats(Optional<ApothicEnchantingCompat.TableStats> stats) {
        if (stats.isEmpty()) {
            this.apothicStatsData[0] = 0;
            this.apothicStatsData[1] = 0;
            this.apothicStatsData[2] = 0;
            this.apothicStatsData[3] = 0;
            return;
        }

        ApothicEnchantingCompat.TableStats tableStats = stats.get();
        this.apothicStatsData[0] = scaledStat(tableStats.eterna());
        this.apothicStatsData[1] = scaledStat(tableStats.quanta());
        this.apothicStatsData[2] = scaledStat(tableStats.arcana());
        this.apothicStatsData[3] = (tableStats.stable() ? APOTHIC_FLAGS_STABLE : 0)
                | (tableStats.treasure() ? APOTHIC_FLAGS_TREASURE : 0);
    }

    private static int scaledStat(float stat) {
        return Math.max(0, Math.round(stat * APOTHIC_STATS_SCALE));
    }

    private static int[] filledArray(int size, int value) {
        int[] values = new int[size];
        for (int index = 0; index < values.length; index++) {
            values[index] = value;
        }
        return values;
    }

    private void clearPreviews() {
        for (int index = 0; index < 3; index++) {
            this.requirements[index] = 0;
            this.costs[index] = 0;
            this.enchantClue[index] = -1;
            this.levelClue[index] = -1;
            this.clearRevealedClues(index);
            this.poolSizes[index] = 0;
            this.disabledReasonFlags[index] = 0;
            this.overlevelOffers[index] = 0;
            this.apothicInfusionOffers[index] = 0;
        }
        for (int index = 0; index < 3; index++) {
            this.activeTagCounts[index] = 0;
            this.bookBoostCounts[index] = 0;
        }
        this.setApothicStats(Optional.empty());
        this.broadcastChanges();
    }

    public record OptionDetails(
            EnchantingRoller.InputProfile profile,
            ItemStack directModifier,
            ItemStack blockingModifier,
            List<ItemStack> globalModifiers,
            List<ItemStack> bookModifiers
    ) {
    }
}
