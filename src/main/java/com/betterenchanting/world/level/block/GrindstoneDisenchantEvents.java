package com.betterenchanting.world.level.block;

import com.betterenchanting.compat.ModularMaterialCompat;
import com.betterenchanting.data.EnchantmentLevelRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class GrindstoneDisenchantEvents {
    private GrindstoneDisenchantEvents() {
    }

    public static void disenchantHeldItem(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (event.getUseBlock().isFalse() || !level.getBlockState(pos).is(Blocks.GRINDSTONE)) {
            return;
        }

        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (held.isEmpty()) {
            return;
        }

        ItemStack result = held.copy();
        if (!removeNonCurseEnchantments(level.registryAccess(), result)) {
            return;
        }

        event.setCancellationResult(level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
        event.setCanceled(true);
        if (level.isClientSide()) {
            return;
        }

        player.setItemInHand(event.getHand(), result);
        player.swing(event.getHand(), true);
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.75D,
                    pos.getZ() + 0.5D,
                    18,
                    0.3D,
                    0.2D,
                    0.3D,
                    0.03D
            );
        }
        player.displayClientMessage(Component.translatable("message.betterenchanting.grindstone_disenchanted", result.getHoverName()), true);
    }

    public static boolean removeNonCurseEnchantments(RegistryAccess registryAccess, ItemStack stack) {
        boolean routedChanged = ModularMaterialCompat.removeNonCurseRoutedEnchantments(registryAccess, stack);
        boolean directChanged = EnchantmentLevelRules.removeNonCurseEnchantments(stack);
        if (routedChanged || directChanged) {
            EnchantmentLevelRules.clampEnchantments(stack);
            return true;
        }
        return false;
    }
}
