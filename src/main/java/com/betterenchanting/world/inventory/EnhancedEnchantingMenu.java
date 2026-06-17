package com.betterenchanting.world.inventory;

import com.mojang.datafixers.util.Pair;
import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.data.EnchantmentLimitRules;
import com.betterenchanting.registry.ModBlocks;
import com.betterenchanting.registry.ModMenus;
import com.betterenchanting.world.EnchantingRoller;
import com.betterenchanting.world.enchantment.FortunesTouchEnchantmentEvents;
import com.betterenchanting.world.level.block.EnchantingTablePower;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

    private static final int PLAYER_INVENTORY_START = ENCHANTING_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final ResourceLocation EMPTY_LAPIS_SLOT = ResourceLocation.withDefaultNamespace("item/empty_slot_lapis_lazuli");

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

    public final int[] requirements = new int[]{0, 0, 0};
    public final int[] costs = new int[]{0, 0, 0};
    public final int[] enchantClue = new int[]{-1, -1, -1};
    public final int[] levelClue = new int[]{-1, -1, -1};
    private final int[] poolSizes = new int[]{0, 0, 0};
    private final int[] activeTagCounts = new int[]{0, 0, 0};
    private final int[] bookBoostCounts = new int[]{0, 0, 0};

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
                return isEnchantingTarget(stack);
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
            this.addSlot(new Slot(this.enchantingSlots, FIRST_MODIFIER_SLOT + slot, 180, 15 + slot * 19) {
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

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
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
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (inventory != this.enchantingSlots) {
            return;
        }

        ItemStack target = inventory.getItem(TARGET_SLOT);
        if (!isEnchantingTarget(target)) {
            clearPreviews();
            return;
        }

        this.access.execute((level, blockPos) -> {
            IdMap<Holder<Enchantment>> ids = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            int bookshelfPower = EnchantingPowerRules.clampBookshelfPower(EnchantingTablePower.bookshelfPower(level, blockPos));
            int baseRequirement = EnchantingPowerRules.offerRequirementForBookshelfPower(bookshelfPower);
            int levelCost = EnchantingPowerRules.levelCostForBookshelfPower(bookshelfPower);
            ItemStack costTarget = costTarget(target);
            PoolModifierRules.ModifierPlan modifierPlan = modifierPlan();
            this.random.setSeed((long) this.enchantmentSeed.get());

            for (int option = 0; option < 3; option++) {
                this.enchantClue[option] = -1;
                this.levelClue[option] = -1;
                this.poolSizes[option] = 0;
                this.activeTagCounts[option] = 0;
                this.bookBoostCounts[option] = 0;
                this.requirements[option] = 0;
                this.costs[option] = 0;

                if (PoolModifierRules.blocksOffer(modifierPlan, option)) {
                    continue;
                }
                ItemStack modifier = modifierStack(modifierPlan, option);

                int requiredLevel = net.neoforged.neoforge.event.EventHooks.onEnchantmentLevelSet(
                        level,
                        blockPos,
                        option,
                        bookshelfPower,
                        target,
                        baseRequirement
                );
                this.requirements[option] = Math.max(0, requiredLevel);
                this.costs[option] = levelCost;
                if (this.requirements[option] <= 0) {
                    this.costs[option] = 0;
                    continue;
                }

                EnchantingRoller.RollPreview preview = EnchantingRoller.preview(
                        level.registryAccess(),
                        target,
                        EnchantingPowerRules.rollPower(this.requirements[option], costTarget, modifier),
                        option,
                        this.enchantmentSeed.get(),
                        optionEssenceStacks(modifierPlan, option),
                        optionBookStacks(modifierPlan, option)
                );
                this.poolSizes[option] = preview.poolSize();
                this.activeTagCounts[option] = preview.profile().essenceTags().size();
                this.bookBoostCounts[option] = preview.profile().bookBoostCount();
                if (!preview.enchantments().isEmpty()) {
                    EnchantmentInstance clue = preview.enchantments().get(this.random.nextInt(preview.enchantments().size()));
                    this.enchantClue[option] = ids.getId(clue.enchantment);
                    this.levelClue[option] = clue.level;
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

        ItemStack target = this.enchantingSlots.getItem(TARGET_SLOT);
        ItemStack lapis = this.enchantingSlots.getItem(LAPIS_SLOT);
        int lapisCost = BetterEnchantingConfig.enchantingLapisCost();
        if (lapisCost > 0 && (lapis.isEmpty() || lapis.getCount() < lapisCost) && !player.hasInfiniteMaterials()) {
            return false;
        }
        int requiredLevel = this.requirements[id];
        int xpCost = this.costs[id];
        if (requiredLevel <= 0
                || xpCost <= 0
                || !isEnchantingTarget(target)
                || player.experienceLevel < Math.max(requiredLevel, xpCost) && !player.getAbilities().instabuild) {
            return false;
        }

        this.access.execute((level, blockPos) -> {
            ItemStack costTarget = costTarget(target);
            PoolModifierRules.ModifierPlan modifierPlan = modifierPlan();
            List<EnchantmentInstance> enchantments = EnchantingRoller.preview(
                    level.registryAccess(),
                    target,
                    EnchantingPowerRules.rollPower(this.requirements[id], costTarget, modifierStack(modifierPlan, id)),
                    id,
                    this.enchantmentSeed.get(),
                    optionEssenceStacks(modifierPlan, id),
                    optionBookStacks(modifierPlan, id)
            ).enchantments();

            if (enchantments.isEmpty()) {
                return;
            }
            if (!canApplyWithFusion(level.registryAccess(), target, enchantments)) {
                return;
            }

            player.onEnchantmentPerformed(target, xpCost);
            ItemStack enchanted = target.getItem().applyEnchantments(target, enchantments);
            FortunesTouchEnchantmentEvents.fuseFortunesTouch(level.registryAccess(), enchanted);
            this.enchantingSlots.setItem(TARGET_SLOT, enchanted);
            net.neoforged.neoforge.common.CommonHooks.onPlayerEnchantItem(player, enchanted, enchantments);

            if (!player.hasInfiniteMaterials()) {
                if (lapisCost > 0) {
                    lapis.consume(lapisCost, player);
                    if (lapis.isEmpty()) {
                        this.enchantingSlots.setItem(LAPIS_SLOT, ItemStack.EMPTY);
                    }
                }
                PoolModifierRules.consumeForOption(this.enchantingSlots, modifierPlan, id, player);
            }

            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, enchanted, xpCost);
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
                    || state.is(Blocks.ENCHANTING_TABLE) && BetterEnchantingConfig.takesOverEnchantingTable();
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
        } else if (isEnchantingTarget(stack)) {
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
        return BetterEnchantingConfig.enchantingLapisCost();
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

    private PoolModifierRules.ModifierPlan modifierPlan() {
        return PoolModifierRules.plan(this.enchantingSlots, FIRST_MODIFIER_SLOT, MODIFIER_SLOT_COUNT, this.enchantmentSeed.get());
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

    private static boolean canApplyWithFusion(RegistryAccess registryAccess, ItemStack target, List<EnchantmentInstance> enchantments) {
        ItemStack simulatedTarget = target.copy();
        ItemStack simulatedResult = simulatedTarget.getItem().applyEnchantments(simulatedTarget, enchantments);
        FortunesTouchEnchantmentEvents.fuseFortunesTouch(registryAccess, simulatedResult);
        return EnchantmentLimitRules.currentEnchantmentCount(simulatedResult) <= EnchantmentLimitRules.maxEnchantments(simulatedResult);
    }

    private static boolean isEnchantingTarget(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem().isEnchantable(stack) || stack.is(Items.ENCHANTED_BOOK));
    }

    private static ItemStack costTarget(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) ? new ItemStack(Items.BOOK) : stack;
    }

    private void clearPreviews() {
        for (int index = 0; index < 3; index++) {
            this.requirements[index] = 0;
            this.costs[index] = 0;
            this.enchantClue[index] = -1;
            this.levelClue[index] = -1;
            this.poolSizes[index] = 0;
        }
        for (int index = 0; index < 3; index++) {
            this.activeTagCounts[index] = 0;
            this.bookBoostCounts[index] = 0;
        }
        this.broadcastChanges();
    }
}
