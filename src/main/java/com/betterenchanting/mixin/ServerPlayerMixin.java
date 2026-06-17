package com.betterenchanting.mixin;

import com.betterenchanting.world.enchantment.GelboundEnchantmentEvents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Unique
    private double betterenchanting$gelboundLandingY;

    @Inject(method = "doCheckFallDamage", at = @At("HEAD"))
    private void betterenchanting$captureGelboundLanding(double xDistance, double yDistance, double zDistance, boolean onGround, CallbackInfo callbackInfo) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        this.betterenchanting$gelboundLandingY = 0.0D;
        if (onGround
                && yDistance < 0.0D
                && player.fallDistance > 0.0F
                && !player.isSuppressingBounce()
                && GelboundEnchantmentEvents.hasGelbound(player)) {
            this.betterenchanting$gelboundLandingY = yDistance;
        }
    }

    @Inject(method = "doCheckFallDamage", at = @At("TAIL"))
    private void betterenchanting$bounceGelboundLanding(double xDistance, double yDistance, double zDistance, boolean onGround, CallbackInfo callbackInfo) {
        if (this.betterenchanting$gelboundLandingY >= 0.0D) {
            return;
        }

        ServerPlayer player = (ServerPlayer) (Object) this;
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, this.betterenchanting$gelboundLandingY, movement.z);
        Blocks.SLIME_BLOCK.updateEntityAfterFallOn(player.level(), player);
        player.resetFallDistance();
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        this.betterenchanting$gelboundLandingY = 0.0D;
    }
}
