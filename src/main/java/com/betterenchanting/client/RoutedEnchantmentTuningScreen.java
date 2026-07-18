package com.betterenchanting.client;

import com.betterenchanting.compat.MobsToolForgingCompat;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedEnchantmentBreakdown;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedEnchantmentState;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedPartBreakdown;
import com.betterenchanting.compat.MobsToolForgingCompat.StationRoutedEnchantmentPreview;
import com.betterenchanting.network.PromoteRoutedEnchantmentPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** A screen-space tuning lens for routed modular-tool enchantments. */
public final class RoutedEnchantmentTuningScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 430;
    private static final int PANEL_MAX_HEIGHT = 238;
    private static final int PANEL_FILL = AttunementUiTheme.PANEL_FILL;
    private static final int PANEL_BORDER = AttunementUiTheme.PANEL_BORDER;
    private static final int SECTION_FILL = AttunementUiTheme.SECTION_FILL;
    private static final int ROW_FILL = AttunementUiTheme.ROW_FILL;
    private static final int ROW_HOVER = AttunementUiTheme.ROW_HOVER;
    private static final int ROW_SELECTED = AttunementUiTheme.ROW_SELECTED;
    private static final int ACTIVE_COLOR = AttunementUiTheme.ACTIVE_COLOR;
    private static final int DORMANT_COLOR = AttunementUiTheme.DORMANT_COLOR;
    private static final int BONUS_COLOR = AttunementUiTheme.BONUS_COLOR;
    private static final int TEXT_COLOR = AttunementUiTheme.TEXT_COLOR;
    private static final int MUTED_COLOR = AttunementUiTheme.MUTED_COLOR;

    private final BlockPos stationPos;
    private int selectedPartIndex = Integer.MIN_VALUE;
    private int invalidTicks;
    private int actionCooldown;
    private List<PartButton> partButtons = List.of();
    private List<EnchantmentButton> enchantmentButtons = List.of();

    public RoutedEnchantmentTuningScreen(BlockPos stationPos) {
        super(Component.translatable("gui.betterenchanting.tuning.title"));
        this.stationPos = stationPos.immutable();
    }

    @Override
    protected void init() {
        super.init();
        rebuildFocusControls(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.actionCooldown > 0) {
            this.actionCooldown--;
        }
        if (currentPreview().isPresent() && playerInRange()) {
            this.invalidTicks = 0;
        } else if (++this.invalidTicks > 10) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        Optional<PreviewState> current = currentPreview();
        if (current.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.betterenchanting.tuning.unavailable"),
                    this.width / 2,
                    this.height / 2,
                    MUTED_COLOR
            );
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        PreviewState state = current.get();
        List<PartChoice> choices = partChoices(state.preview(), state.breakdown());
        ensureSelection(choices);

        int panelWidth = Math.min(PANEL_MAX_WIDTH, this.width - 20);
        int panelHeight = Math.min(PANEL_MAX_HEIGHT, this.height - 20);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int headerBottom = top + 42;
        int footerTop = bottom - 22;
        int partWidth = Math.min(136, Math.max(104, panelWidth / 3));
        int dividerX = left + partWidth;

        graphics.fill(left, top, right, bottom, PANEL_FILL);
        graphics.renderOutline(left, top, panelWidth, panelHeight, PANEL_BORDER);
        graphics.fill(left + 1, headerBottom, right - 1, headerBottom + 1, 0x806F4B89);
        graphics.fill(dividerX, headerBottom + 1, dividerX + 1, footerTop, 0x806F4B89);
        graphics.fill(left + 1, footerTop, right - 1, footerTop + 1, 0x806F4B89);

        graphics.renderItem(state.preview().toolStack(), left + 12, top + 12);
        graphics.drawString(this.font, this.title, left + 36, top + 9, TEXT_COLOR, false);
        graphics.drawString(
                this.font,
                Component.translatable("gui.betterenchanting.tuning.subtitle"),
                left + 36,
                top + 22,
                MUTED_COLOR,
                false
        );

        renderPartChoices(graphics, choices, left + 7, headerBottom + 7, partWidth - 14, footerTop - headerBottom - 14, mouseX, mouseY);
        renderEnchantments(
                graphics,
                selectedChoice(choices),
                dividerX + 9,
                headerBottom + 7,
                right - dividerX - 16,
                footerTop - headerBottom - 14,
                mouseX,
                mouseY
        );

        String footer = this.font.plainSubstrByWidth(
                Component.translatable("gui.betterenchanting.tuning.footer").getString(),
                panelWidth - 16
        );
        graphics.drawCenteredString(this.font, footer, this.width / 2, footerTop + 7, MUTED_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPartChoices(
            GuiGraphics graphics,
            List<PartChoice> choices,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY
    ) {
        List<PartButton> buttons = new ArrayList<>();
        int rowHeight = Math.min(34, Math.max(22, height / Math.max(1, choices.size())));
        for (int index = 0; index < choices.size(); index++) {
            PartChoice choice = choices.get(index);
            int rowY = y + index * rowHeight;
            if (rowY + rowHeight > y + height) {
                break;
            }
            boolean selected = choice.partIndex() == this.selectedPartIndex;
            boolean hovered = contains(mouseX, mouseY, x, rowY, width, rowHeight - 2);
            graphics.fill(x, rowY, x + width, rowY + rowHeight - 2,
                    selected ? ROW_SELECTED : hovered ? ROW_HOVER : ROW_FILL);
            if (selected) {
                graphics.fill(x, rowY, x + 3, rowY + rowHeight - 2, ACTIVE_COLOR);
            }
            int itemY = rowY + Math.max(2, (rowHeight - 18) / 2);
            graphics.renderItem(choice.stack(), x + 6, itemY);
            int textX = x + 28;
            int textWidth = width - 34;
            graphics.drawString(this.font, truncate(choice.name(), textWidth), textX, rowY + 5, TEXT_COLOR, false);
            if (rowHeight >= 29) {
                graphics.drawString(this.font, truncate(choice.detail(), textWidth), textX, rowY + 17, MUTED_COLOR, false);
            }
            buttons.add(new PartButton(x, rowY, width, rowHeight - 2, choice.partIndex()));
        }
        this.partButtons = List.copyOf(buttons);
    }

    private void renderEnchantments(
            GuiGraphics graphics,
            PartChoice choice,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(x, y, x + width, y + height, SECTION_FILL);
        graphics.drawString(this.font, choice.name(), x + 8, y + 7, TEXT_COLOR, false);
        graphics.drawString(this.font, truncate(choice.detail(), width - 16), x + 8, y + 19, MUTED_COLOR, false);

        List<RoutedEnchantmentState> enchantments = choice.enchantments();
        if (enchantments.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.betterenchanting.tuning.no_enchantments"),
                    x + width / 2,
                    y + height / 2,
                    MUTED_COLOR
            );
            this.enchantmentButtons = List.of();
            return;
        }

        List<EnchantmentButton> buttons = new ArrayList<>();
        int listTop = y + 34;
        int listHeight = height - 38;
        int rowHeight = Math.min(31, Math.max(22, listHeight / enchantments.size()));
        for (int index = 0; index < enchantments.size(); index++) {
            RoutedEnchantmentState enchantment = enchantments.get(index);
            int rowY = listTop + index * rowHeight;
            if (rowY + rowHeight > y + height - 2) {
                break;
            }
            boolean actionable = canUse(enchantment);
            boolean hovered = contains(mouseX, mouseY, x + 5, rowY, width - 10, rowHeight - 2);
            graphics.fill(x + 5, rowY, x + width - 5, rowY + rowHeight - 2, hovered ? ROW_HOVER : ROW_FILL);

            int color = enchantment.overlevelBonusActive()
                    ? BONUS_COLOR
                    : enchantment.active() ? ACTIVE_COLOR : DORMANT_COLOR;
            int orbX = x + 16;
            int orbY = rowY + rowHeight / 2 - 1;
            renderOrb(graphics, orbX, orbY, color, hovered);

            int displayLevel = enchantment.effectiveLevel() > 0
                    ? enchantment.effectiveLevel()
                    : enchantment.level();
            Component name = net.minecraft.world.item.enchantment.Enchantment.getFullname(
                    enchantment.enchantment(),
                    displayLevel
            );
            int actionWidth = width >= 220 ? 80 : 0;
            int nameWidth = width - 42 - actionWidth;
            graphics.drawString(this.font, truncate(name, nameWidth), x + 30, rowY + 5, TEXT_COLOR, false);
            if (rowHeight >= 29) {
                graphics.drawString(this.font, truncate(stateText(enchantment), nameWidth), x + 30, rowY + 17, color, false);
            }
            if (actionWidth > 0) {
                Component action = actionText(enchantment);
                graphics.drawString(
                        this.font,
                        truncate(action, actionWidth - 8),
                        x + width - actionWidth,
                        rowY + Math.max(5, (rowHeight - 9) / 2),
                        actionable ? TEXT_COLOR : MUTED_COLOR,
                        false
                );
            }
            buttons.add(new EnchantmentButton(
                    x + 5,
                    rowY,
                    width - 10,
                    rowHeight - 2,
                    choice.partIndex(),
                    enchantment.enchantmentId(),
                    actionable
            ));
        }
        this.enchantmentButtons = List.copyOf(buttons);
    }

    private static void renderOrb(GuiGraphics graphics, int x, int y, int color, boolean hovered) {
        int outer = hovered ? 6 : 5;
        int glow = color & 0x00FFFFFF | 0x50000000;
        graphics.fill(x - outer, y - 1, x + outer + 1, y + 2, glow);
        graphics.fill(x - 1, y - outer, x + 2, y + outer + 1, glow);
        graphics.fill(x - 4, y - 1, x + 5, y + 2, color);
        graphics.fill(x - 1, y - 4, x + 2, y + 5, color);
        graphics.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (PartButton part : this.partButtons) {
                if (part.contains(mouseX, mouseY)) {
                    this.selectedPartIndex = part.partIndex();
                    rebuildFocusControls(true);
                    return true;
                }
            }
            if (this.actionCooldown <= 0) {
                for (EnchantmentButton enchantment : this.enchantmentButtons) {
                    if (enchantment.actionable() && enchantment.contains(mouseX, mouseY)) {
                        Vec3 effectPosition = Vec3.atCenterOf(this.stationPos).add(0.0D, 1.0D, 0.0D);
                        PacketDistributor.sendToServer(new PromoteRoutedEnchantmentPayload(
                                this.stationPos,
                                enchantment.partIndex(),
                                enchantment.enchantmentId(),
                                effectPosition.x,
                                effectPosition.y,
                                effectPosition.z
                        ));
                        this.actionCooldown = 5;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void rebuildFocusControls(boolean focusEnchantments) {
        this.clearWidgets();
        Optional<PreviewState> current = currentPreview();
        if (current.isEmpty()) {
            return;
        }
        List<PartChoice> choices = partChoices(current.get().preview(), current.get().breakdown());
        if (choices.isEmpty()) {
            return;
        }
        ensureSelection(choices);

        int panelWidth = Math.min(PANEL_MAX_WIDTH, this.width - 20);
        int panelHeight = Math.min(PANEL_MAX_HEIGHT, this.height - 20);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int headerBottom = top + 42;
        int footerTop = bottom - 22;
        int partWidth = Math.min(136, Math.max(104, panelWidth / 3));
        int dividerX = left + partWidth;
        int partX = left + 7;
        int partY = headerBottom + 7;
        int partHeight = footerTop - headerBottom - 14;
        int partRowHeight = Math.min(34, Math.max(22, partHeight / Math.max(1, choices.size())));

        ControllerFocusButton firstPart = null;
        for (int index = 0; index < choices.size(); index++) {
            PartChoice choice = choices.get(index);
            int rowY = partY + index * partRowHeight;
            if (rowY + partRowHeight > partY + partHeight) {
                break;
            }
            ControllerFocusButton button = this.addRenderableWidget(new ControllerFocusButton(
                    partX, rowY, partWidth - 14, partRowHeight - 2, choice.name(), () -> {
                        this.selectedPartIndex = choice.partIndex();
                        rebuildFocusControls(true);
                    }
            ));
            if (firstPart == null) {
                firstPart = button;
            }
        }

        PartChoice selected = selectedChoice(choices);
        int enchantX = dividerX + 14;
        int enchantY = headerBottom + 41;
        int enchantWidth = right - dividerX - 26;
        int enchantHeight = footerTop - headerBottom - 52;
        int enchantRowHeight = Math.min(31, Math.max(22,
                enchantHeight / Math.max(1, selected.enchantments().size())));
        ControllerFocusButton firstEnchantment = null;
        for (int index = 0; index < selected.enchantments().size(); index++) {
            RoutedEnchantmentState enchantment = selected.enchantments().get(index);
            int rowY = enchantY + index * enchantRowHeight;
            if (rowY + enchantRowHeight > footerTop - 2) {
                break;
            }
            ControllerFocusButton button = this.addRenderableWidget(new ControllerFocusButton(
                    enchantX, rowY, enchantWidth, enchantRowHeight - 2,
                    net.minecraft.world.item.enchantment.Enchantment.getFullname(
                            enchantment.enchantment(),
                            enchantment.effectiveLevel() > 0 ? enchantment.effectiveLevel() : enchantment.level()),
                    () -> activateEnchantment(selected.partIndex(), enchantment.enchantmentId())
            ));
            button.active = canUse(enchantment);
            if (firstEnchantment == null && button.active) {
                firstEnchantment = button;
            }
        }

        ControllerFocusButton initial = focusEnchantments && firstEnchantment != null
                ? firstEnchantment : firstPart;
        if (initial != null) {
            this.setInitialFocus(initial);
        }
    }

    private void activateEnchantment(int partIndex, ResourceLocation enchantmentId) {
        if (this.actionCooldown > 0) {
            return;
        }
        Vec3 effectPosition = Vec3.atCenterOf(this.stationPos).add(0.0D, 1.0D, 0.0D);
        PacketDistributor.sendToServer(new PromoteRoutedEnchantmentPayload(
                this.stationPos, partIndex, enchantmentId,
                effectPosition.x, effectPosition.y, effectPosition.z));
        this.actionCooldown = 5;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Optional<PreviewState> currentPreview() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return Optional.empty();
        }
        return MobsToolForgingCompat.stationRoutedEnchantmentPreview(this.minecraft.level, this.stationPos)
                .flatMap(preview -> preview.breakdown().map(breakdown -> new PreviewState(preview, breakdown)));
    }

    private boolean playerInRange() {
        return this.minecraft != null
                && this.minecraft.player != null
                && this.minecraft.player.distanceToSqr(Vec3.atCenterOf(this.stationPos)) <= 64.0D;
    }

    private static List<PartChoice> partChoices(
            StationRoutedEnchantmentPreview preview,
            RoutedEnchantmentBreakdown breakdown
    ) {
        List<PartChoice> choices = new ArrayList<>();
        long finalActive = breakdown.toolEnchantments().stream().filter(RoutedEnchantmentState::active).count();
        choices.add(new PartChoice(
                -1,
                preview.toolStack(),
                Component.translatable("gui.betterenchanting.tuning.final_tool"),
                Component.translatable(
                        "gui.betterenchanting.tuning.active_count",
                        finalActive,
                        breakdown.toolEnchantments().size()
                ),
                breakdown.toolEnchantments()
        ));
        for (RoutedPartBreakdown part : breakdown.parts()) {
            long active = part.enchantments().stream().filter(RoutedEnchantmentState::active).count();
            choices.add(new PartChoice(
                    part.partIndex(),
                    part.partStack(),
                    Component.literal(readableSlot(part.slotId())),
                    Component.translatable("gui.betterenchanting.tuning.part_capacity", active, part.limit()),
                    part.enchantments()
            ));
        }
        return List.copyOf(choices);
    }

    private void ensureSelection(List<PartChoice> choices) {
        if (choices.stream().anyMatch(choice -> choice.partIndex() == this.selectedPartIndex)) {
            return;
        }
        this.selectedPartIndex = choices.stream()
                .filter(choice -> choice.enchantments().stream().anyMatch(enchantment -> !enchantment.active()))
                .mapToInt(PartChoice::partIndex)
                .findFirst()
                .orElse(choices.getFirst().partIndex());
    }

    private PartChoice selectedChoice(List<PartChoice> choices) {
        return choices.stream()
                .filter(choice -> choice.partIndex() == this.selectedPartIndex)
                .findFirst()
                .orElse(choices.getFirst());
    }

    private String truncate(Component text, int width) {
        return this.font.plainSubstrByWidth(text.getString(), Math.max(1, width));
    }

    private static Component stateText(RoutedEnchantmentState enchantment) {
        if (enchantment.overlevelBonusActive()) {
            return Component.translatable("gui.betterenchanting.tuning.state.bonus");
        }
        if (enchantment.active()) {
            return Component.translatable("gui.betterenchanting.tuning.state.active");
        }
        return Component.translatable("gui.betterenchanting.tuning.state.dormant");
    }

    private static Component actionText(RoutedEnchantmentState enchantment) {
        if (!enchantment.active()) {
            return Component.translatable("gui.betterenchanting.tuning.action.prioritize");
        }
        if (enchantment.overleveled() && !enchantment.overlevelBonusActive()) {
            return Component.translatable("gui.betterenchanting.tuning.action.focus_bonus");
        }
        return Component.translatable("gui.betterenchanting.tuning.action.active");
    }

    private static boolean canUse(RoutedEnchantmentState enchantment) {
        return !enchantment.active()
                || enchantment.overleveled() && !enchantment.overlevelBonusActive();
    }

    private static String readableSlot(String slotId) {
        if (slotId == null || slotId.isBlank()) {
            return "Part";
        }
        StringBuilder result = new StringBuilder();
        for (String word : slotId.replace('_', ' ').split(" ")) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.isEmpty() ? "Part" : result.toString();
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record PreviewState(
            StationRoutedEnchantmentPreview preview,
            RoutedEnchantmentBreakdown breakdown
    ) {
    }

    private record PartChoice(
            int partIndex,
            ItemStack stack,
            Component name,
            Component detail,
            List<RoutedEnchantmentState> enchantments
    ) {
    }

    private record PartButton(int x, int y, int width, int height, int partIndex) {
        private boolean contains(double mouseX, double mouseY) {
            return RoutedEnchantmentTuningScreen.contains(mouseX, mouseY, x, y, width, height);
        }
    }

    private record EnchantmentButton(
            int x,
            int y,
            int width,
            int height,
            int partIndex,
            ResourceLocation enchantmentId,
            boolean actionable
    ) {
        private boolean contains(double mouseX, double mouseY) {
            return RoutedEnchantmentTuningScreen.contains(mouseX, mouseY, x, y, width, height);
        }
    }
}
