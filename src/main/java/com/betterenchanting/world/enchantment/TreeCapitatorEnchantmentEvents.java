package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModTags;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class TreeCapitatorEnchantmentEvents {
    private static final ThreadLocal<Boolean> TREE_CAPITATING = ThreadLocal.withInitial(() -> false);

    private TreeCapitatorEnchantmentEvents() {
    }

    public static void chopTree(BlockDropsEvent event) {
        if (event.isCanceled() || TREE_CAPITATING.get() || !(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = event.getLevel();
        ItemStack tool = event.getTool();
        if (!hasTreeCapitatorAxe(level, tool) || player.isSpectator() || !player.mayBuild()) {
            return;
        }

        BlockState originState = event.getState();
        if (!isTreeLog(originState)
                || originState.hasBlockEntity()
                || event.getBlockEntity() != null
                || originState.getDestroySpeed(level, event.getPos()) < 0.0F) {
            return;
        }

        Set<BlockPos> treeLogs = collectConnectedLogs(level, event.getPos(), originState);
        if (treeLogs.size() <= 1 || !hasEnoughNaturalLeaves(level, treeLogs)) {
            return;
        }

        TREE_CAPITATING.set(true);
        try {
            chopConnectedLogs(level, player, event.getPos(), originState, treeLogs);
        } finally {
            TREE_CAPITATING.set(false);
        }
    }

    private static Set<BlockPos> collectConnectedLogs(ServerLevel level, BlockPos origin, BlockState originState) {
        int maxLogs = BetterEnchantingConfig.treeCapitatorMaxLogs();
        Queue<BlockPos> searchQueue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> logs = new LinkedHashSet<>();
        BlockPos start = origin.immutable();
        searchQueue.add(start);
        visited.add(start);
        logs.add(start);

        while (!searchQueue.isEmpty() && logs.size() < maxLogs) {
            BlockPos current = searchQueue.remove();
            for (int xOffset = -1; xOffset <= 1 && logs.size() < maxLogs; xOffset++) {
                for (int yOffset = -1; yOffset <= 1 && logs.size() < maxLogs; yOffset++) {
                    for (int zOffset = -1; zOffset <= 1 && logs.size() < maxLogs; zOffset++) {
                        if (xOffset == 0 && yOffset == 0 && zOffset == 0) {
                            continue;
                        }

                        BlockPos next = current.offset(xOffset, yOffset, zOffset).immutable();
                        if (!visited.add(next) || !isMatchingLog(level, next, originState)) {
                            continue;
                        }

                        logs.add(next);
                        searchQueue.add(next);
                    }
                }
            }
        }

        return logs;
    }

    private static boolean hasEnoughNaturalLeaves(ServerLevel level, Set<BlockPos> logs) {
        int requiredLeaves = BetterEnchantingConfig.treeCapitatorMinNaturalLeaves();
        if (requiredLeaves <= 0) {
            return true;
        }

        int scanRadius = BetterEnchantingConfig.treeCapitatorLeafScanRadius();
        Set<BlockPos> scanned = new HashSet<>();
        int naturalLeaves = 0;

        for (BlockPos log : logs) {
            for (int xOffset = -scanRadius; xOffset <= scanRadius; xOffset++) {
                for (int yOffset = -scanRadius; yOffset <= scanRadius; yOffset++) {
                    for (int zOffset = -scanRadius; zOffset <= scanRadius; zOffset++) {
                        BlockPos leafPos = log.offset(xOffset, yOffset, zOffset).immutable();
                        if (!scanned.add(leafPos) || !level.isLoaded(leafPos)) {
                            continue;
                        }

                        if (isNaturalLeaf(level.getBlockState(leafPos)) && ++naturalLeaves >= requiredLeaves) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static void chopConnectedLogs(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState originState, Set<BlockPos> logs) {
        for (BlockPos log : logs) {
            if (log.equals(origin)) {
                continue;
            }
            if (!hasTreeCapitatorAxe(level, player.getMainHandItem())) {
                return;
            }
            if (canChopLog(level, player, log, originState)) {
                player.gameMode.destroyBlock(log);
            }
        }
    }

    private static boolean canChopLog(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState originState) {
        return isMatchingLog(level, pos, originState)
                && player.mayInteract(level, pos)
                && level.getBlockState(pos).canHarvestBlock(level, pos, player);
    }

    private static boolean isMatchingLog(ServerLevel level, BlockPos pos, BlockState originState) {
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return state.is(originState.getBlock())
                && isTreeLog(state)
                && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static boolean isTreeLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private static boolean isNaturalLeaf(BlockState state) {
        if (!state.is(BlockTags.LEAVES)) {
            return false;
        }
        if (!state.hasProperty(LeavesBlock.PERSISTENT)) {
            return true;
        }
        return !state.getValue(LeavesBlock.PERSISTENT);
    }

    private static boolean hasTreeCapitatorAxe(Level level, ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.Items.TOOL_AXES) && treeCapitatorLevel(level, stack) > 0;
    }

    private static int treeCapitatorLevel(Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || stack.isEmpty()) {
            return 0;
        }

        return serverLevel.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.TREE_CAPITATOR)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
