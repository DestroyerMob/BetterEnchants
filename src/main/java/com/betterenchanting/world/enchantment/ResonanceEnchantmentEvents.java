package com.betterenchanting.world.enchantment;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.network.ResonanceHighlightPayload;
import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ResonanceEnchantmentEvents {
    private static final int MIN_RADIUS = 2;
    private static final int MAX_RADIUS = 6;
    private static final int MAX_HIGHLIGHTS = 4096;

    private ResonanceEnchantmentEvents() {
    }

    public static void revealMatchingOres(BlockDropsEvent event) {
        if (event.isCanceled()
                || VeinMinerEnchantmentEvents.isVeinMining()
                || !(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = event.getLevel();
        ItemStack tool = event.getTool();
        int resonanceLevel = getResonanceLevel(level, tool);
        if (resonanceLevel <= 0 || player.isSpectator() || !player.mayBuild() || !tool.is(ModTags.Items.TOOL_PICKAXES)) {
            return;
        }

        BlockState originState = event.getState();
        if (!canReveal(originState)) {
            return;
        }

        int radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, resonanceLevel + 1));
        List<BlockPos> positions = findMatchingOres(level, event.getPos(), originState, radius);
        PacketDistributor.sendToPlayer(
                player,
                new ResonanceHighlightPayload(
                        event.getPos(),
                        BuiltInRegistries.BLOCK.getKey(originState.getBlock()),
                        radius,
                        positions,
                        BetterEnchantingConfig.resonanceHighlightDurationTicks()
                )
        );
    }

    private static List<BlockPos> findMatchingOres(ServerLevel level, BlockPos origin, BlockState originState, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.isLoaded(cursor) || !level.getWorldBorder().isWithinBounds(cursor)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(cursor);
                    if (state.is(originState.getBlock()) && canReveal(state)) {
                        positions.add(cursor.immutable());
                        if (positions.size() >= MAX_HIGHLIGHTS) {
                            return positions;
                        }
                    }
                }
            }
        }
        return positions;
    }

    private static boolean canReveal(BlockState state) {
        return !state.isAir() && state.is(ModTags.Blocks.ORES) && isAllowedByConfig(state);
    }

    private static boolean isAllowedByConfig(BlockState state) {
        List<? extends String> whitelist = BetterEnchantingConfig.resonanceBlockWhitelist();
        if (!whitelist.isEmpty() && !matchesAny(whitelist, state)) {
            return false;
        }
        return !matchesAny(BetterEnchantingConfig.resonanceBlockBlacklist(), state);
    }

    private static boolean matchesAny(List<? extends String> entries, BlockState state) {
        for (String entry : entries) {
            if (matchesEntry(entry, state)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesEntry(String entry, BlockState state) {
        String trimmed = entry == null ? "" : entry.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        if (trimmed.startsWith("#")) {
            ResourceLocation tagLocation = ResourceLocation.tryParse(trimmed.substring(1));
            return tagLocation != null && state.is(TagKey.create(Registries.BLOCK, tagLocation));
        }

        ResourceLocation blockLocation = ResourceLocation.tryParse(trimmed);
        return blockLocation != null && BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(blockLocation);
    }

    private static int getResonanceLevel(Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || stack.isEmpty()) {
            return 0;
        }

        return serverLevel.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.RESONANCE)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }
}
