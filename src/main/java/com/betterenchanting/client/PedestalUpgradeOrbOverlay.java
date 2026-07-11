package com.betterenchanting.client;

import com.betterenchanting.compat.MobsToolForgingCompat;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedEnchantmentBreakdown;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedEnchantmentState;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedPartBreakdown;
import com.betterenchanting.data.EnchantmentLevelRules;
import com.betterenchanting.network.SelectPedestalUpgradePayload;
import com.betterenchanting.registry.ModItems;
import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import com.betterenchanting.world.inventory.PedestalUpgradeRules.UpgradePlan;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PedestalUpgradeOrbOverlay {
    private static final int SCAN_RADIUS = 8;
    private static final int VERTICAL_RADIUS = 4;
    private static final int SCAN_INTERVAL = 5;
    private static final double MAX_PICK_DISTANCE = 8.0D;
    private static final double PICK_RADIUS = 0.48D;
    private static final double ORBIT_PAUSE_TRANSITION_TICKS = 5.0D;
    private static final double ORBIT_STATE_TTL_TICKS = 80.0D;
    private static final int ORB_COLOR = 0xFFE562FF;
    private static final int SELECTED_COLOR = 0xFFFFD166;
    private static final int MAX_COLOR = 0xFF8B93A1;
    private static final RenderType ORBS = RenderType.create(
            "betterenchanting_pedestal_upgrade_orbs",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );

    private static ClientLevel cachedLevel;
    private static BlockPos cachedOrigin = BlockPos.ZERO;
    private static long lastScan = Long.MIN_VALUE;
    private static List<AttunementPedestalBlockEntity> cachedPedestals = List.of();
    private static List<Orb> lastOrbs = List.of();
    private static Optional<Orb> hovered = Optional.empty();
    private static final Map<OrbitKey, OrbitState> orbitStates = new HashMap<>();

    private PedestalUpgradeOrbOverlay() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canDisplay(minecraft)) {
            lastOrbs = List.of();
            hovered = Optional.empty();
            orbitStates.clear();
            return;
        }

        Camera camera = event.getCamera();
        double time = minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        List<Orb> orbs = new ArrayList<>();
        List<PartDisplay> parts = new ArrayList<>();
        for (AttunementPedestalBlockEntity pedestal : nearbyPedestals(minecraft)) {
            layoutPedestal(pedestal, camera, time, orbs, parts);
        }
        updateOrbitStates(orbs, hovered.map(OrbitKey::from), time);
        Optional<Orb> currentHovered = pick(orbs, minecraft, time);
        updateOrbitStates(orbs, currentHovered.map(OrbitKey::from), time);
        lastOrbs = List.copyOf(orbs);
        hovered = currentHovered;

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (PartDisplay part : parts) {
            renderItem(event.getPoseStack(), camera, buffers, part.stack(), part.position(), part.seed());
        }
        VertexConsumer consumer = buffers.getBuffer(ORBS);
        for (Orb orb : orbs) {
            boolean isHovered = hovered.filter(value -> value.sameTarget(orb)).isPresent();
            int color = isHovered ? 0xFFFFFFFF : orb.color();
            double pulse = 1.0D + Math.sin(time * 0.22D + orb.phase()) * 0.10D;
            Vec3 position = orbPosition(orb, time);
            drawDiamond(event.getPoseStack(), camera, consumer, position, 0.13D * pulse, color, 0.92F);
            drawDiamond(event.getPoseStack(), camera, consumer, position, 0.205D * pulse, color, 0.18F);
        }
        buffers.endBatch(ORBS);
    }

    public static void renderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canDisplay(minecraft) || hovered.isEmpty()) {
            return;
        }
        Orb orb = hovered.get();
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        Component name = Enchantment.getFullname(orb.enchantment(), orb.currentLevel());
        Component progression = orb.plan().maximumReached()
                ? Component.translatable("gui.betterenchanting.pedestal.orb.maximum").withStyle(ChatFormatting.GRAY)
                : Component.translatable(
                        "gui.betterenchanting.pedestal.orb.progression",
                        orb.plan().currentLevel(),
                        orb.plan().nextLevel()
                )
                .withStyle(orb.selected() ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE);
        Component requirements = orb.plan().validSelection()
                ? Component.translatable(
                        "gui.betterenchanting.pedestal.orb.requirements",
                        orb.plan().essenceCost(),
                        orb.plan().availablePower(),
                        orb.plan().requiredPower()
                ).withStyle(ChatFormatting.DARK_GRAY)
                : Component.empty();
        Component action = orbAction(orb, minecraft);
        int width = Math.max(font.width(name), Math.max(font.width(progression),
                Math.max(font.width(requirements), font.width(action)))) + 22;
        width = Math.max(128, Math.min(width, minecraft.getWindow().getGuiScaledWidth() - 16));
        int x = Math.max(8, Math.min(minecraft.getWindow().getGuiScaledWidth() - width - 8,
                minecraft.getWindow().getGuiScaledWidth() / 2 + 14));
        int y = Math.max(8, Math.min(minecraft.getWindow().getGuiScaledHeight() - 56,
                minecraft.getWindow().getGuiScaledHeight() / 2 + 12));
        graphics.fill(x, y, x + width, y + 52, 0xD0100E17);
        graphics.renderOutline(x, y, width, 52, orb.selected() ? SELECTED_COLOR : 0xAAFFFFFF);
        graphics.fill(x + 6, y + 7, x + 10, y + 45, orb.color());
        graphics.drawString(font, name, x + 15, y + 5, 0xFFF2EAF7, true);
        graphics.drawString(font, progression, x + 15, y + 17, 0xFFD8B4F2, false);
        graphics.drawString(font, requirements, x + 15, y + 29, 0xFF918A99, false);
        graphics.drawString(font, action, x + 15, y + 41,
                canUpgrade(orb, minecraft) && orb.selected() ? 0xFFB8F29F : 0xFFAFA7B7, false);
    }

    public static void handleInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canDisplay(minecraft)) {
            return;
        }
        Optional<Orb> hit = pick(lastOrbs, minecraft,
                minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false));
        if (hit.isEmpty()) {
            return;
        }
        Orb orb = hit.get();
        PacketDistributor.sendToServer(new SelectPedestalUpgradePayload(
                orb.pedestalPos(), orb.partIndex(), orb.enchantmentId()));
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    private static void layoutPedestal(AttunementPedestalBlockEntity pedestal, Camera camera, double time,
                                       List<Orb> orbs, List<PartDisplay> parts) {
        ItemStack target = pedestal.target();
        if (target.isEmpty()) {
            return;
        }
        BlockPos pos = pedestal.getBlockPos();
        Vec3 anchor = Vec3.atCenterOf(pos).add(0.0D, 1.42D, 0.0D);
        Vec3 right = rightVector(anchor, camera.getPosition());
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Optional<RoutedEnchantmentBreakdown> routed = MobsToolForgingCompat.routedEnchantmentBreakdown(
                pedestal.getLevel().registryAccess(), target);
        List<RoutedPartBreakdown> enchantedParts = routed.map(value -> value.parts().stream()
                .filter(part -> !part.enchantments().isEmpty()).toList()).orElse(List.of());
        if (!enchantedParts.isEmpty()) {
            for (int partOrdinal = 0; partOrdinal < enchantedParts.size(); partOrdinal++) {
                RoutedPartBreakdown part = enchantedParts.get(partOrdinal);
                double offset = (partOrdinal - (enchantedParts.size() - 1) * 0.5D) * 1.08D;
                Vec3 partPos = anchor.add(right.scale(offset)).add(0.0D,
                        Math.sin(time * 0.10D + part.partIndex()) * 0.04D, 0.0D);
                parts.add(new PartDisplay(part.partStack(), partPos, (int) (pos.asLong() + part.partIndex())));
                addRoutedOrbs(pedestal, part, partPos, right, up, time, orbs);
            }
            return;
        }

        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(target);
        List<Object2IntMap.Entry<Holder<Enchantment>>> entries = new ArrayList<>(enchantments.entrySet());
        for (int index = 0; index < entries.size(); index++) {
            Object2IntMap.Entry<Holder<Enchantment>> entry = entries.get(index);
            ResourceLocation id = entry.getKey().unwrapKey().map(ResourceKey::location).orElse(null);
            if (id == null) {
                continue;
            }
            double angle = index * Math.PI * 2.0D / Math.max(1, entries.size());
            orbs.add(createOrb(pedestal, -1, entry.getKey(), id, entry.getIntValue(), anchor, right, up,
                    0.72D, angle, 0.015D, index));
        }
    }

    private static void addRoutedOrbs(AttunementPedestalBlockEntity pedestal, RoutedPartBreakdown part,
                                       Vec3 center, Vec3 right, Vec3 up, double time, List<Orb> orbs) {
        List<RoutedEnchantmentState> enchantments = part.enchantments();
        for (int index = 0; index < enchantments.size(); index++) {
            RoutedEnchantmentState state = enchantments.get(index);
            double angle = index * Math.PI * 2.0D / Math.max(1, enchantments.size());
            orbs.add(createOrb(pedestal, part.partIndex(), state.enchantment(), state.enchantmentId(),
                    state.level(), center, right, up, 0.43D, angle, -0.018D, part.partIndex() * 17 + index));
        }
    }

    private static Orb createOrb(AttunementPedestalBlockEntity pedestal, int partIndex,
                                  Holder<Enchantment> enchantment, ResourceLocation id, int level,
                                  Vec3 center, Vec3 orbitX, Vec3 orbitY, double orbitRadius,
                                  double baseAngle, double orbitSpeed, int phase) {
        boolean selected = id.equals(pedestal.selectedEnchantment()) && partIndex == pedestal.selectedPartIndex();
        UpgradePlan plan = pedestal.previewPlan(partIndex, id);
        int color = selected ? SELECTED_COLOR : plan.maximumReached() ? MAX_COLOR : ORB_COLOR;
        return new Orb(pedestal.getBlockPos(), partIndex, enchantment, id, level, center, orbitX, orbitY,
                orbitRadius, baseAngle, orbitSpeed, selected, plan, color, phase);
    }

    private static Component orbAction(Orb orb, Minecraft minecraft) {
        if (!orb.selected()) {
            return Component.translatable("gui.betterenchanting.pedestal.orb.select").withStyle(ChatFormatting.GRAY);
        }
        UpgradePlan plan = orb.plan();
        if (plan.canUpgrade()) {
            return Component.translatable("gui.betterenchanting.pedestal.orb.upgrade").withStyle(ChatFormatting.GREEN);
        }
        if (!plan.validSelection() || plan.maximumReached()) {
            return Component.translatable("gui.betterenchanting.pedestal.maximum").withStyle(ChatFormatting.GRAY);
        }
        if (!plan.linkedTable()) {
            return Component.translatable("gui.betterenchanting.pedestal.no_table").withStyle(ChatFormatting.RED);
        }
        if (!plan.enoughPower()) {
            return Component.translatable("gui.betterenchanting.pedestal.low_power").withStyle(ChatFormatting.RED);
        }
        if (!plan.matchingEssence()) {
            return Component.translatable("gui.betterenchanting.pedestal.wrong_essence").withStyle(ChatFormatting.RED);
        }
        if (!plan.enoughEssence()) {
            return Component.translatable("gui.betterenchanting.pedestal.need_essence", plan.essenceCost())
                    .withStyle(ChatFormatting.RED);
        }
        if (plan.catalystRequired() && !plan.hasCatalyst()) {
            return Component.translatable("gui.betterenchanting.pedestal.need_star").withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable("gui.betterenchanting.pedestal.unavailable").withStyle(ChatFormatting.RED);
    }

    private static boolean canUpgrade(Orb orb, Minecraft minecraft) {
        return orb.plan().canUpgrade();
    }

    private static List<AttunementPedestalBlockEntity> nearbyPedestals(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        BlockPos origin = minecraft.player.blockPosition();
        long time = level.getGameTime();
        if (level == cachedLevel && origin.equals(cachedOrigin) && time - lastScan < SCAN_INTERVAL) {
            return cachedPedestals;
        }
        List<AttunementPedestalBlockEntity> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -VERTICAL_RADIUS; y <= VERTICAL_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (level.getBlockEntity(cursor) instanceof AttunementPedestalBlockEntity pedestal
                            && !pedestal.target().isEmpty()) {
                        found.add(pedestal);
                    }
                }
            }
        }
        cachedLevel = level;
        cachedOrigin = origin.immutable();
        lastScan = time;
        cachedPedestals = List.copyOf(found);
        return cachedPedestals;
    }

    private static Optional<Orb> pick(List<Orb> orbs, Minecraft minecraft, double time) {
        if (minecraft.player == null || orbs.isEmpty()) {
            return Optional.empty();
        }
        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        Orb best = null;
        double bestAlong = Double.MAX_VALUE;
        for (Orb orb : orbs) {
            Vec3 delta = orbPosition(orb, time).subtract(origin);
            double along = delta.dot(look);
            if (along < 0.4D || along > MAX_PICK_DISTANCE) {
                continue;
            }
            if (delta.subtract(look.scale(along)).length() <= PICK_RADIUS && along < bestAlong) {
                best = orb;
                bestAlong = along;
            }
        }
        return Optional.ofNullable(best);
    }

    private static void updateOrbitStates(List<Orb> orbs, Optional<OrbitKey> pausedOrbit, double time) {
        for (Orb orb : orbs) {
            OrbitKey key = OrbitKey.from(orb);
            OrbitState state = orbitStates.computeIfAbsent(key, unused -> new OrbitState(time));
            state.advance(time, pausedOrbit.filter(key::equals).isPresent());
        }
        orbitStates.entrySet().removeIf(entry -> time - entry.getValue().lastSeenTime > ORBIT_STATE_TTL_TICKS);
    }

    private static Vec3 orbPosition(Orb orb, double time) {
        double orbitTime = Optional.ofNullable(orbitStates.get(OrbitKey.from(orb)))
                .map(state -> state.orbitTime)
                .orElse(time);
        double angle = orb.baseAngle() + orbitTime * orb.orbitSpeed();
        return orb.center()
                .add(orb.orbitX().scale(Math.cos(angle) * orb.orbitRadius()))
                .add(orb.orbitY().scale(Math.sin(angle) * orb.orbitRadius()));
    }

    private static boolean canDisplay(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && minecraft.screen == null
                && !minecraft.options.hideGui
                && (minecraft.player.getMainHandItem().is(ModItems.ATTUNEMENT_FOCUS.get())
                || minecraft.player.getOffhandItem().is(ModItems.ATTUNEMENT_FOCUS.get()));
    }

    private static Vec3 rightVector(Vec3 anchor, Vec3 camera) {
        Vec3 toCamera = camera.subtract(anchor);
        Vec3 right = new Vec3(toCamera.z, 0.0D, -toCamera.x);
        return right.lengthSqr() < 0.001D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
    }

    private static void renderItem(PoseStack poses, Camera camera, MultiBufferSource buffers,
                                   ItemStack stack, Vec3 position, int seed) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 relative = position.subtract(camera.getPosition());
        poses.pushPose();
        poses.translate(relative.x, relative.y, relative.z);
        poses.mulPose(camera.rotation());
        poses.scale(0.29F, 0.29F, 0.29F);
        minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poses, buffers, minecraft.level, seed);
        poses.popPose();
    }

    private static void drawDiamond(PoseStack poses, Camera camera, VertexConsumer consumer,
                                    Vec3 position, double radius, int color, float alpha) {
        Vec3 relative = position.subtract(camera.getPosition());
        poses.pushPose();
        poses.translate(relative.x, relative.y, relative.z);
        poses.mulPose(camera.rotation());
        PoseStack.Pose pose = poses.last();
        float r = (float) radius;
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        consumer.addVertex(pose, 0.0F, r, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, r, 0.0F, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, 0.0F, -r, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, -r, 0.0F, 0.0F).setColor(red, green, blue, alpha);
        poses.popPose();
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * Math.max(0.0D, Math.min(1.0D, amount));
    }

    private static double smoothStep(double amount) {
        double clamped = Math.max(0.0D, Math.min(1.0D, amount));
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private record PartDisplay(ItemStack stack, Vec3 position, int seed) {
    }

    private record Orb(BlockPos pedestalPos, int partIndex, Holder<Enchantment> enchantment,
                       ResourceLocation enchantmentId, int currentLevel, Vec3 center, Vec3 orbitX, Vec3 orbitY,
                       double orbitRadius, double baseAngle, double orbitSpeed, boolean selected,
                       UpgradePlan plan, int color, int phase) {
        private boolean sameTarget(Orb other) {
            return this.pedestalPos.equals(other.pedestalPos)
                    && this.partIndex == other.partIndex
                    && this.enchantmentId.equals(other.enchantmentId);
        }
    }

    private record OrbitKey(BlockPos pedestalPos, int partIndex) {
        private static OrbitKey from(Orb orb) {
            return new OrbitKey(orb.pedestalPos(), orb.partIndex());
        }
    }

    private static final class OrbitState {
        private double orbitTime;
        private double lastRenderTime;
        private double startPauseWeight;
        private double targetPauseWeight;
        private double pauseTransitionStartTime;
        private double lastSeenTime;

        private OrbitState(double time) {
            this.orbitTime = time;
            this.lastRenderTime = time;
            this.pauseTransitionStartTime = time;
            this.lastSeenTime = time;
        }

        private void advance(double time, boolean paused) {
            double pauseWeight = pauseWeight(time);
            orbitTime += Math.max(0.0D, time - lastRenderTime) * (1.0D - pauseWeight);
            lastRenderTime = time;

            double target = paused ? 1.0D : 0.0D;
            if (Math.abs(targetPauseWeight - target) > 0.001D) {
                startPauseWeight = pauseWeight;
                targetPauseWeight = target;
                pauseTransitionStartTime = time;
            }
            lastSeenTime = time;
        }

        private double pauseWeight(double time) {
            return lerp(startPauseWeight, targetPauseWeight,
                    smoothStep((time - pauseTransitionStartTime) / ORBIT_PAUSE_TRANSITION_TICKS));
        }
    }
}
