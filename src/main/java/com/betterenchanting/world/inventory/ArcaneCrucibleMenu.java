package com.betterenchanting.world.inventory;

import com.mojang.datafixers.util.Pair;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.registry.ModBlocks;
import com.betterenchanting.registry.ModMenus;
import com.betterenchanting.registry.ModTags;
import com.betterenchanting.world.CrucibleRoller;
import com.betterenchanting.world.level.block.ArcaneCrucibleBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public class ArcaneCrucibleMenu extends AbstractContainerMenu {
    public static final int TARGET_SLOT = 0;
    public static final int LAPIS_SLOT = 1;
    public static final int FIRST_ESSENCE_SLOT = 2;
    public static final int ESSENCE_SLOT_COUNT = 3;
    public static final int FIRST_BOOK_SLOT = 5;
    public static final int BOOK_SLOT_COUNT = 2;
    public static final int CRUCIBLE_SLOT_COUNT = 7;

    private static final int PLAYER_INVENTORY_START = CRUCIBLE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final ResourceLocation EMPTY_LAPIS_SLOT = ResourceLocation.withDefaultNamespace("item/empty_slot_lapis_lazuli");

    private final Container crucibleSlots = new SimpleContainer(CRUCIBLE_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            ArcaneCrucibleMenu.this.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
    private final DataSlot enchantmentSeed = DataSlot.standalone();

    public final int[] costs = new int[]{0, 0, 0};
    public final int[] enchantClue = new int[]{-1, -1, -1};
    public final int[] levelClue = new int[]{-1, -1, -1};
    private final int[] poolSizes = new int[]{0, 0, 0};
    private final int[] summary = new int[]{0, 0, 0};

    public ArcaneCrucibleMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, BlockPos.ZERO);
    }

    public ArcaneCrucibleMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, buffer == null ? BlockPos.ZERO : buffer.readBlockPos());
    }

    public ArcaneCrucibleMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, BlockPos pos) {
        super(ModMenus.ARCANE_CRUCIBLE.get(), containerId);
        this.access = access;
        this.pos = pos;

        this.addSlot(new Slot(this.crucibleSlots, TARGET_SLOT, 18, 38) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.isEnchantable();
            }
        });
        this.addSlot(new Slot(this.crucibleSlots, LAPIS_SLOT, 18, 66) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAPIS_LAZULI);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_LAPIS_SLOT);
            }
        });

        for (int slot = 0; slot < ESSENCE_SLOT_COUNT; slot++) {
            this.addSlot(new Slot(this.crucibleSlots, FIRST_ESSENCE_SLOT + slot, 54 + slot * 22, 28) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModTags.Items.ESSENCES) || EssenceDefinitions.isEssence(stack);
                }
            });
        }

        for (int slot = 0; slot < BOOK_SLOT_COUNT; slot++) {
            this.addSlot(new Slot(this.crucibleSlots, FIRST_BOOK_SLOT + slot, 66 + slot * 22, 66) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.hasAnyEnchantments(stack);
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 38 + column * 18, 126 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 38 + column * 18, 184));
        }

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
        this.addDataSlot(DataSlot.shared(this.summary, 0));
        this.addDataSlot(DataSlot.shared(this.summary, 1));
        this.addDataSlot(DataSlot.shared(this.summary, 2));
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (inventory != this.crucibleSlots) {
            return;
        }

        ItemStack target = inventory.getItem(TARGET_SLOT);
        if (target.isEmpty() || !target.isEnchantable()) {
            clearPreviews();
            return;
        }

        this.access.execute((level, blockPos) -> {
            IdMap<Holder<Enchantment>> ids = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            int bookshelfPower = ArcaneCrucibleBlock.bookshelfPower(level, blockPos);
            this.random.setSeed((long) this.enchantmentSeed.get());

            List<ItemStack> essences = essenceStacks();
            List<ItemStack> books = bookStacks();
            CrucibleRoller.InputProfile profile = CrucibleRoller.profile(target, essences, books);
            this.summary[0] = profile.essenceTags().size();
            this.summary[1] = profile.bookBoostCount();
            this.summary[2] = profile.restricted() ? 1 : 0;

            for (int option = 0; option < 3; option++) {
                this.costs[option] = EnchantmentHelper.getEnchantmentCost(this.random, option, bookshelfPower, target);
                this.enchantClue[option] = -1;
                this.levelClue[option] = -1;
                this.poolSizes[option] = 0;
                if (this.costs[option] < option + 1) {
                    this.costs[option] = 0;
                }
                this.costs[option] = net.neoforged.neoforge.event.EventHooks.onEnchantmentLevelSet(
                        level,
                        blockPos,
                        option,
                        bookshelfPower,
                        target,
                        this.costs[option]
                );

                if (this.costs[option] > 0) {
                    CrucibleRoller.RollPreview preview = CrucibleRoller.preview(
                            level.registryAccess(),
                            target,
                            this.costs[option],
                            option,
                            this.enchantmentSeed.get(),
                            essences,
                            books
                    );
                    this.poolSizes[option] = preview.poolSize();
                    if (!preview.enchantments().isEmpty()) {
                        EnchantmentInstance clue = preview.enchantments().get(this.random.nextInt(preview.enchantments().size()));
                        this.enchantClue[option] = ids.getId(clue.enchantment);
                        this.levelClue[option] = clue.level;
                    }
                }
            }

            this.broadcastChanges();
        });
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= this.costs.length) {
            Util.logAndPauseIfInIde(player.getName() + " pressed invalid Arcane Crucible button id: " + id);
            return false;
        }

        ItemStack target = this.crucibleSlots.getItem(TARGET_SLOT);
        ItemStack lapis = this.crucibleSlots.getItem(LAPIS_SLOT);
        int lapisCost = id + 1;
        if ((lapis.isEmpty() || lapis.getCount() < lapisCost) && !player.hasInfiniteMaterials()) {
            return false;
        }
        if (this.costs[id] <= 0
                || target.isEmpty()
                || (player.experienceLevel < lapisCost || player.experienceLevel < this.costs[id]) && !player.getAbilities().instabuild) {
            return false;
        }

        this.access.execute((level, blockPos) -> {
            List<EnchantmentInstance> enchantments = CrucibleRoller.preview(
                    level.registryAccess(),
                    target,
                    this.costs[id],
                    id,
                    this.enchantmentSeed.get(),
                    essenceStacks(),
                    bookStacks()
            ).enchantments();

            if (enchantments.isEmpty()) {
                return;
            }

            player.onEnchantmentPerformed(target, lapisCost);
            ItemStack enchanted = target.getItem().applyEnchantments(target, enchantments);
            this.crucibleSlots.setItem(TARGET_SLOT, enchanted);
            net.neoforged.neoforge.common.CommonHooks.onPlayerEnchantItem(player, enchanted, enchantments);

            if (!player.hasInfiniteMaterials()) {
                lapis.consume(lapisCost, player);
                if (lapis.isEmpty()) {
                    this.crucibleSlots.setItem(LAPIS_SLOT, ItemStack.EMPTY);
                }
                consumeInputs(FIRST_ESSENCE_SLOT, ESSENCE_SLOT_COUNT, player);
                consumeInputs(FIRST_BOOK_SLOT, BOOK_SLOT_COUNT, player);
            }

            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, enchanted, lapisCost);
            }

            this.crucibleSlots.setChanged();
            this.enchantmentSeed.set(player.getEnchantmentSeed());
            this.slotsChanged(this.crucibleSlots);
            level.playSound(null, blockPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
        });
        return true;
    }

    private void consumeInputs(int firstSlot, int count, Player player) {
        for (int slot = firstSlot; slot < firstSlot + count; slot++) {
            ItemStack stack = this.crucibleSlots.getItem(slot);
            if (!stack.isEmpty()) {
                stack.consume(1, player);
                if (stack.isEmpty()) {
                    this.crucibleSlots.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.crucibleSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ARCANE_CRUCIBLE.get());
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
        if (index < CRUCIBLE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.LAPIS_LAZULI)) {
            if (!this.moveItemStackTo(stack, LAPIS_SLOT, LAPIS_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModTags.Items.ESSENCES) || EssenceDefinitions.isEssence(stack)) {
            if (!this.moveItemStackTo(stack, FIRST_ESSENCE_SLOT, FIRST_ESSENCE_SLOT + ESSENCE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.hasAnyEnchantments(stack)) {
            if (!this.moveItemStackTo(stack, FIRST_BOOK_SLOT, FIRST_BOOK_SLOT + BOOK_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.isEnchantable()) {
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

    public int getGoldCount() {
        ItemStack lapis = this.crucibleSlots.getItem(LAPIS_SLOT);
        return lapis.isEmpty() ? 0 : lapis.getCount();
    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed.get();
    }

    public int getPoolSize(int option) {
        return option < 0 || option >= this.poolSizes.length ? 0 : this.poolSizes[option];
    }

    public int getActiveTagCount() {
        return this.summary[0];
    }

    public int getBookBoostCount() {
        return this.summary[1];
    }

    public boolean isRestricted() {
        return this.summary[2] != 0;
    }

    public List<ItemStack> essenceStacks() {
        return stacks(FIRST_ESSENCE_SLOT, ESSENCE_SLOT_COUNT);
    }

    public List<ItemStack> bookStacks() {
        return stacks(FIRST_BOOK_SLOT, BOOK_SLOT_COUNT);
    }

    public CrucibleRoller.InputProfile clientProfile() {
        return CrucibleRoller.profile(this.crucibleSlots.getItem(TARGET_SLOT), essenceStacks(), bookStacks());
    }

    public BlockPos pos() {
        return this.pos;
    }

    private List<ItemStack> stacks(int firstSlot, int count) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = firstSlot; slot < firstSlot + count; slot++) {
            ItemStack stack = this.crucibleSlots.getItem(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private void clearPreviews() {
        for (int index = 0; index < 3; index++) {
            this.costs[index] = 0;
            this.enchantClue[index] = -1;
            this.levelClue[index] = -1;
            this.poolSizes[index] = 0;
        }
        this.summary[0] = 0;
        this.summary[1] = 0;
        this.summary[2] = 0;
        this.broadcastChanges();
    }
}
