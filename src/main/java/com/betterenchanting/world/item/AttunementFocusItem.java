package com.betterenchanting.world.item;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.network.GeodeSearchPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AttunementFocusItem extends Item {
    private static final int CHUNK_RADIUS = 1;
    private static final int COOLDOWN_TICKS = 15 * 20;
    private static final int MAX_HIGHLIGHTS = 512;

    public AttunementFocusItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            SearchResult result = findBuddingAmethyst(serverPlayer.serverLevel(), player.blockPosition());
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new GeodeSearchPayload(
                            player.blockPosition(),
                            result.searchMin(),
                            result.searchMax(),
                            result.positions(),
                            BetterEnchantingConfig.resonanceHighlightDurationTicks()
                    )
            );
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            playActivationEffects(serverPlayer.serverLevel(), serverPlayer, !result.positions().isEmpty());
            player.displayClientMessage(
                    Component.translatable(result.positions().isEmpty()
                            ? "message.betterenchanting.attunement_focus.no_geode"
                            : "message.betterenchanting.attunement_focus.geode_found"),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.betterenchanting.attunement_focus.scan").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.betterenchanting.attunement_focus.tune").withStyle(ChatFormatting.GRAY));
    }

    private static SearchResult findBuddingAmethyst(ServerLevel level, BlockPos origin) {
        int centerChunkX = SectionPos.blockToSectionCoord(origin.getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        int minChunkX = centerChunkX - CHUNK_RADIUS;
        int maxChunkX = centerChunkX + CHUNK_RADIUS;
        int minChunkZ = centerChunkZ - CHUNK_RADIUS;
        int maxChunkZ = centerChunkZ + CHUNK_RADIUS;
        BlockPos searchMin = new BlockPos(
                SectionPos.sectionToBlockCoord(minChunkX),
                level.getMinBuildHeight(),
                SectionPos.sectionToBlockCoord(minChunkZ)
        );
        BlockPos searchMax = new BlockPos(
                SectionPos.sectionToBlockCoord(maxChunkX) + 15,
                level.getMaxBuildHeight() - 1,
                SectionPos.sectionToBlockCoord(maxChunkZ) + 15
        );

        List<BlockPos> positions = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                scanChunk(chunk, positions);
                if (positions.size() >= MAX_HIGHLIGHTS) {
                    return new SearchResult(searchMin, searchMax, List.copyOf(positions));
                }
            }
        }
        return new SearchResult(searchMin, searchMax, List.copyOf(positions));
    }

    private static void scanChunk(LevelChunk chunk, List<BlockPos> positions) {
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(Blocks.BUDDING_AMETHYST))) {
                continue;
            }
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex));
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        if (!section.getBlockState(localX, localY, localZ).is(Blocks.BUDDING_AMETHYST)) {
                            continue;
                        }
                        positions.add(new BlockPos(baseX + localX, baseY + localY, baseZ + localZ));
                        if (positions.size() >= MAX_HIGHLIGHTS) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void playActivationEffects(ServerLevel level, ServerPlayer player, boolean found) {
        float pitch = found ? 1.35F : 0.82F;
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                1.0F,
                pitch
        );
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS,
                0.55F,
                found ? 1.18F : 0.9F
        );
        RandomSource random = level.random;
        for (int index = 0; index < 18; index++) {
            double angle = Math.PI * 2.0D * index / 18.0D;
            double radius = 0.45D + random.nextDouble() * 0.35D;
            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.8D + random.nextDouble() * 0.9D,
                    player.getZ() + Math.sin(angle) * radius,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.03D
            );
        }
    }

    private record SearchResult(BlockPos searchMin, BlockPos searchMax, List<BlockPos> positions) {
    }
}
