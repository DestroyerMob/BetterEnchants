package com.betterenchanting.client;

import com.betterenchanting.compat.MobsToolForgingCompat;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedEnchantmentBreakdown;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedPartBreakdown;
import com.betterenchanting.network.SelectPedestalUpgradePayload;
import com.betterenchanting.registry.ModItems;
import com.betterenchanting.world.inventory.PedestalUpgradeRules.UpgradePlan;
import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Screen-space pedestal interaction using the same visual language as the Attunement Lens. */
public final class PedestalUpgradeScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 470;
    private static final int PANEL_MAX_HEIGHT = 280;
    private static final int HEADER_HEIGHT = 42;
    private static final int DETAILS_HEIGHT = 76;

    private final BlockPos pedestalPos;
    private int selectedPartIndex = Integer.MIN_VALUE;
    private int invalidTicks;
    private int actionCooldown;
    private List<PartButton> partButtons = List.of();
    private List<EnchantmentButton> enchantmentButtons = List.of();
    private EnchantmentChoice detailChoice;

    public PedestalUpgradeScreen(BlockPos pedestalPos) {
        super(Component.translatable("gui.betterenchanting.pedestal.screen.title"));
        this.pedestalPos = pedestalPos.immutable();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.actionCooldown > 0) {
            this.actionCooldown--;
        }
        if (currentPedestal().filter(pedestal -> !pedestal.target().isEmpty()).isPresent()
                && playerInRange() && holdingFocus()) {
            this.invalidTicks = 0;
        } else if (++this.invalidTicks > 10) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        Optional<AttunementPedestalBlockEntity> current = currentPedestal();
        if (current.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.betterenchanting.pedestal.screen.unavailable"),
                    this.width / 2, this.height / 2, AttunementUiTheme.MUTED_COLOR);
            return;
        }

        AttunementPedestalBlockEntity pedestal = current.get();
        List<PartChoice> choices = partChoices(pedestal);
        if (choices.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.betterenchanting.pedestal.screen.no_enchantments"),
                    this.width / 2, this.height / 2, AttunementUiTheme.MUTED_COLOR);
            return;
        }
        ensureSelection(pedestal, choices);

        int panelWidth = Math.min(PANEL_MAX_WIDTH, this.width - 20);
        int panelHeight = Math.min(PANEL_MAX_HEIGHT, this.height - 20);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int headerBottom = top + HEADER_HEIGHT;
        int detailsTop = bottom - DETAILS_HEIGHT;
        int partWidth = Math.min(142, Math.max(108, panelWidth / 3));
        int dividerX = left + partWidth;

        graphics.fill(left, top, right, bottom, AttunementUiTheme.PANEL_FILL);
        graphics.renderOutline(left, top, panelWidth, panelHeight, AttunementUiTheme.PANEL_BORDER);
        graphics.fill(left + 1, headerBottom, right - 1, headerBottom + 1, 0x806F4B89);
        graphics.fill(dividerX, headerBottom + 1, dividerX + 1, detailsTop, 0x806F4B89);
        graphics.fill(left + 1, detailsTop, right - 1, detailsTop + 1, 0x806F4B89);

        graphics.renderItem(pedestal.target(), left + 12, top + 12);
        graphics.drawString(this.font, this.title, left + 36, top + 9,
                AttunementUiTheme.TEXT_COLOR, false);
        graphics.drawString(this.font,
                Component.translatable("gui.betterenchanting.pedestal.screen.subtitle"),
                left + 36, top + 22, AttunementUiTheme.MUTED_COLOR, false);

        renderParts(graphics, choices, left + 7, headerBottom + 7,
                partWidth - 14, detailsTop - headerBottom - 14, mouseX, mouseY);
        renderEnchantments(graphics, pedestal, selectedChoice(choices), dividerX + 9, headerBottom + 7,
                right - dividerX - 16, detailsTop - headerBottom - 14, mouseX, mouseY);
        renderDetails(graphics, pedestal, this.detailChoice, left + 8, detailsTop + 7,
                panelWidth - 16, DETAILS_HEIGHT - 14);
    }

    private void renderParts(GuiGraphics graphics, List<PartChoice> choices, int x, int y,
                             int width, int height, int mouseX, int mouseY) {
        List<PartButton> buttons = new ArrayList<>();
        int rowHeight = Math.min(34, Math.max(24, height / Math.max(1, choices.size())));
        for (int index = 0; index < choices.size(); index++) {
            PartChoice choice = choices.get(index);
            int rowY = y + index * rowHeight;
            if (rowY + rowHeight > y + height) {
                break;
            }
            boolean selected = choice.partIndex() == this.selectedPartIndex;
            boolean hovered = contains(mouseX, mouseY, x, rowY, width, rowHeight - 2);
            graphics.fill(x, rowY, x + width, rowY + rowHeight - 2,
                    selected ? AttunementUiTheme.ROW_SELECTED
                            : hovered ? AttunementUiTheme.ROW_HOVER : AttunementUiTheme.ROW_FILL);
            if (selected) {
                graphics.fill(x, rowY, x + 3, rowY + rowHeight - 2, AttunementUiTheme.ACTIVE_COLOR);
            }
            graphics.renderItem(choice.stack(), x + 6, rowY + Math.max(3, (rowHeight - 18) / 2));
            graphics.drawString(this.font, truncate(choice.name(), width - 36), x + 28,
                    rowY + Math.max(7, (rowHeight - 9) / 2), AttunementUiTheme.TEXT_COLOR, false);
            buttons.add(new PartButton(x, rowY, width, rowHeight - 2, choice.partIndex()));
        }
        this.partButtons = List.copyOf(buttons);
    }

    private void renderEnchantments(GuiGraphics graphics, AttunementPedestalBlockEntity pedestal,
                                     PartChoice part, int x, int y, int width, int height,
                                     int mouseX, int mouseY) {
        graphics.fill(x, y, x + width, y + height, AttunementUiTheme.SECTION_FILL);
        graphics.drawString(this.font, part.name(), x + 8, y + 7, AttunementUiTheme.TEXT_COLOR, false);
        int listTop = y + 22;
        int listHeight = height - 26;
        int rowHeight = Math.min(34, Math.max(24, listHeight / Math.max(1, part.enchantments().size())));
        List<EnchantmentButton> buttons = new ArrayList<>();
        EnchantmentChoice hoveredChoice = null;
        EnchantmentChoice selectedChoice = null;
        for (int index = 0; index < part.enchantments().size(); index++) {
            EnchantmentChoice choice = part.enchantments().get(index);
            int rowY = listTop + index * rowHeight;
            if (rowY + rowHeight > y + height - 2) {
                break;
            }
            UpgradePlan plan = pedestal.previewPlan(part.partIndex(), choice.id());
            boolean selected = choice.id().equals(pedestal.selectedEnchantment())
                    && part.partIndex() == pedestal.selectedPartIndex();
            boolean hovered = contains(mouseX, mouseY, x + 5, rowY, width - 10, rowHeight - 2);
            int rowColor = selected ? AttunementUiTheme.ROW_SELECTED
                    : hovered ? AttunementUiTheme.ROW_HOVER : AttunementUiTheme.ROW_FILL;
            graphics.fill(x + 5, rowY, x + width - 5, rowY + rowHeight - 2, rowColor);
            int accent = plan.maximumReached() ? AttunementUiTheme.DORMANT_COLOR
                    : plan.canUpgrade() ? AttunementUiTheme.READY_COLOR : AttunementUiTheme.ACTIVE_COLOR;
            graphics.fill(x + 5, rowY, x + 8, rowY + rowHeight - 2, accent);

            Component name = Enchantment.getFullname(choice.enchantment(), choice.level());
            graphics.drawString(this.font, truncate(name, width - 96), x + 14, rowY + 5,
                    AttunementUiTheme.TEXT_COLOR, false);
            Component progression = plan.maximumReached()
                    ? Component.translatable("gui.betterenchanting.pedestal.orb.maximum")
                    : Component.translatable("gui.betterenchanting.pedestal.orb.progression",
                    plan.currentLevel(), plan.nextLevel());
            if (rowHeight >= 29) {
                graphics.drawString(this.font, truncate(progression, width - 96), x + 14, rowY + 17,
                        accent, false);
            }
            if (plan.validSelection()) {
                ItemStack essence = requiredEssence(plan);
                graphics.renderItem(essence, x + width - 43, rowY + Math.max(2, (rowHeight - 18) / 2));
                String count = plan.availableEssence() + "/" + plan.essenceCost();
                graphics.drawString(this.font, count, x + width - 25,
                        rowY + Math.max(7, (rowHeight - 9) / 2),
                        plan.enoughEssence() ? AttunementUiTheme.READY_COLOR : AttunementUiTheme.ERROR_COLOR,
                        false);
            }
            EnchantmentChoice planned = choice.withPlan(plan);
            if (hovered) {
                hoveredChoice = planned;
            }
            if (selected) {
                selectedChoice = planned;
            }
            buttons.add(new EnchantmentButton(x + 5, rowY, width - 10, rowHeight - 2,
                    part.partIndex(), choice.id(), selected, plan.canUpgrade()));
        }
        this.enchantmentButtons = List.copyOf(buttons);
        this.detailChoice = hoveredChoice != null ? hoveredChoice
                : selectedChoice != null ? selectedChoice
                : part.enchantments().isEmpty() ? null
                : part.enchantments().getFirst().withPlan(
                pedestal.previewPlan(part.partIndex(), part.enchantments().getFirst().id()));
    }

    private void renderDetails(GuiGraphics graphics, AttunementPedestalBlockEntity pedestal,
                               EnchantmentChoice choice, int x, int y, int width, int height) {
        if (choice == null || choice.plan() == null || !choice.plan().validSelection()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.betterenchanting.pedestal.screen.choose"),
                    x + width / 2, y + height / 2 - 4, AttunementUiTheme.MUTED_COLOR);
            return;
        }
        UpgradePlan plan = choice.plan();
        ItemStack essence = requiredEssence(plan);
        graphics.renderItem(essence, x + 2, y + 3);
        int missing = Math.max(0, plan.essenceCost() - plan.availableEssence());
        Component essenceLine = Component.translatable(
                "gui.betterenchanting.pedestal.screen.essence_requirement",
                essence.getHoverName(), plan.availableEssence(), plan.essenceCost(), missing);
        graphics.drawString(this.font, truncate(essenceLine, width - 30), x + 24, y + 2,
                plan.enoughEssence() ? AttunementUiTheme.READY_COLOR : AttunementUiTheme.ERROR_COLOR, false);
        Component power = Component.translatable("gui.betterenchanting.pedestal.screen.power_requirement",
                plan.availablePower(), plan.requiredPower());
        graphics.drawString(this.font, power, x + 24, y + 14,
                plan.enoughPower() ? AttunementUiTheme.READY_COLOR : AttunementUiTheme.ERROR_COLOR, false);
        if (plan.catalystRequired()) {
            Component catalyst = Component.translatable("gui.betterenchanting.pedestal.screen.catalyst_requirement");
            graphics.drawString(this.font, catalyst, x + 24, y + 26,
                    plan.hasCatalyst() ? AttunementUiTheme.READY_COLOR : AttunementUiTheme.BONUS_COLOR, false);
        }
        boolean selected = choice.id().equals(pedestal.selectedEnchantment())
                && choice.partIndex() == pedestal.selectedPartIndex();
        Component action = actionText(plan, selected);
        graphics.drawString(this.font, truncate(action, width - 4), x + 2, y + height - 11,
                selected && plan.canUpgrade() ? AttunementUiTheme.READY_COLOR : AttunementUiTheme.MUTED_COLOR,
                false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (PartButton part : this.partButtons) {
                if (part.contains(mouseX, mouseY)) {
                    this.selectedPartIndex = part.partIndex();
                    return true;
                }
            }
            if (this.actionCooldown <= 0) {
                for (EnchantmentButton enchantment : this.enchantmentButtons) {
                    if (enchantment.contains(mouseX, mouseY)
                            && (!enchantment.selected() || enchantment.canUpgrade())) {
                        PacketDistributor.sendToServer(new SelectPedestalUpgradePayload(
                                this.pedestalPos, enchantment.partIndex(), enchantment.enchantmentId()));
                        this.actionCooldown = 4;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Optional<AttunementPedestalBlockEntity> currentPedestal() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return Optional.empty();
        }
        return this.minecraft.level.getBlockEntity(this.pedestalPos) instanceof AttunementPedestalBlockEntity pedestal
                ? Optional.of(pedestal)
                : Optional.empty();
    }

    private boolean playerInRange() {
        return this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.distanceToSqr(Vec3.atCenterOf(this.pedestalPos)) <= 64.0D;
    }

    private boolean holdingFocus() {
        return this.minecraft != null && this.minecraft.player != null
                && (this.minecraft.player.getMainHandItem().is(ModItems.ATTUNEMENT_FOCUS.get())
                || this.minecraft.player.getOffhandItem().is(ModItems.ATTUNEMENT_FOCUS.get()));
    }

    private static List<PartChoice> partChoices(AttunementPedestalBlockEntity pedestal) {
        ItemStack target = pedestal.target();
        Optional<RoutedEnchantmentBreakdown> routed = MobsToolForgingCompat.routedEnchantmentBreakdown(
                pedestal.getLevel().registryAccess(), target);
        if (routed.isPresent()) {
            List<PartChoice> choices = new ArrayList<>();
            for (RoutedPartBreakdown part : routed.get().parts()) {
                List<EnchantmentChoice> enchantments = part.enchantments().stream()
                        .map(state -> new EnchantmentChoice(part.partIndex(), state.enchantment(),
                                state.enchantmentId(), state.level(), null))
                        .toList();
                if (!enchantments.isEmpty()) {
                    choices.add(new PartChoice(part.partIndex(), part.partStack(),
                            Component.literal(readableSlot(part.slotId())), enchantments));
                }
            }
            if (!choices.isEmpty()) {
                return List.copyOf(choices);
            }
        }

        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(target);
        List<EnchantmentChoice> entries = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            ResourceLocation id = entry.getKey().unwrapKey().map(ResourceKey::location).orElse(null);
            if (id != null) {
                entries.add(new EnchantmentChoice(-1, entry.getKey(), id, entry.getIntValue(), null));
            }
        }
        return entries.isEmpty() ? List.of() : List.of(new PartChoice(
                -1, target, Component.translatable("gui.betterenchanting.tuning.final_tool"), List.copyOf(entries)));
    }

    private void ensureSelection(AttunementPedestalBlockEntity pedestal, List<PartChoice> choices) {
        if (this.selectedPartIndex == Integer.MIN_VALUE
                && choices.stream().anyMatch(choice -> choice.partIndex() == pedestal.selectedPartIndex())) {
            this.selectedPartIndex = pedestal.selectedPartIndex();
        }
        if (choices.stream().noneMatch(choice -> choice.partIndex() == this.selectedPartIndex)) {
            this.selectedPartIndex = choices.getFirst().partIndex();
        }
    }

    private PartChoice selectedChoice(List<PartChoice> choices) {
        return choices.stream().filter(choice -> choice.partIndex() == this.selectedPartIndex)
                .findFirst().orElse(choices.getFirst());
    }

    private String truncate(Component text, int width) {
        return this.font.plainSubstrByWidth(text.getString(), Math.max(1, width));
    }

    private static ItemStack requiredEssence(UpgradePlan plan) {
        return plan.requiredEssenceItem().equals(ResourceLocation.withDefaultNamespace("air"))
                ? ItemStack.EMPTY
                : new ItemStack(BuiltInRegistries.ITEM.get(plan.requiredEssenceItem()));
    }

    private static Component actionText(UpgradePlan plan, boolean selected) {
        if (plan.maximumReached()) {
            return Component.translatable("gui.betterenchanting.pedestal.orb.maximum");
        }
        if (!selected) {
            return Component.translatable("gui.betterenchanting.pedestal.screen.select");
        }
        if (plan.canUpgrade()) {
            return Component.translatable("gui.betterenchanting.pedestal.screen.upgrade");
        }
        return Component.translatable("gui.betterenchanting.pedestal.screen.supply_requirements");
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

    private record PartChoice(int partIndex, ItemStack stack, Component name,
                              List<EnchantmentChoice> enchantments) {
    }

    private record EnchantmentChoice(int partIndex, Holder<Enchantment> enchantment, ResourceLocation id,
                                     int level, UpgradePlan plan) {
        private EnchantmentChoice withPlan(UpgradePlan plan) {
            return new EnchantmentChoice(this.partIndex, this.enchantment, this.id, this.level, plan);
        }
    }

    private record PartButton(int x, int y, int width, int height, int partIndex) {
        private boolean contains(double mouseX, double mouseY) {
            return PedestalUpgradeScreen.contains(mouseX, mouseY, x, y, width, height);
        }
    }

    private record EnchantmentButton(int x, int y, int width, int height, int partIndex,
                                     ResourceLocation enchantmentId, boolean selected, boolean canUpgrade) {
        private boolean contains(double mouseX, double mouseY) {
            return PedestalUpgradeScreen.contains(mouseX, mouseY, x, y, width, height);
        }
    }
}
