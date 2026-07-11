package com.betterenchanting.client;

import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.network.InteractiveEnchantingStatePayload;
import com.betterenchanting.network.InteractiveEnchantingStatePayload.Clue;
import com.betterenchanting.network.InteractiveEnchantingStatePayload.Option;
import com.betterenchanting.network.RequestInteractiveEnchantingStatePayload;
import com.betterenchanting.network.SelectInteractiveEnchantingOptionPayload;
import com.betterenchanting.world.inventory.EnhancedEnchantingMenu;
import com.betterenchanting.world.level.block.EnchantingTableStorage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Renders and operates server-authoritative enhanced-enchanting offers in the world. */
public final class InteractiveEnchantingOverlay {
    private static final int SCAN_RADIUS = 8;
    private static final int VERTICAL_RADIUS = 4;
    private static final long REQUEST_INTERVAL = 10L;
    private static final long STATE_TTL = 40L;
    private static final double MAX_PICK_DISTANCE = 8.0D;
    private static final double PICK_RADIUS = 0.40D;
    private static final int MYSTERY_COLOR = 0xFF756B91;
    private static final int DISABLED_COLOR = 0xFF55515D;
    private static final RenderType ORBS = RenderType.create(
            "betterenchanting_interactive_options",
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

    private static final Map<BlockPos, TimedState> STATES = new HashMap<>();
    private static final Map<BlockPos, Long> LAST_REQUESTS = new HashMap<>();
    private static ClientLevel cachedLevel;
    private static BlockPos cachedOrigin = BlockPos.ZERO;
    private static long lastScan = Long.MIN_VALUE;
    private static List<EnchantingTableBlockEntity> cachedTables = List.of();
    private static List<Orb> lastOrbs = List.of();
    private static Optional<Orb> hovered = Optional.empty();

    private InteractiveEnchantingOverlay() {
    }

    public static void acceptState(InteractiveEnchantingStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            resetForLevel(minecraft.level);
            STATES.put(payload.tablePos(), new TimedState(payload, minecraft.level.getGameTime()));
        }
    }

    public static boolean hasAvailableOffer(BlockPos tablePos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        resetForLevel(minecraft.level);
        TimedState state = STATES.get(tablePos);
        if (state == null || minecraft.level.getGameTime() - state.receivedTick() > STATE_TTL) {
            return false;
        }
        return state.payload().options().stream()
                .anyMatch(option -> option.available(state.payload().reagentCount()));
    }

    public static void ensureState(BlockPos tablePos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            resetForLevel(minecraft.level);
            requestState(tablePos, minecraft.level.getGameTime());
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canDisplay(minecraft)) {
            lastOrbs = List.of();
            hovered = Optional.empty();
            return;
        }
        resetForLevel(minecraft.level);
        long tick = minecraft.level.getGameTime();
        STATES.entrySet().removeIf(entry -> tick - entry.getValue().receivedTick() > STATE_TTL);

        List<Orb> orbs = new ArrayList<>();
        for (EnchantingTableBlockEntity table : nearbyTables(minecraft)) {
            ensureState(table.getBlockPos());
            TimedState state = STATES.get(table.getBlockPos());
            if (state != null) {
                layout(table.getBlockPos(), state.payload(), event.getCamera(), tick, orbs);
            }
        }
        lastOrbs = List.copyOf(orbs);
        hovered = pick(orbs, minecraft);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(ORBS);
        double renderTime = tick + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (Orb orb : orbs) {
            boolean selected = hovered.filter(orb::sameTarget).isPresent();
            int color = selected ? 0xFFFFFFFF : orb.color();
            double pulse = 1.0D + Math.sin(renderTime * 0.20D + orb.option() * 2.1D) * 0.09D;
            drawDiamond(event.getPoseStack(), event.getCamera(), consumer, orb.position(),
                    0.145D * pulse, color, 0.94F);
            drawDiamond(event.getPoseStack(), event.getCamera(), consumer, orb.position(),
                    0.23D * pulse, color, selected ? 0.28F : 0.16F);
        }
        buffers.endBatch(ORBS);
    }

    public static void renderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!canDisplay(minecraft) || hovered.isEmpty()) {
            return;
        }
        Orb orb = hovered.get();
        Component name = clueName(minecraft, orb.state());
        Component clues = clueSummary(minecraft, orb.state());
        Component power = Component.translatable(
                "gui.betterenchanting.interactive.power",
                orb.option() + 1,
                orb.state().requirement()
        ).withStyle(ChatFormatting.GRAY);
        Component action = action(orb);

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = Math.max(font.width(name), Math.max(font.width(clues),
                Math.max(font.width(power), font.width(action)))) + 22;
        width = Math.max(144, Math.min(width, minecraft.getWindow().getGuiScaledWidth() - 16));
        int x = Math.max(8, Math.min(minecraft.getWindow().getGuiScaledWidth() - width - 8,
                minecraft.getWindow().getGuiScaledWidth() / 2 + 14));
        int y = Math.max(8, Math.min(minecraft.getWindow().getGuiScaledHeight() - 58,
                minecraft.getWindow().getGuiScaledHeight() / 2 + 12));
        graphics.fill(x, y, x + width, y + 52, 0xD0100E17);
        graphics.renderOutline(x, y, width, 52, orb.color());
        graphics.fill(x + 6, y + 7, x + 10, y + 45, orb.color());
        graphics.drawString(font, name, x + 15, y + 5, 0xFFF2EAF7, true);
        graphics.drawString(font, clues, x + 15, y + 17, 0xFFBDB1C7, false);
        graphics.drawString(font, power, x + 15, y + 29, 0xFF918A99, false);
        graphics.drawString(font, action, x + 15, y + 41,
                orb.available() ? 0xFFB8F29F : 0xFFE18D8D, false);
    }

    public static void handleInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!canDisplay(minecraft) || !minecraft.player.getMainHandItem().isEmpty()) {
            return;
        }
        Optional<Orb> hit = pick(lastOrbs, minecraft);
        if (hit.isEmpty()) {
            return;
        }
        Orb orb = hit.get();
        if (orb.available()) {
            PacketDistributor.sendToServer(new SelectInteractiveEnchantingOptionPayload(orb.tablePos(), orb.option()));
        }
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    private static void layout(BlockPos pos, InteractiveEnchantingStatePayload state, Camera camera,
                               long tick, List<Orb> output) {
        Vec3 anchor = Vec3.atCenterOf(pos).add(0.0D, 1.72D, 0.0D);
        Vec3 right = rightVector(anchor, camera.getPosition());
        for (int option = 0; option < state.options().size(); option++) {
            Option optionState = state.options().get(option);
            double offset = (option - 1.0D) * 0.64D;
            double bob = Math.sin(tick * 0.10D + option * 2.0D) * 0.04D;
            Vec3 position = anchor.add(right.scale(offset)).add(0.0D, bob - Math.abs(offset) * 0.08D, 0.0D);
            boolean available = optionState.available(state.reagentCount());
            int color = available ? clueColor(optionState, Minecraft.getInstance()) : DISABLED_COLOR;
            output.add(new Orb(pos, option, optionState, state.reagentCount(), position, color, available));
        }
    }

    private static int clueColor(Option option, Minecraft minecraft) {
        if (option.clues().isEmpty() || minecraft.level == null) {
            return MYSTERY_COLOR;
        }
        return minecraft.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(option.clues().getFirst().enchantmentId())
                .map(ClientTooltipEvents::dominantAffinityColor)
                .map(TextColor::getValue)
                .map(rgb -> 0xFF000000 | rgb)
                .orElse(MYSTERY_COLOR);
    }

    private static Component clueName(Minecraft minecraft, Option option) {
        if (option.clues().isEmpty() || minecraft.level == null) {
            return Component.translatable("gui.betterenchanting.interactive.mystery").withStyle(ChatFormatting.DARK_PURPLE);
        }
        Clue clue = option.clues().getFirst();
        return minecraft.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(clue.enchantmentId())
                .<Component>map(holder -> coloredName(holder, clue.level()))
                .orElseGet(() -> Component.translatable("gui.betterenchanting.interactive.mystery"));
    }

    private static Component clueSummary(Minecraft minecraft, Option option) {
        if (option.clues().isEmpty() || minecraft.level == null) {
            return Component.translatable("tooltip.betterenchanting.option.no_clue").withStyle(ChatFormatting.DARK_RED);
        }
        MutableComponent names = Component.empty();
        for (int index = 0; index < option.clues().size(); index++) {
            if (index > 0) {
                names.append(Component.literal(", "));
            }
            Clue clue = option.clues().get(index);
            minecraft.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(clue.enchantmentId())
                    .ifPresent(holder -> names.append(coloredName(holder, clue.level())));
        }
        return Component.translatable(
                option.allCluesRevealed()
                        ? "gui.betterenchanting.interactive.clues_all"
                        : "gui.betterenchanting.interactive.clues",
                names
        ).withStyle(ChatFormatting.GRAY);
    }

    private static Component coloredName(Holder<Enchantment> enchantment, int level) {
        return Enchantment.getFullname(enchantment, level).copy()
                .withStyle(style -> style.withColor(ClientTooltipEvents.dominantAffinityColor(enchantment)));
    }

    private static Component action(Orb orb) {
        if (orb.available()) {
            return Component.translatable("gui.betterenchanting.interactive.enchant").withStyle(ChatFormatting.GREEN);
        }
        if (orb.reagentCount() <= 0) {
            return Component.translatable("gui.betterenchanting.interactive.need_reagent").withStyle(ChatFormatting.RED);
        }
        return Component.translatable("gui.betterenchanting.interactive.unavailable").withStyle(ChatFormatting.RED);
    }

    private static List<EnchantingTableBlockEntity> nearbyTables(Minecraft minecraft) {
        BlockPos origin = minecraft.player.blockPosition();
        long tick = minecraft.level.getGameTime();
        if (origin.equals(cachedOrigin) && tick - lastScan < 5L) {
            return cachedTables;
        }
        List<EnchantingTableBlockEntity> result = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -VERTICAL_RADIUS; y <= VERTICAL_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (minecraft.level.getBlockEntity(cursor) instanceof EnchantingTableBlockEntity table
                            && table instanceof EnchantingTableStorage storage) {
                        ItemStack target = storage.betterenchanting$getEnchantingInventory()
                                .getItem(EnhancedEnchantingMenu.TARGET_SLOT);
                        if (!target.isEmpty()) {
                            result.add(table);
                        }
                    }
                }
            }
        }
        cachedOrigin = origin.immutable();
        lastScan = tick;
        cachedTables = List.copyOf(result);
        return cachedTables;
    }

    private static void requestState(BlockPos pos, long tick) {
        Long previous = LAST_REQUESTS.get(pos);
        if (previous == null || tick - previous >= REQUEST_INTERVAL) {
            LAST_REQUESTS.put(pos.immutable(), tick);
            PacketDistributor.sendToServer(new RequestInteractiveEnchantingStatePayload(pos));
        }
    }

    private static Optional<Orb> pick(List<Orb> orbs, Minecraft minecraft) {
        if (minecraft.player == null) {
            return Optional.empty();
        }
        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        Orb best = null;
        double bestAlong = Double.MAX_VALUE;
        for (Orb orb : orbs) {
            Vec3 delta = orb.position().subtract(origin);
            double along = delta.dot(look);
            if (along < 0.25D || along > MAX_PICK_DISTANCE) {
                continue;
            }
            if (delta.subtract(look.scale(along)).length() <= PICK_RADIUS && along < bestAlong) {
                best = orb;
                bestAlong = along;
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean canDisplay(Minecraft minecraft) {
        return BetterEnchantingConfig.usesInteractiveEnchanting()
                && EffectiveBalance.takesOverEnchantingTable()
                && minecraft.player != null && minecraft.level != null && minecraft.screen == null
                && !minecraft.options.hideGui;
    }

    private static Vec3 rightVector(Vec3 anchor, Vec3 camera) {
        Vec3 toCamera = camera.subtract(anchor);
        Vec3 right = new Vec3(toCamera.z, 0.0D, -toCamera.x);
        return right.lengthSqr() < 0.001D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
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

    private static void resetForLevel(ClientLevel level) {
        if (cachedLevel != level) {
            cachedLevel = level;
            STATES.clear();
            LAST_REQUESTS.clear();
            lastOrbs = List.of();
            hovered = Optional.empty();
            cachedOrigin = BlockPos.ZERO;
            lastScan = Long.MIN_VALUE;
            cachedTables = List.of();
        }
    }

    private record TimedState(InteractiveEnchantingStatePayload payload, long receivedTick) {
    }

    private record Orb(BlockPos tablePos, int option, Option state, int reagentCount, Vec3 position,
                       int color, boolean available) {
        private boolean sameTarget(Orb other) {
            return this.tablePos.equals(other.tablePos) && this.option == other.option;
        }
    }
}
