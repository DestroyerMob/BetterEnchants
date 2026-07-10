package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.network.ChainedMiningAnimationPayload;
import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModTags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class VeinMinerEnchantmentEvents {
    private static final ThreadLocal<Boolean> VEIN_MINING = ThreadLocal.withInitial(() -> false);
    private static final Map<MinecraftServer, Map<UUID, ArrayDeque<VeinBreakTask>>> PENDING_BREAKS = new WeakHashMap<>();
    private static final int MIN_RADIUS = 2;
    private static final int MAX_RADIUS = 6;

    private VeinMinerEnchantmentEvents() {
    }

    static boolean isVeinMining() {
        return VEIN_MINING.get();
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
                || !originState.is(ModTags.Blocks.ORES)
                || originState.hasBlockEntity()
                || event.getBlockEntity() != null
                || originState.getDestroySpeed(level, event.getPos()) < 0.0F) {
            return;
        }

        VeinMinerMode mode = VeinMinerModeState.current(player);
        Set<TagKey<Block>> oreFamilyTags = TaggedBlockFamilies.oreFamilies(originState);
        int maxBlocks = veinMinerLevel * EffectiveBalance.veinMinerConnectedBlocksPerLevel();

        List<BlockPos> candidates = mode.connectedSearch()
                ? connectedCandidates(level, player, event.getPos(), originState, oreFamilyTags, maxBlocks, mode)
                : radiusCandidates(
                        level,
                        player,
                        event.getPos(),
                        originState,
                        oreFamilyTags,
                        Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, veinMinerLevel + 1)),
                        mode
                );
        if (candidates.size() > maxBlocks) {
            candidates = new ArrayList<>(candidates.subList(0, maxBlocks));
        }
        if (!candidates.isEmpty()) {
            queueBreaks(level, player, originState, oreFamilyTags, mode, candidates);
        }
    }

    private static List<BlockPos> connectedCandidates(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            BlockState originState,
            Set<TagKey<Block>> oreFamilyTags,
            int maxBlocks,
            VeinMinerMode mode
    ) {
        Queue<BlockPos> searchQueue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos start = origin.immutable();
        searchQueue.add(start);
        visited.add(start);

        while (!searchQueue.isEmpty() && candidates.size() < maxBlocks) {
            BlockPos current = searchQueue.remove();
            for (Direction direction : Direction.values()) {
                if (candidates.size() >= maxBlocks) {
                    break;
                }

                BlockPos next = current.relative(direction).immutable();
                if (!visited.add(next)) {
                    continue;
                }

                BlockState nextState = level.getBlockState(next);
                if (!canVeinMine(level, player, next, nextState, originState, oreFamilyTags, mode)) {
                    continue;
                }

                candidates.add(next);
                searchQueue.add(next);
            }
        }
        return candidates;
    }

    private static List<BlockPos> radiusCandidates(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            BlockState originState,
            Set<TagKey<Block>> oreFamilyTags,
            int radius,
            VeinMinerMode mode
    ) {
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
                    if (canVeinMine(level, player, cursor, state, originState, oreFamilyTags, mode)) {
                        candidates.add(cursor.immutable());
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingInt(pos -> distanceSquared(origin, pos)));
        return candidates;
    }

    private static void queueBreaks(
            ServerLevel level,
            ServerPlayer player,
            BlockState originState,
            Set<TagKey<Block>> oreFamilyTags,
            VeinMinerMode mode,
            List<BlockPos> candidates
    ) {
        Map<UUID, ArrayDeque<VeinBreakTask>> playerTasks = PENDING_BREAKS
                .computeIfAbsent(level.getServer(), ignored -> new java.util.HashMap<>());
        ArrayDeque<VeinBreakTask> tasks = playerTasks.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        boolean startingCascade = tasks.isEmpty();
        tasks.addLast(new VeinBreakTask(
                level.dimension(),
                originState,
                oreFamilyTags,
                mode,
                new ArrayDeque<>(candidates),
                level.getGameTime() + 1L
        ));
        if (startingCascade) {
            setAnimationSuppressed(player, true);
        }
    }

    public static void tickPendingBreaks(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Map<UUID, ArrayDeque<VeinBreakTask>> playerTasks = PENDING_BREAKS.get(server);
        if (playerTasks == null || playerTasks.isEmpty()) {
            return;
        }

        var iterator = playerTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ArrayDeque<VeinBreakTask>> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ArrayDeque<VeinBreakTask> tasks = entry.getValue();
            if (player == null || tasks.isEmpty()) {
                iterator.remove();
                continue;
            }

            if (tasks.peekFirst().tick(server, player)) {
                tasks.removeFirst();
            }
            if (tasks.isEmpty()) {
                setAnimationSuppressed(player, false);
                iterator.remove();
            }
        }

        if (playerTasks.isEmpty()) {
            PENDING_BREAKS.remove(server);
        }
    }

    private static void setAnimationSuppressed(ServerPlayer player, boolean suppressed) {
        PacketDistributor.sendToPlayer(
                player,
                new ChainedMiningAnimationPayload(ChainedMiningAnimationPayload.VEIN_MINER, suppressed)
        );
    }

    private static boolean canVeinMine(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            BlockState originState,
            Set<TagKey<Block>> oreFamilyTags,
            VeinMinerMode mode
    ) {
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos) || state.isAir() || state.hasBlockEntity()) {
            return false;
        }
        if (!state.is(ModTags.Blocks.ORES)
                || (mode.matchingOre() && !TaggedBlockFamilies.matches(originState, state, oreFamilyTags))
                || state.getDestroySpeed(level, pos) < 0.0F) {
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

    private static final class VeinBreakTask {
        private final ResourceKey<Level> dimension;
        private final BlockState originState;
        private final Set<TagKey<Block>> oreFamilyTags;
        private final VeinMinerMode mode;
        private final ArrayDeque<BlockPos> positions;
        private long nextBreakTime;

        private VeinBreakTask(
                ResourceKey<Level> dimension,
                BlockState originState,
                Set<TagKey<Block>> oreFamilyTags,
                VeinMinerMode mode,
                ArrayDeque<BlockPos> positions,
                long nextBreakTime
        ) {
            this.dimension = dimension;
            this.originState = originState;
            this.oreFamilyTags = Set.copyOf(oreFamilyTags);
            this.mode = mode;
            this.positions = positions;
            this.nextBreakTime = nextBreakTime;
        }

        private boolean tick(MinecraftServer server, ServerPlayer player) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null
                    || player.level() != level
                    || player.isSpectator()
                    || !player.mayBuild()
                    || getVeinMinerLevel(level, player.getMainHandItem()) <= 0) {
                return true;
            }
            if (level.getGameTime() < nextBreakTime) {
                return false;
            }

            while (!positions.isEmpty()) {
                BlockPos pos = positions.removeFirst();
                BlockState state = level.getBlockState(pos);
                if (!canVeinMine(level, player, pos, state, originState, oreFamilyTags, mode)) {
                    continue;
                }

                boolean destroyed;
                VEIN_MINING.set(true);
                try {
                    destroyed = player.gameMode.destroyBlock(pos);
                } finally {
                    VEIN_MINING.set(false);
                }
                if (!destroyed) {
                    continue;
                }

                level.levelEvent(2001, pos, Block.getId(state));
                nextBreakTime = level.getGameTime() + 1L;
                return positions.isEmpty();
            }
            return true;
        }
    }
}
