package com.betterenchanting.client;

import com.betterenchanting.world.CrucibleRoller;
import com.betterenchanting.world.inventory.ArcaneCrucibleMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

public class ArcaneCrucibleScreen extends AbstractContainerScreen<ArcaneCrucibleMenu> {
    private static final int PANEL = 0xFF2E2A32;
    private static final int PANEL_DARK = 0xFF19171D;
    private static final int PANEL_LIGHT = 0xFF4B4652;
    private static final int SLOT = 0xFF1F1C23;
    private static final int SLOT_BORDER = 0xFF746B7D;
    private static final int OPTION = 0xFF26343D;
    private static final int OPTION_HOVER = 0xFF345666;
    private static final int OPTION_DISABLED = 0xFF222226;
    private static final int TEXT = 0xFFE7E2D2;
    private static final int MUTED = 0xFFB0A7B6;
    private static final int GOLD = 0xFFFFD66D;

    private static final int OPTION_X = 126;
    private static final int OPTION_Y = 24;
    private static final int OPTION_WIDTH = 92;
    private static final int OPTION_HEIGHT = 20;

    public ArcaneCrucibleScreen(ArcaneCrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 236;
        this.imageHeight = 212;
        this.titleLabelX = 10;
        this.titleLabelY = 8;
        this.inventoryLabelX = 38;
        this.inventoryLabelY = 115;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int option = 0; option < 3; option++) {
            if (this.isHovering(OPTION_X, OPTION_Y + option * 24, OPTION_WIDTH, OPTION_HEIGHT, mouseX, mouseY)
                    && this.minecraft != null
                    && this.minecraft.player != null
                    && this.menu.clickMenuButton(this.minecraft.player, option)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, option);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);
        guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 14, PANEL_DARK);
        guiGraphics.renderOutline(x, y, this.imageWidth, this.imageHeight, PANEL_LIGHT);

        drawSlot(guiGraphics, x + 18, y + 38);
        drawSlot(guiGraphics, x + 18, y + 66);
        for (int slot = 0; slot < ArcaneCrucibleMenu.ESSENCE_SLOT_COUNT; slot++) {
            drawSlot(guiGraphics, x + 54 + slot * 22, y + 28);
        }
        for (int slot = 0; slot < ArcaneCrucibleMenu.BOOK_SLOT_COUNT; slot++) {
            drawSlot(guiGraphics, x + 66 + slot * 22, y + 66);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(guiGraphics, x + 38 + column * 18, y + 126 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(guiGraphics, x + 38 + column * 18, y + 184);
        }

        drawInputLabels(guiGraphics, x, y);
        drawOptions(guiGraphics, x, y, mouseX, mouseY);
        drawSummary(guiGraphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, MUTED, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderOptionTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawInputLabels(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(this.font, Component.translatable("gui.betterenchanting.target"), x + 14, y + 27, MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.betterenchanting.lapis"), x + 16, y + 86, MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.betterenchanting.essences"), x + 54, y + 17, MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.betterenchanting.books"), x + 68, y + 86, MUTED, false);
    }

    private void drawOptions(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        for (int option = 0; option < 3; option++) {
            int optionY = y + OPTION_Y + option * 24;
            boolean active = this.menu.costs[option] > 0 && this.menu.enchantClue[option] >= 0;
            boolean hovered = mouseX >= x + OPTION_X && mouseX < x + OPTION_X + OPTION_WIDTH && mouseY >= optionY && mouseY < optionY + OPTION_HEIGHT;
            int color = !active ? OPTION_DISABLED : hovered ? OPTION_HOVER : OPTION;
            guiGraphics.fill(x + OPTION_X, optionY, x + OPTION_X + OPTION_WIDTH, optionY + OPTION_HEIGHT, color);
            guiGraphics.renderOutline(x + OPTION_X, optionY, OPTION_WIDTH, OPTION_HEIGHT, active ? GOLD : PANEL_LIGHT);

            String cost = active ? Integer.toString(this.menu.costs[option]) : "-";
            guiGraphics.drawString(this.font, Component.translatable("gui.betterenchanting.option", option + 1), x + OPTION_X + 5, optionY + 3, TEXT, false);
            guiGraphics.drawString(this.font, cost, x + OPTION_X + OPTION_WIDTH - 5 - this.font.width(cost), optionY + 3, active ? GOLD : MUTED, false);
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("gui.betterenchanting.pool", this.menu.getPoolSize(option)),
                    x + OPTION_X + 5,
                    optionY + 12,
                    active ? MUTED : 0xFF77737A,
                    false
            );
        }
    }

    private void drawSummary(GuiGraphics guiGraphics, int x, int y) {
        CrucibleRoller.InputProfile profile = this.menu.clientProfile();
        String tags = clipped(CrucibleRoller.tagSummary(profile.essenceTags()), 126);
        String targetTags = clipped(CrucibleRoller.tagSummary(profile.targetTags()), 126);
        Component mode = this.menu.isRestricted()
                ? Component.translatable("gui.betterenchanting.mode.restricted")
                : Component.translatable("gui.betterenchanting.mode.weighted");

        guiGraphics.drawString(this.font, Component.translatable("gui.betterenchanting.active_tags", tags), x + 54, y + 100, TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.betterenchanting.target_tags", targetTags), x + 54, y + 109, MUTED, false);
        guiGraphics.drawString(this.font, mode, x + 126, y + 100, this.menu.isRestricted() ? GOLD : MUTED, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.betterenchanting.book_boosts", this.menu.getBookBoostCount()), x + 126, y + 109, MUTED, false);
    }

    private void renderOptionTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        for (int option = 0; option < 3; option++) {
            if (!this.isHovering(OPTION_X, OPTION_Y + option * 24, OPTION_WIDTH, OPTION_HEIGHT, mouseX, mouseY) || this.menu.costs[option] <= 0) {
                continue;
            }

            Optional<Holder.Reference<Enchantment>> clue = this.minecraft.level
                    .registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(this.menu.enchantClue[option]);
            int clueLevel = this.menu.levelClue[option];
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(
                    "container.enchant.clue",
                    clue.<Component>map(holder -> Enchantment.getFullname(holder, clueLevel)).orElse(CommonComponents.EMPTY)
            ).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("tooltip.betterenchanting.pool_size", this.menu.getPoolSize(option)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.betterenchanting.tags", this.menu.getActiveTagCount()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.betterenchanting.books", this.menu.getBookBoostCount()).withStyle(ChatFormatting.GRAY));
            if (this.minecraft.player != null && this.minecraft.player.experienceLevel < this.menu.costs[option]) {
                tooltip.add(Component.translatable("container.enchant.level.requirement", this.menu.costs[option]).withStyle(ChatFormatting.RED));
            }
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            return;
        }
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
        guiGraphics.fill(x, y, x + 16, y + 16, SLOT);
    }

    private String clipped(String value, int width) {
        if (this.font.width(value) <= width) {
            return value;
        }
        return this.font.plainSubstrByWidth(value, width - this.font.width("...")) + "...";
    }
}
