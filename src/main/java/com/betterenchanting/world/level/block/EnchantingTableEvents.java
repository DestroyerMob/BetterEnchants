package com.betterenchanting.world.level.block;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.world.inventory.EnhancedEnchantingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class EnchantingTableEvents {
    private static final Component ENCHANTING_TABLE_TITLE = Component.translatable("container.betterenchanting.enchanting_table");

    private EnchantingTableEvents() {
    }

    public static void openEssenceEnchantingTable(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getUseBlock().isFalse()) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean isEnhancedTable = state.is(Blocks.ENCHANTING_TABLE)
                && EffectiveBalance.takesOverEnchantingTable();
        if (!isEnhancedTable) {
            return;
        }

        if (BetterEnchantingConfig.usesInteractiveEnchanting()) {
            handleInteractiveUse(event, level, pos);
            return;
        }
        if (shouldPreserveSneakItemUse(event.getEntity(), level, pos)) {
            return;
        }

        event.setCancellationResult(level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        event.setCanceled(true);
        if (!level.isClientSide) {
            event.getEntity().openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, player) -> new EnhancedEnchantingMenu(
                                    containerId,
                                    inventory,
                                    ContainerLevelAccess.create(level, pos),
                                    pos
                            ),
                            ENCHANTING_TABLE_TITLE
                    ),
                    buffer -> buffer.writeBlockPos(pos)
            );
        }
    }

    private static void handleInteractiveUse(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos pos) {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        boolean openFallback = held.isEmpty() && player.isSecondaryUseActive();

        event.setCancellationResult(level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        event.setCanceled(true);
        if (level.isClientSide) {
            return;
        }
        if (openFallback) {
            openMenu(player, level, pos);
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof EnchantingTableStorage storage)) {
            return;
        }

        SimpleContainer inventory = storage.betterenchanting$getEnchantingInventory();
        boolean inserted = false;
        if (!held.isEmpty() && player.isSecondaryUseActive() && EnhancedEnchantingMenu.acceptsModifier(held)) {
            for (int index = 0; index < EnhancedEnchantingMenu.MODIFIER_SLOT_COUNT && !inserted; index++) {
                inserted = InWorldMachineInteraction.insert(
                        inventory,
                        EnhancedEnchantingMenu.FIRST_MODIFIER_SLOT + index,
                        player,
                        held,
                        1
                );
            }
        } else if (EnhancedEnchantingMenu.acceptsReagent(held)) {
            inserted = InWorldMachineInteraction.insert(
                    inventory,
                    EnhancedEnchantingMenu.REAGENT_SLOT,
                    player,
                    held,
                    held.getMaxStackSize()
            );
        } else if (EnhancedEnchantingMenu.acceptsTarget(held)) {
            inserted = InWorldMachineInteraction.insert(
                    inventory,
                    EnhancedEnchantingMenu.TARGET_SLOT,
                    player,
                    held,
                    1
            );
        }

        if (inserted) {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.65F, 1.2F);
        } else if (held.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.betterenchanting.interactive.instructions"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.betterenchanting.interactive.cannot_insert"), true);
        }
    }

    private static void openMenu(Player player, Level level, BlockPos pos) {
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) -> new EnhancedEnchantingMenu(
                                containerId,
                                inventory,
                                ContainerLevelAccess.create(level, pos),
                                pos
                        ),
                        ENCHANTING_TABLE_TITLE
                ),
                buffer -> buffer.writeBlockPos(pos)
        );
    }

    private static boolean shouldPreserveSneakItemUse(Player player, Level level, BlockPos pos) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean hasHeldItem = !mainHand.isEmpty() || !offHand.isEmpty();
        return player.isSecondaryUseActive()
                && hasHeldItem
                && !(mainHand.doesSneakBypassUse(level, pos, player) && offHand.doesSneakBypassUse(level, pos, player));
    }
}
