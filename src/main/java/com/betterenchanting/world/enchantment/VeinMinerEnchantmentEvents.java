package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.registry.ModEnchantments;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class VeinMinerEnchantmentEvents {
    private static final ThreadLocal<Boolean> VEIN_MINING = ThreadLocal.withInitial(() -> false);

    private VeinMinerEnchantmentEvents() {
    }

    public static void veinMineConnectedBlocks(BlockDropsEvent event) {
        if (event.isCanceled() || VEIN_MINING.get() || !(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = event.getLevel();
        ItemStack tool = event.getTool();
        int veinMinerLevel = getVeinMinerLevel(level, tool);
        if (veinMinerLevel <= 0 || player.isSpectator() || !player.mayBuild()) {
            return;
        }

        BlockState originState = event.getState();
        if (originState.isAir() || originState.hasBlockEntity() || event.getBlockEntity() != null || originState.getDestroySpeed(level, event.getPos()) < 0.0F) {
            return;
        }

        VEIN_MINING.set(true);
        try {
            veinMine(level, player, event.getPos(), originState, veinMinerLevel * BetterEnchantingConfig.veinMinerConnectedBlocksPerLevel());
        } finally {
            VEIN_MINING.set(false);
        }
    }

    private static void veinMine(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState originState, int maxConnectedBlocks) {
        Queue<BlockPos> searchQueue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos start = origin.immutable();
        searchQueue.add(start);
        visited.add(start);

        int broken = 0;
        while (!searchQueue.isEmpty() && broken < maxConnectedBlocks) {
            BlockPos current = searchQueue.remove();
            for (Direction direction : Direction.values()) {
                if (broken >= maxConnectedBlocks) {
                    break;
                }

                BlockPos next = current.relative(direction).immutable();
                if (!visited.add(next)) {
                    continue;
                }

                BlockState nextState = level.getBlockState(next);
                if (!canVeinMine(level, player, next, nextState, originState) || getVeinMinerLevel(level, player.getMainHandItem()) <= 0) {
                    continue;
                }

                if (player.gameMode.destroyBlock(next)) {
                    broken++;
                    searchQueue.add(next);
                }
            }
        }
    }

    private static boolean canVeinMine(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockState originState) {
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos) || state.isAir() || state.hasBlockEntity()) {
            return false;
        }
        if (!state.is(originState.getBlock()) || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        return player.mayInteract(level, pos) && state.canHarvestBlock(level, pos, player);
    }

    private static int getVeinMinerLevel(Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || stack.isEmpty()) {
            return 0;
        }

        return serverLevel.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.VEIN_MINER)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
