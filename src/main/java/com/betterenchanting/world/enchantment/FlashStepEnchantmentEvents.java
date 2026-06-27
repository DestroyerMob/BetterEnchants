package com.betterenchanting.world.enchantment;

import com.betterenchanting.data.EnchantmentLevelRules;
import com.betterenchanting.registry.ModEnchantments;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class FlashStepEnchantmentEvents {
    private static final double FLASH_DISTANCE = 4.0D;
    private static final double WALL_MARGIN = 0.65D;
    private static final double MIN_FLASH_DISTANCE = 1.0D;
    private static final int COOLDOWN_TICKS = 12;
    private static final double[] VERTICAL_OFFSETS = {0.0D, 1.0D, -1.0D, 2.0D, -2.0D};
    private static final Map<UUID, Long> LAST_FLASH_STEP_TICKS = new HashMap<>();

    private FlashStepEnchantmentEvents() {
    }

    public static void tryFlashStep(ServerPlayer player) {
        if (!canFlashStep(player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();
        Long lastUse = LAST_FLASH_STEP_TICKS.get(player.getUUID());
        if (lastUse != null && gameTime - lastUse < COOLDOWN_TICKS) {
            return;
        }

        Optional<Vec3> destination = findDestination(player);
        if (destination.isEmpty()) {
            return;
        }

        Vec3 start = player.position();
        Vec3 target = destination.get();
        playEffects(level, start);
        player.teleportTo(target.x, target.y, target.z);
        player.resetFallDistance();
        playEffects(level, target);
        LAST_FLASH_STEP_TICKS.put(player.getUUID(), gameTime);
    }

    private static boolean canFlashStep(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && flashStepLevel(player.serverLevel(), player.getItemBySlot(EquipmentSlot.LEGS)) > 0;
    }

    private static int flashStepLevel(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.FLASH_STEP)
                .map(holder -> EnchantmentLevelRules.effectiveLevel(holder, stack.getEnchantmentLevel(holder)))
                .orElse(0);
    }

    private static Optional<Vec3> findDestination(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = horizontalFacing(player);
        Vec3 startEye = player.getEyePosition();
        Vec3 targetEye = startEye.add(direction.scale(FLASH_DISTANCE));
        BlockHitResult hit = level.clip(new ClipContext(startEye, targetEye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double distance = hit.getType() == HitResult.Type.BLOCK
                ? Math.max(0.0D, startEye.distanceTo(hit.getLocation()) - WALL_MARGIN)
                : FLASH_DISTANCE;
        if (distance < MIN_FLASH_DISTANCE) {
            return Optional.empty();
        }

        Vec3 base = player.position().add(direction.scale(distance));
        for (double verticalOffset : VERTICAL_OFFSETS) {
            Vec3 candidate = base.add(0.0D, verticalOffset, 0.0D);
            if (isSafeDestination(level, player, candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static Vec3 horizontalFacing(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() > 1.0E-6D) {
            return horizontal.normalize();
        }

        float yawRadians = player.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians)).normalize();
    }

    private static boolean isSafeDestination(ServerLevel level, ServerPlayer player, Vec3 position) {
        AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(position).deflate(1.0E-7D);
        BlockPos center = BlockPos.containing(position);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        return level.isLoaded(center)
                && level.isInWorldBounds(min)
                && level.isInWorldBounds(max)
                && level.noCollision(player, box)
                && !level.containsAnyLiquid(box);
    }

    private static void playEffects(ServerLevel level, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.45F, 1.6F);
        level.sendParticles(ParticleTypes.PORTAL, position.x, position.y + 1.0D, position.z, 18, 0.35D, 0.5D, 0.35D, 0.02D);
    }
}
