package com.betterenchanting.client;

import com.betterenchanting.client.MachineDisplayState.Display;
import com.betterenchanting.data.EssenceDistillationRecipes;
import com.betterenchanting.world.level.block.entity.ArcaneCrucibleBlockEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ArcaneCrucibleRenderer implements BlockEntityRenderer<ArcaneCrucibleBlockEntity> {
    private static final RenderType REACTION_GLOW = RenderType.create(
            "betterenchanting_crucible_reaction",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1_024,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );
    private static final Vec3 REACTION_CENTER = new Vec3(0.5D, 1.62D, 0.5D);
    private static final int AMETHYST_COLOR = 0xC989FF;
    private static final int ARCANE_BLUE = 0x72D8FF;
    private static final int DEFAULT_ESSENCE_COLOR = 0xB36BFF;
    private static final Map<ArcaneCrucibleBlockEntity, AnimationState> ANIMATIONS = new WeakHashMap<>();

    public ArcaneCrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArcaneCrucibleBlockEntity crucible, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!(crucible.getLevel() instanceof ClientLevel level)) {
            return;
        }
        double time = level.getGameTime() + partialTick;
        ItemStack recipeResult = EssenceDistillationRecipes.find(
                        crucible.getItem(ArcaneCrucibleBlockEntity.MEDIUM_SLOT),
                        crucible.getItem(ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT),
                        crucible.getItem(ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + 1)
                )
                .map(EssenceDistillationRecipes.Recipe::resultCopy)
                .orElse(ItemStack.EMPTY);
        AnimationState state = ANIMATIONS.computeIfAbsent(crucible, unused -> new AnimationState());
        state.advance(crucible, recipeResult, time);
        List<Display> published = new ArrayList<>();
        ItemStack formingResult = state.formingResult();
        int reactionColor = essenceColor(formingResult);

        float convergence = smoothstep(0.12F, 0.92F, state.progress);
        float collision = smoothstep(0.86F, 0.98F, state.progress);
        float ingredientScale = 1.0F - collision;
        double orbitAngle = 0.38D + smoothstep(0.02F, 0.92F, state.progress) * 2.20D;
        double orbitRadius = Mth.lerp(convergence, 0.42D, 0.018D);
        double orbitHeight = Mth.lerp(smoothstep(0.08F, 0.92F, state.progress), 1.23D, REACTION_CENTER.y);

        Vec3 mediumPosition = orbitPosition(orbitAngle, orbitRadius, orbitHeight);
        renderIngredient(crucible, state.items, ArcaneCrucibleBlockEntity.MEDIUM_SLOT, poses, buffers,
                packedOverlay, mediumPosition, 0.42F * ingredientScale, time, 0);
        publishCurrent(crucible, ArcaneCrucibleBlockEntity.MEDIUM_SLOT, mediumPosition, 0.27D, published);

        Vec3[] catalystPositions = new Vec3[ArcaneCrucibleBlockEntity.CATALYST_SLOT_COUNT];
        for (int index = 0; index < ArcaneCrucibleBlockEntity.CATALYST_SLOT_COUNT; index++) {
            int slot = ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + index;
            double angle = orbitAngle + (index + 1) * Mth.TWO_PI / 3.0D;
            Vec3 position = orbitPosition(angle, orbitRadius, orbitHeight);
            catalystPositions[index] = position;
            renderIngredient(crucible, state.items, slot, poses, buffers, packedOverlay,
                    position, 0.34F * ingredientScale, time, 1 + index);
            publishCurrent(crucible, slot, position, 0.24D, published);
        }

        if (state.progress > 0.002F) {
            renderReaction(
                    state.items,
                    poses,
                    buffers.getBuffer(REACTION_GLOW),
                    catalystPositions,
                    mediumPosition,
                    state.progress,
                    reactionColor,
                    time
            );
        }

        float essenceReveal = smoothstep(0.88F, 0.99F, state.progress);
        if (!formingResult.isEmpty() && essenceReveal > 0.001F) {
            FloatingItemVisuals.render(
                    crucible,
                    formingResult,
                    poses,
                    buffers,
                    packedOverlay,
                    REACTION_CENTER,
                    0.48F * essenceReveal,
                    FloatingItemVisuals.slowRotation(time, 35.0D),
                    19
            );
        }

        Vec3 outputPosition = REACTION_CENTER.add(
                0.0D,
                state.progress <= 0.001F ? FloatingItemVisuals.bob(time, 1.4D, 0.025D) : 0.0D,
                0.0D
        );
        if (state.progress <= 0.001F) {
            FloatingItemVisuals.render(
                    crucible,
                    crucible.getItem(ArcaneCrucibleBlockEntity.OUTPUT_SLOT),
                    poses,
                    buffers,
                    packedOverlay,
                    outputPosition,
                    0.48F,
                    FloatingItemVisuals.slowRotation(time, 35.0D),
                    3
            );
            publishCurrent(crucible, ArcaneCrucibleBlockEntity.OUTPUT_SLOT, outputPosition, 0.31D, published);
        }

        MachineDisplayState.publish(level, crucible.getBlockPos(), published);
    }

    private static Vec3 orbitPosition(double angle, double radius, double height) {
        return new Vec3(
                REACTION_CENTER.x + Math.cos(angle) * radius,
                height,
                REACTION_CENTER.z + Math.sin(angle) * radius
        );
    }

    private static void renderIngredient(ArcaneCrucibleBlockEntity crucible, MachineItemAnimation animation,
                                         int slot, PoseStack poses, MultiBufferSource buffers, int packedOverlay,
                                         Vec3 position, float scale, double time, int seedOffset) {
        FloatingItemVisuals.render(
                crucible,
                animation.displayed(slot),
                poses,
                buffers,
                packedOverlay,
                position,
                scale * animation.reveal(slot),
                FloatingItemVisuals.slowRotation(time, seedOffset * 120.0D),
                seedOffset
        );
    }

    private static void publishCurrent(ArcaneCrucibleBlockEntity crucible, int slot, Vec3 localPosition,
                                       double pickRadius, List<Display> displays) {
        ItemStack current = crucible.getItem(slot);
        if (current.isEmpty()) {
            return;
        }
        displays.add(new Display(
                crucible.getBlockPos(),
                slot,
                localPosition.add(Vec3.atLowerCornerOf(crucible.getBlockPos())),
                current,
                pickRadius
        ));
    }

    private static void renderReaction(MachineItemAnimation animation, PoseStack poses, VertexConsumer consumer,
                                       Vec3[] catalystPositions, Vec3 mediumPosition, float progress,
                                       int reactionColor, double time) {
        PoseStack.Pose pose = poses.last();
        float activation = smoothstep(0.02F, 0.18F, progress);
        float mixing = smoothstep(0.12F, 0.72F, progress);
        float condensation = smoothstep(0.68F, 0.97F, progress);
        float reactionPulse = 0.88F + sinTime(time, 0.24D + progress * 0.28D, 0.0D) * 0.12F;

        renderTransferStream(
                pose,
                consumer,
                mediumPosition,
                REACTION_CENTER,
                AMETHYST_COLOR,
                activation * (0.55F + mixing * 0.45F),
                time,
                0
        );
        for (int index = 0; index < catalystPositions.length; index++) {
            int slot = ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + index;
            if (animation.displayed(slot).isEmpty()) {
                continue;
            }
            int streamColor = index == 0
                    ? reactionColor
                    : blendColor(reactionColor, ARCANE_BLUE, 0.46F);
            renderTransferStream(
                    pose,
                    consumer,
                    catalystPositions[index],
                    REACTION_CENTER,
                    streamColor,
                    activation * (0.64F + mixing * 0.36F),
                    time,
                    index + 1
            );
        }

        float contractingRadius = Mth.lerp(smoothstep(0.08F, 0.90F, progress), 0.37F, 0.10F);
        renderRing(
                pose,
                consumer,
                REACTION_CENTER.add(0.0D, -0.065D, 0.0D),
                contractingRadius,
                0.010F + mixing * 0.008F,
                reactionColor,
                activation * (0.22F + mixing * 0.34F),
                0.0F
        );
        renderRing(
                pose,
                consumer,
                REACTION_CENTER.add(0.0D, -0.045D, 0.0D),
                0.17F + sinTime(time, 0.18D, 0.0D) * 0.018F,
                0.008F,
                blendColor(AMETHYST_COLOR, reactionColor, mixing),
                activation * (0.20F + mixing * 0.30F) * (1.0F - condensation * 0.55F),
                Mth.PI / 24.0F
        );

        float coreSize = (0.055F + mixing * 0.075F - condensation * 0.040F) * reactionPulse;
        drawSpark(
                pose,
                consumer,
                REACTION_CENTER,
                coreSize * 1.85F,
                blendColor(AMETHYST_COLOR, reactionColor, mixing),
                activation * (0.13F + mixing * 0.22F)
        );
        drawSpark(
                pose,
                consumer,
                REACTION_CENTER,
                coreSize,
                blendColor(reactionColor, 0xFFFFFF, condensation * 0.58F),
                activation * (0.64F + mixing * 0.24F)
        );

        renderReactionVapor(
                pose,
                consumer,
                progress,
                blendColor(reactionColor, 0xFFFFFF, condensation * 0.45F),
                time
        );
    }

    private static void renderTransferStream(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end,
                                             int color, float activity, double time, int phase) {
        if (activity <= 0.002F) {
            return;
        }
        Vec3 radial = start.subtract(end);
        Vec3 curl = new Vec3(-radial.z, 0.0D, radial.x).scale((phase % 2 == 0 ? 1.0D : -1.0D) * 0.30D);
        Vec3 control = start.lerp(end, 0.48D).add(curl).add(0.0D, 0.10D + phase * 0.014D, 0.0D);
        for (int index = 0; index < 7; index++) {
            float travel = fraction(time * (0.030D + activity * 0.022D) + index / 7.0D + phase * 0.19D);
            float easedTravel = travel * travel * (3.0F - 2.0F * travel);
            Vec3 position = quadratic(start, control, end, easedTravel);
            float envelope = Mth.sin(travel * Mth.PI);
            float size = 0.010F + envelope * (0.010F + activity * 0.006F);
            drawSpark(pose, consumer, position, size, color, activity * envelope * 0.82F);
        }
    }

    private static void renderReactionVapor(PoseStack.Pose pose, VertexConsumer consumer, float progress,
                                            int color, double time) {
        float activity = smoothstep(0.10F, 0.58F, progress);
        if (activity <= 0.002F) {
            return;
        }
        for (int index = 0; index < 8; index++) {
            float rise = fraction(time * (0.012D + progress * 0.012D) + index / 8.0D);
            float angle = wrappedRadians(time, 0.075D, index * 2.399963D + rise * 3.2D);
            float radius = (0.035F + rise * 0.075F) * (1.0F - progress * 0.32F);
            Vec3 position = new Vec3(
                    REACTION_CENTER.x + Mth.cos(angle) * radius,
                    REACTION_CENTER.y + 0.035D + rise * 0.42D,
                    REACTION_CENTER.z + Mth.sin(angle) * radius
            );
            float envelope = Mth.sin(rise * Mth.PI);
            drawSpark(
                    pose,
                    consumer,
                    position,
                    0.008F + envelope * 0.012F,
                    color,
                    activity * envelope * (0.26F + progress * 0.42F)
            );
        }
    }

    private static void renderRing(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center,
                                   float radius, float width, int color, float alpha, float rotation) {
        if (alpha <= 0.002F) {
            return;
        }
        float innerRadius = Math.max(0.0F, radius - width);
        float outerRadius = radius + width;
        for (int index = 0; index < 24; index++) {
            float firstAngle = rotation + index * Mth.TWO_PI / 24.0F;
            float secondAngle = rotation + (index + 1) * Mth.TWO_PI / 24.0F;
            addColoredVertex(pose, consumer,
                    center.x + Mth.cos(firstAngle) * innerRadius, center.y,
                    center.z + Mth.sin(firstAngle) * innerRadius, color, alpha);
            addColoredVertex(pose, consumer,
                    center.x + Mth.cos(secondAngle) * innerRadius, center.y,
                    center.z + Mth.sin(secondAngle) * innerRadius, color, alpha);
            addColoredVertex(pose, consumer,
                    center.x + Mth.cos(secondAngle) * outerRadius, center.y,
                    center.z + Mth.sin(secondAngle) * outerRadius, color, alpha);
            addColoredVertex(pose, consumer,
                    center.x + Mth.cos(firstAngle) * outerRadius, center.y,
                    center.z + Mth.sin(firstAngle) * outerRadius, color, alpha);
        }
    }

    private static void drawSpark(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center,
                                  float size, int color, float alpha) {
        if (alpha <= 0.002F || size <= 0.001F) {
            return;
        }
        addDiamond(pose, consumer, center, size, color, alpha, 0);
        addDiamond(pose, consumer, center, size, color, alpha * 0.72F, 1);
        addDiamond(pose, consumer, center, size, color, alpha * 0.72F, 2);
    }

    private static void addDiamond(PoseStack.Pose pose, VertexConsumer consumer, Vec3 center,
                                   float size, int color, float alpha, int plane) {
        if (plane == 0) {
            addColoredVertex(pose, consumer, center.x, center.y, center.z - size, color, alpha);
            addColoredVertex(pose, consumer, center.x + size, center.y, center.z, color, alpha);
            addColoredVertex(pose, consumer, center.x, center.y, center.z + size, color, alpha);
            addColoredVertex(pose, consumer, center.x - size, center.y, center.z, color, alpha);
        } else if (plane == 1) {
            addColoredVertex(pose, consumer, center.x, center.y + size, center.z, color, alpha);
            addColoredVertex(pose, consumer, center.x + size, center.y, center.z, color, alpha);
            addColoredVertex(pose, consumer, center.x, center.y - size, center.z, color, alpha);
            addColoredVertex(pose, consumer, center.x - size, center.y, center.z, color, alpha);
        } else {
            addColoredVertex(pose, consumer, center.x, center.y + size, center.z, color, alpha);
            addColoredVertex(pose, consumer, center.x, center.y, center.z + size, color, alpha);
            addColoredVertex(pose, consumer, center.x, center.y - size, center.z, color, alpha);
            addColoredVertex(pose, consumer, center.x, center.y, center.z - size, color, alpha);
        }
    }

    private static void addColoredVertex(PoseStack.Pose pose, VertexConsumer consumer,
                                         double x, double y, double z, int color, float alpha) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red(color), green(color), blue(color), Mth.clamp(alpha, 0.0F, 1.0F));
    }

    private static Vec3 quadratic(Vec3 start, Vec3 control, Vec3 end, float progress) {
        double remaining = 1.0D - progress;
        return start.scale(remaining * remaining)
                .add(control.scale(2.0D * remaining * progress))
                .add(end.scale(progress * progress));
    }

    private static float smoothstep(float start, float end, float value) {
        float normalized = Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return normalized * normalized * (3.0F - 2.0F * normalized);
    }

    private static float fraction(double value) {
        return (float) (value - Math.floor(value));
    }

    private static float wrappedRadians(double time, double speed, double phase) {
        return (float) ((time * speed + phase) % (Math.PI * 2.0D));
    }

    private static float sinTime(double time, double speed, double phase) {
        return Mth.sin(wrappedRadians(time, speed, phase));
    }

    private static int essenceColor(ItemStack result) {
        if (result.isEmpty()) {
            return DEFAULT_ESSENCE_COLOR;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (!"betterenchanting".equals(id.getNamespace())) {
            return DEFAULT_ESSENCE_COLOR;
        }
        return switch (id.getPath()) {
            case "fire_essence" -> 0xFF8A2A;
            case "frost_essence" -> 0x6EE7FF;
            case "lightning_essence" -> 0xFFE75E;
            case "physical_essence" -> 0xFF6464;
            case "mining_essence" -> 0x55D66B;
            case "defensive_essence" -> 0x5B8CFF;
            case "vitality_essence" -> 0xFF75B7;
            case "mobility_essence" -> 0xF2F2F2;
            case "void_essence" -> 0x8F55FF;
            case "purification_essence" -> 0xFFFFFF;
            default -> DEFAULT_ESSENCE_COLOR;
        };
    }

    private static int blendColor(int from, int to, float amount) {
        float blend = Mth.clamp(amount, 0.0F, 1.0F);
        int red = Math.round(Mth.lerp(blend, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int green = Math.round(Mth.lerp(blend, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int blue = Math.round(Mth.lerp(blend, from & 0xFF, to & 0xFF));
        return red << 16 | green << 8 | blue;
    }

    private static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    @Override
    public AABB getRenderBoundingBox(ArcaneCrucibleBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1.5D, 2.0D, 1.5D);
    }

    private static final class AnimationState {
        private final MachineItemAnimation items = new MachineItemAnimation(ArcaneCrucibleBlockEntity.CONTAINER_SIZE);
        private ItemStack formingResult = ItemStack.EMPTY;
        private float progress;
        private float predictedProgress;
        private double completionHoldTicks;
        private boolean completing;
        private int lastOutputCount;
        private int lastSyncedProgress = -1;

        private void advance(ArcaneCrucibleBlockEntity crucible, ItemStack recipeResult, double time) {
            double delta = this.items.advance(crucible, time);
            int syncedProgress = crucible.progress();
            int outputCount = crucible.getItem(ArcaneCrucibleBlockEntity.OUTPUT_SLOT).getCount();
            boolean outputIncreased = outputCount > this.lastOutputCount;
            if (!recipeResult.isEmpty()) {
                this.formingResult = recipeResult.copy();
            }

            if (this.lastSyncedProgress < 0) {
                this.predictedProgress = crucible.progressFraction();
                this.progress = this.predictedProgress;
            } else if (syncedProgress == 0 && this.lastSyncedProgress > 0) {
                if (outputIncreased
                        && this.lastSyncedProgress >= ArcaneCrucibleBlockEntity.DISTILLATION_TIME - 8) {
                    this.predictedProgress = 1.0F;
                    this.progress = 1.0F;
                    this.completionHoldTicks = 2.5D;
                    this.completing = true;
                    this.items.hide(ArcaneCrucibleBlockEntity.MEDIUM_SLOT);
                    for (int index = 0; index < ArcaneCrucibleBlockEntity.CATALYST_SLOT_COUNT; index++) {
                        this.items.hide(ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + index);
                    }
                } else {
                    this.predictedProgress = 0.0F;
                    this.completionHoldTicks = 0.0D;
                }
            } else if (syncedProgress > 0) {
                float synchronizedFraction = crucible.progressFraction();
                if (synchronizedFraction + 0.08F < this.predictedProgress) {
                    this.predictedProgress = synchronizedFraction;
                } else {
                    this.predictedProgress = Math.max(this.predictedProgress, synchronizedFraction);
                }
            }

            if (this.completing) {
                this.completionHoldTicks = Math.max(0.0D, this.completionHoldTicks - delta);
                this.predictedProgress = 1.0F;
                this.progress = 1.0F;
                if (this.completionHoldTicks <= 0.0D) {
                    this.completing = false;
                    this.predictedProgress = 0.0F;
                    this.progress = 0.0F;
                    this.formingResult = ItemStack.EMPTY;
                }
                this.lastOutputCount = outputCount;
                this.lastSyncedProgress = syncedProgress;
                return;
            }

            if (syncedProgress > 0) {
                this.predictedProgress = Math.min(
                        1.0F,
                        this.predictedProgress + (float) (delta / ArcaneCrucibleBlockEntity.DISTILLATION_TIME)
                );
            } else {
                this.predictedProgress = 0.0F;
            }

            float blend = MachineItemAnimation.smoothing(delta, syncedProgress > 0 ? 1.10D : 0.82D);
            this.progress = Mth.lerp(blend, this.progress, this.predictedProgress);
            if (syncedProgress == 0 && this.completionHoldTicks <= 0.0D && this.progress < 0.01F) {
                this.progress = 0.0F;
                this.formingResult = ItemStack.EMPTY;
            }
            this.lastOutputCount = outputCount;
            this.lastSyncedProgress = syncedProgress;
        }

        private ItemStack formingResult() {
            return this.formingResult;
        }
    }
}
