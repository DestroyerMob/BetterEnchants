package com.betterenchanting.world.inventory;

import com.betterenchanting.data.EssenceDistillationRecipes;
import com.betterenchanting.registry.ModBlocks;
import com.betterenchanting.registry.ModMenus;
import com.betterenchanting.world.level.block.entity.ArcaneCrucibleBlockEntity;
import net.minecraft.core.BlockPos;
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

public final class ArcaneCrucibleMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ArcaneCrucibleBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public ArcaneCrucibleMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(2),
                ContainerLevelAccess.NULL
        );
        if (buffer != null) {
            buffer.readBlockPos();
        }
    }

    public ArcaneCrucibleMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        this(
                containerId,
                playerInventory,
                container,
                data,
                container instanceof ArcaneCrucibleBlockEntity crucible && crucible.getLevel() != null
                        ? ContainerLevelAccess.create(crucible.getLevel(), crucible.getBlockPos())
                        : ContainerLevelAccess.NULL
        );
    }

    private ArcaneCrucibleMenu(
            int containerId,
            Inventory playerInventory,
            Container container,
            ContainerData data,
            ContainerLevelAccess access
    ) {
        super(ModMenus.ARCANE_CRUCIBLE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        this.access = access;

        this.addSlot(new Slot(container, ArcaneCrucibleBlockEntity.MEDIUM_SLOT, 26, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EssenceDistillationRecipes.isMedium(stack);
            }
        });
        for (int index = 0; index < ArcaneCrucibleBlockEntity.CATALYST_SLOT_COUNT; index++) {
            this.addSlot(new Slot(container, ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + index, 62 + index * 20, 40) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return EssenceDistillationRecipes.isCatalyst(stack);
                }
            });
        }
        this.addSlot(new Slot(container, ArcaneCrucibleBlockEntity.OUTPUT_SLOT, 134, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 96 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 154));
        }

        this.addDataSlots(data);
        container.startOpen(playerInventory.player);
    }

    public int progressWidth(int width) {
        int max = this.data.get(1);
        return max <= 0 ? 0 : Math.min(width, this.data.get(0) * width / max);
    }

    public boolean isDistilling() {
        return this.data.get(0) > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.ARCANE_CRUCIBLE.get());
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
        if (index < MACHINE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (EssenceDistillationRecipes.isMedium(stack)) {
            if (!this.moveItemStackTo(
                    stack,
                    ArcaneCrucibleBlockEntity.MEDIUM_SLOT,
                    ArcaneCrucibleBlockEntity.MEDIUM_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (EssenceDistillationRecipes.isCatalyst(stack)) {
            if (!this.moveItemStackTo(
                    stack,
                    ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT,
                    ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + ArcaneCrucibleBlockEntity.CATALYST_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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
}
