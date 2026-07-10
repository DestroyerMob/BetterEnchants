package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.network.ChainedMiningAnimationPayload;
import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModTags;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TreeCapitatorEnchantmentEvents {
    private static final ThreadLocal<Boolean> TREE_CAPITATING = ThreadLocal.withInitial(() -> false);
    private static final Map<MinecraftServer, Map<UUID, ArrayDeque<ChopTask>>> PENDING_CHOPS = new WeakHashMap<>();

    private TreeCapitatorEnchantmentEvents() {
    }

    public static void chopTree(BlockDropsEvent event) {
        if (event.isCanceled() || TREE_CAPITATING.get() || !(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = event.getLevel();
        ItemStack tool = event.getTool();
        int treeCapitatorLevel = treeCapitatorLevel(level, tool);
        if (treeCapitatorLevel <= 0 || !tool.is(ModTags.Items.TOOL_AXES) || player.isSpectator() || !player.mayBuild()) {
            return;
        }

        BlockState originState = event.getState();
        if (!isTreeLog(originState)
                || originState.hasBlockEntity()
                || event.getBlockEntity() != null
                || originState.getDestroySpeed(level, event.getPos()) < 0.0F) {
            return;
        }

        Set<TagKey<Block>> logFamilyTags = TaggedBlockFamilies.logFamilies(originState);
        Set<BlockPos> treeLogs = collectConnectedLogs(level, event.getPos(), originState, logFamilyTags);
        if (treeLogs.size() <= 1 || !hasEnoughNaturalLeaves(level, treeLogs)) {
            return;
        }
        Optional<ReplantPlan> replantPlan = treeCapitatorLevel >= 2
                ? createReplantPlan(event.getPos(), originState, treeLogs)
                : Optional.empty();

        queueChop(level, player, event.getPos(), originState, logFamilyTags, treeLogs, replantPlan);
    }

    private static Set<BlockPos> collectConnectedLogs(
            ServerLevel level,
            BlockPos origin,
            BlockState originState,
            Set<TagKey<Block>> logFamilyTags
    ) {
        int maxLogs = EffectiveBalance.treeCapitatorMaxLogs();
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
                        if (!visited.add(next) || !isMatchingLog(level, next, originState, logFamilyTags)) {
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
        int requiredLeaves = EffectiveBalance.treeCapitatorMinNaturalLeaves();
        if (requiredLeaves <= 0) {
            return true;
        }

        int scanRadius = EffectiveBalance.treeCapitatorLeafScanRadius();
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

    private static void queueChop(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            BlockState originState,
            Set<TagKey<Block>> logFamilyTags,
            Set<BlockPos> logs,
            Optional<ReplantPlan> replantPlan
    ) {
        ArrayDeque<BlockPos> remainingLogs = new ArrayDeque<>(logs);
        remainingLogs.remove(origin);
        if (remainingLogs.isEmpty()) {
            return;
        }

        Map<UUID, ArrayDeque<ChopTask>> playerTasks = PENDING_CHOPS
                .computeIfAbsent(level.getServer(), ignored -> new java.util.HashMap<>());
        ArrayDeque<ChopTask> tasks = playerTasks.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        boolean startingCascade = tasks.isEmpty();
        tasks.addLast(new ChopTask(
                level.dimension(),
                originState,
                logFamilyTags,
                remainingLogs,
                replantPlan,
                level.getGameTime() + 1L
        ));
        if (startingCascade) {
            setAnimationSuppressed(player, true);
        }
    }

    public static void tickPendingChops(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Map<UUID, ArrayDeque<ChopTask>> playerTasks = PENDING_CHOPS.get(server);
        if (playerTasks == null || playerTasks.isEmpty()) {
            return;
        }

        var iterator = playerTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ArrayDeque<ChopTask>> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ArrayDeque<ChopTask> tasks = entry.getValue();
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
            PENDING_CHOPS.remove(server);
        }
    }

    private static void setAnimationSuppressed(ServerPlayer player, boolean suppressed) {
        PacketDistributor.sendToPlayer(
                player,
                new ChainedMiningAnimationPayload(ChainedMiningAnimationPayload.TREE_CAPITATOR, suppressed)
        );
    }

    private static boolean canChopLog(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState originState,
            Set<TagKey<Block>> logFamilyTags
    ) {
        return isMatchingLog(level, pos, originState, logFamilyTags)
                && player.mayInteract(level, pos)
                && level.getBlockState(pos).canHarvestBlock(level, pos, player);
    }

    private static boolean isMatchingLog(
            ServerLevel level,
            BlockPos pos,
            BlockState originState,
            Set<TagKey<Block>> logFamilyTags
    ) {
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return isTreeLog(state)
                && TaggedBlockFamilies.matches(originState, state, logFamilyTags)
                && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static boolean isTreeLog(BlockState state) {
        return state.is(ModTags.Blocks.TREE_LOGS);
    }

    private static boolean isNaturalLeaf(BlockState state) {
        if (!state.is(ModTags.Blocks.TREE_LEAVES)) {
            return false;
        }
        if (!state.hasProperty(LeavesBlock.PERSISTENT)) {
            return true;
        }
        return !state.getValue(LeavesBlock.PERSISTENT);
    }

    private static Optional<ReplantPlan> createReplantPlan(BlockPos origin, BlockState originState, Set<BlockPos> logs) {
        Optional<Block> sapling = saplingFor(originState);
        if (sapling.isEmpty()) {
            return Optional.empty();
        }

        List<TreeReplantPlanner.Position> positions = TreeReplantPlanner.plantingPositions(
                logs.stream().map(TreeCapitatorEnchantmentEvents::plannerPosition).toList(),
                plannerPosition(origin)
        );
        return Optional.of(new ReplantPlan(
                sapling.get(),
                positions.stream().map(position -> new BlockPos(position.x(), position.y(), position.z())).toList()
        ));
    }

    private static TreeReplantPlanner.Position plannerPosition(BlockPos pos) {
        return new TreeReplantPlanner.Position(pos.getX(), pos.getY(), pos.getZ());
    }

    private static Optional<Block> saplingFor(BlockState logState) {
        ResourceLocation logId = BuiltInRegistries.BLOCK.getKey(logState.getBlock());
        String family = logId.getPath();
        if (family.startsWith("stripped_")) {
            family = family.substring("stripped_".length());
        }
        for (String suffix : List.of("_log", "_wood", "_stem", "_hyphae")) {
            if (family.endsWith(suffix)) {
                family = family.substring(0, family.length() - suffix.length());
                break;
            }
        }
        if (family.isEmpty()) {
            return Optional.empty();
        }

        for (String suffix : List.of("_sapling", "_propagule")) {
            ResourceLocation candidateId = ResourceLocation.fromNamespaceAndPath(logId.getNamespace(), family + suffix);
            Optional<Block> candidate = BuiltInRegistries.BLOCK.getOptional(candidateId);
            if (candidate.isPresent() && candidate.get() != Blocks.AIR) {
                return candidate;
            }
        }
        return Optional.empty();
    }

    private static void replant(ServerLevel level, ServerPlayer player, ReplantPlan plan) {
        BlockState saplingState = plan.sapling().defaultBlockState();
        boolean allPositionsAvailable = plan.positions().stream().allMatch(pos ->
                level.isLoaded(pos)
                        && level.getWorldBorder().isWithinBounds(pos)
                        && player.mayInteract(level, pos)
                        && level.getBlockState(pos).canBeReplaced()
                        && saplingState.canSurvive(level, pos)
        );
        if (!allPositionsAvailable) {
            return;
        }

        for (BlockPos pos : plan.positions()) {
            level.setBlock(pos, saplingState, Block.UPDATE_ALL);
        }
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

    private record ReplantPlan(Block sapling, List<BlockPos> positions) {
        private ReplantPlan {
            positions = List.copyOf(positions);
        }
    }

    private static final class ChopTask {
        private final ResourceKey<Level> dimension;
        private final BlockState originState;
        private final Set<TagKey<Block>> logFamilyTags;
        private final ArrayDeque<BlockPos> logs;
        private final Optional<ReplantPlan> replantPlan;
        private long nextBreakTime;

        private ChopTask(
                ResourceKey<Level> dimension,
                BlockState originState,
                Set<TagKey<Block>> logFamilyTags,
                ArrayDeque<BlockPos> logs,
                Optional<ReplantPlan> replantPlan,
                long nextBreakTime
        ) {
            this.dimension = dimension;
            this.originState = originState;
            this.logFamilyTags = Set.copyOf(logFamilyTags);
            this.logs = logs;
            this.replantPlan = replantPlan;
            this.nextBreakTime = nextBreakTime;
        }

        private boolean tick(MinecraftServer server, ServerPlayer player) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null
                    || player.level() != level
                    || player.isSpectator()
                    || !player.mayBuild()
                    || !hasTreeCapitatorAxe(level, player.getMainHandItem())) {
                return true;
            }
            if (level.getGameTime() < nextBreakTime) {
                return false;
            }

            while (!logs.isEmpty()) {
                BlockPos pos = logs.removeFirst();
                if (!canChopLog(level, player, pos, originState, logFamilyTags)) {
                    continue;
                }

                BlockState state = level.getBlockState(pos);
                boolean destroyed;
                TREE_CAPITATING.set(true);
                try {
                    destroyed = player.gameMode.destroyBlock(pos);
                } finally {
                    TREE_CAPITATING.set(false);
                }
                if (!destroyed) {
                    continue;
                }

                level.levelEvent(2001, pos, Block.getId(state));
                nextBreakTime = level.getGameTime() + 1L;
                if (logs.isEmpty()) {
                    replantPlan.ifPresent(plan -> replant(level, player, plan));
                    return true;
                }
                return false;
            }

            replantPlan.ifPresent(plan -> replant(level, player, plan));
            return true;
        }
    }
}
