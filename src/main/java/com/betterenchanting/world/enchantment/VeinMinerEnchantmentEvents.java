package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.registry.ModEnchantments;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class VeinMinerEnchantmentEvents {
    private static final ThreadLocal<Boolean> VEIN_MINING = ThreadLocal.withInitial(() -> false);
    private static final int MIN_RADIUS = 2;
    private static final int MAX_RADIUS = 6;

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
        if (originState.isAir()
                || !originState.is(Tags.Blocks.ORES)
                || originState.hasBlockEntity()
                || event.getBlockEntity() != null
                || originState.getDestroySpeed(level, event.getPos()) < 0.0F) {
            return;
        }

        VeinMinerMode mode = VeinMinerModeState.current(player);
        int maxBlocks = veinMinerLevel * EffectiveBalance.veinMinerConnectedBlocksPerLevel();

        VEIN_MINING.set(true);
        try {
            if (mode.connectedSearch()) {
                veinMineConnected(level, player, event.getPos(), originState, maxBlocks, mode);
            } else {
                veinMineRadius(level, player, event.getPos(), originState, maxBlocks, veinMinerLevel, mode);
            }
        } finally {
            VEIN_MINING.set(false);
        }
    }

    private static void veinMineConnected(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState originState, int maxBlocks, VeinMinerMode mode) {
        Queue<BlockPos> searchQueue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos start = origin.immutable();
        searchQueue.add(start);
        visited.add(start);

        int broken = 0;
        while (!searchQueue.isEmpty() && broken < maxBlocks) {
            BlockPos current = searchQueue.remove();
            for (Direction direction : Direction.values()) {
                if (broken >= maxBlocks) {
                    break;
                }

                BlockPos next = current.relative(direction).immutable();
                if (!visited.add(next)) {
                    continue;
                }

                BlockState nextState = level.getBlockState(next);
                if (!canVeinMine(level, player, next, nextState, originState, mode) || getVeinMinerLevel(level, player.getMainHandItem()) <= 0) {
                    continue;
                }

                if (player.gameMode.destroyBlock(next)) {
                    broken++;
                    searchQueue.add(next);
                }
            }
        }
    }

    private static void veinMineRadius(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState originState, int maxBlocks, int veinMinerLevel, VeinMinerMode mode) {
        int radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, veinMinerLevel + 1));
        List<BlockPos> candidates = radiusCandidates(level, player, origin, originState, radius, mode);
        int broken = 0;
        for (BlockPos pos : candidates) {
            if (broken >= maxBlocks || getVeinMinerLevel(level, player.getMainHandItem()) <= 0) {
                break;
            }

            BlockState state = level.getBlockState(pos);
            if (!canVeinMine(level, player, pos, state, originState, mode)) {
                continue;
            }

            if (player.gameMode.destroyBlock(pos)) {
                broken++;
            }
        }
    }

    private static List<BlockPos> radiusCandidates(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState originState, int radius, VeinMinerMode mode) {
        int radiusSquared = radius * radius;
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if ((x == 0 && y == 0 && z == 0) || x * x + y * y + z * z > radiusSquared) {
                        continue;
                    }

                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.isLoaded(cursor) || !level.getWorldBorder().isWithinBounds(cursor)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(cursor);
                    if (canVeinMine(level, player, cursor, state, originState, mode)) {
                        candidates.add(cursor.immutable());
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingInt(pos -> distanceSquared(origin, pos)));
        return candidates;
    }

    private static boolean canVeinMine(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockState originState, VeinMinerMode mode) {
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos) || state.isAir() || state.hasBlockEntity()) {
            return false;
        }
        if (!state.is(Tags.Blocks.ORES) || (mode.matchingOre() && !state.is(originState.getBlock())) || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        return player.mayInteract(level, pos) && state.canHarvestBlock(level, pos, player);
    }

    private static int distanceSquared(BlockPos first, BlockPos second) {
        int x = first.getX() - second.getX();
        int y = first.getY() - second.getY();
        int z = first.getZ() - second.getZ();
        return x * x + y * y + z * z;
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
