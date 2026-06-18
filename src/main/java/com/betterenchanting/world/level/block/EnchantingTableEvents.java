package com.betterenchanting.world.level.block;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModBlocks;
import com.betterenchanting.world.inventory.EnhancedEnchantingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    private static final Component ARCANE_CRUCIBLE_TITLE = Component.translatable("container.betterenchanting.arcane_crucible");

    private EnchantingTableEvents() {
    }

    public static void openEssenceEnchantingTable(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getUseBlock().isFalse()) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean isCrucible = state.is(ModBlocks.ARCANE_CRUCIBLE.get());
        boolean isEnhancedTable = state.is(Blocks.ENCHANTING_TABLE) && EffectiveBalance.takesOverEnchantingTable();
        if ((!isCrucible && !isEnhancedTable) || shouldPreserveSneakItemUse(event.getEntity(), level, pos)) {
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
                            isCrucible ? ARCANE_CRUCIBLE_TITLE : ENCHANTING_TABLE_TITLE
                    ),
                    buffer -> buffer.writeBlockPos(pos)
            );
        }
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
