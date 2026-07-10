package com.betterenchanting.world.inventory;

import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.registry.ModBlocks;
import com.betterenchanting.registry.ModMenus;
import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class AttunementPedestalMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = AttunementPedestalBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_END = PLAYER_END + 9;

    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public AttunementPedestalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(
                containerId,
                inventory,
                new SimpleContainer(MACHINE_SLOTS),
                new SimpleContainerData(AttunementPedestalBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL
        );
        if (buffer != null) {
            buffer.readBlockPos();
        }
    }

    public AttunementPedestalMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        this(
                containerId,
                inventory,
                container,
                data,
                container instanceof AttunementPedestalBlockEntity pedestal && pedestal.getLevel() != null
                        ? ContainerLevelAccess.create(pedestal.getLevel(), pedestal.getBlockPos())
                        : ContainerLevelAccess.NULL
        );
    }

    private AttunementPedestalMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data,
            ContainerLevelAccess access
    ) {
        super(ModMenus.ATTUNEMENT_PEDESTAL.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, AttunementPedestalBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.access = access;

        this.addSlot(new Slot(container, AttunementPedestalBlockEntity.TARGET_SLOT, 26, 40) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
            }
        });
        this.addSlot(new Slot(container, AttunementPedestalBlockEntity.ESSENCE_SLOT, 62, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EssenceDefinitions.isEssence(stack);
            }
        });
        this.addSlot(new Slot(container, AttunementPedestalBlockEntity.CATALYST_SLOT, 82, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.NETHER_STAR);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 96 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 154));
        }

        this.addDataSlots(data);
        container.startOpen(inventory.player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0) {
            return false;
        }
        return !(this.container instanceof AttunementPedestalBlockEntity pedestal) || pedestal.tryUpgrade(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ATTUNEMENT_PEDESTAL.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()) {
            if (!this.moveItemStackTo(stack, AttunementPedestalBlockEntity.TARGET_SLOT, AttunementPedestalBlockEntity.TARGET_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (EssenceDefinitions.isEssence(stack)) {
            if (!this.moveItemStackTo(stack, AttunementPedestalBlockEntity.ESSENCE_SLOT, AttunementPedestalBlockEntity.ESSENCE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.NETHER_STAR)) {
            if (!this.moveItemStackTo(stack, AttunementPedestalBlockEntity.CATALYST_SLOT, AttunementPedestalBlockEntity.CATALYST_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_END) {
            if (!this.moveItemStackTo(stack, PLAYER_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    public int currentLevel() {
        return this.data.get(0);
    }

    public int nextLevel() {
        return this.data.get(1);
    }

    public int essenceCost() {
        return this.data.get(2);
    }

    public int requiredPower() {
        return this.data.get(3);
    }

    public int availablePower() {
        return this.data.get(4);
    }

    public int flags() {
        return this.data.get(5);
    }

    public int selectedPartIndex() {
        return this.data.get(7);
    }

    public Holder<Enchantment> selectedEnchantment(RegistryAccess registries) {
        int id = this.data.get(6);
        return id < 0 ? null : registries.registryOrThrow(Registries.ENCHANTMENT).asHolderIdMap().byId(id);
    }

    public boolean hasFlag(int flag) {
        return (this.flags() & flag) != 0;
    }

    public boolean canUpgrade() {
        return this.hasFlag(AttunementPedestalBlockEntity.FLAG_VALID_SELECTION)
                && this.hasFlag(AttunementPedestalBlockEntity.FLAG_ENOUGH_ESSENCE)
                && this.hasFlag(AttunementPedestalBlockEntity.FLAG_LINKED_TABLE)
                && this.hasFlag(AttunementPedestalBlockEntity.FLAG_ENOUGH_POWER)
                && this.hasFlag(AttunementPedestalBlockEntity.FLAG_HAS_CATALYST);
    }
}
