package com.betterenchanting.client;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.world.inventory.EnhancedEnchantingMenu;
import com.betterenchanting.world.inventory.EnhancedEnchantingMenu.OptionDetails;
import com.betterenchanting.world.EnchantingRoller;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class EnhancedEnchantingScreen extends AbstractContainerScreen<EnhancedEnchantingMenu> {
    private static final int EXTENDED_IMAGE_WIDTH = 201;
    private static final int APOTHIC_IMAGE_HEIGHT = 197;
    private static final int APOTHIC_STAT_BAR_WIDTH = 110;
    private static final int APOTHIC_STAT_BAR_MAX = 100;
    private static final int STATUS_X = 60;
    private static final int STATUS_WIDTH = 108;
    private static final int STATUS_Y = 72;
    private static final int APOTHIC_STATUS_Y = 105;
    private static final ResourceLocation[] ENABLED_LEVEL_SPRITES = new ResourceLocation[]{
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1"),
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2"),
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3")
    };
    private static final ResourceLocation[] DISABLED_LEVEL_SPRITES = new ResourceLocation[]{
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1_disabled"),
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2_disabled"),
            ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3_disabled")
    };
    private static final ResourceLocation ENCHANTMENT_SLOT_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace(
            "container/enchanting_table/enchantment_slot_disabled"
    );
    private static final ResourceLocation ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace(
            "container/enchanting_table/enchantment_slot_highlighted"
    );
    private static final ResourceLocation ENCHANTMENT_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/enchanting_table/enchantment_slot");
    private static final ResourceLocation ENCHANTING_TABLE_LOCATION = BetterEnchanting.id("textures/gui/container/enchanting_table.png");
    private static final ResourceLocation APOTHIC_ENCHANTING_TABLE_LOCATION = BetterEnchanting.id("textures/gui/container/enchanting_table_apothic.png");
    private static final ResourceLocation ENCHANTING_BOOK_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/enchanting_table_book.png");

    private final RandomSource random = RandomSource.create();
    private BookModel bookModel;
    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    private ItemStack last = ItemStack.EMPTY;
    private float apothicEterna;
    private float lastApothicEterna;
    private float apothicQuanta;
    private float lastApothicQuanta;
    private float apothicArcana;
    private float lastApothicArcana;

    public EnhancedEnchantingScreen(EnhancedEnchantingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = EXTENDED_IMAGE_WIDTH;
        if (menu.usesApothicLayout()) {
            this.imageHeight = APOTHIC_IMAGE_HEIGHT;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.tickBook();
        this.tickApothicBars();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        for (int option = 0; option < 3; option++) {
            double optionX = mouseX - (double) (x + 60);
            double optionY = mouseY - (double) (y + 14 + 19 * option);
            if (optionX >= 0.0D
                    && optionY >= 0.0D
                    && optionX < 108.0D
                    && optionY < 19.0D
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
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        ResourceLocation texture = this.enchantingTableTexture();
        guiGraphics.blit(texture, x, y, 0, 0, this.imageWidth, this.imageHeight);
        this.renderBook(guiGraphics, x, y, partialTick);
        EnchantmentNames.getInstance().initSeed((long) this.menu.getEnchantmentSeed());
        int reagentCount = this.menu.getReagentCount();
        int reagentCost = this.menu.getReagentCost();

        for (int option = 0; option < 3; option++) {
            int optionX = x + 60;
            int textX = optionX + 20;
            int requiredLevel = this.menu.requirements[option];
            int displayCost = this.menu.costs[option];
            if (requiredLevel == 0 || displayCost == 0) {
                this.renderEnchantmentSlot(guiGraphics, texture, optionX, y + 14 + 19 * option, option, displayCost, true, false);
            } else {
                int textWidth = 86;
                FormattedText randomName = EnchantmentNames.getInstance().getRandomName(this.font, textWidth);
                int textColor = 6839882;
                boolean disabled = reagentCost > 0 && reagentCount < reagentCost
                        || this.menu.enchantClue[option] == -1
                        || this.menu.getDisabledReasonFlags(option) != 0;

                if (disabled) {
                    this.renderEnchantmentSlot(guiGraphics, texture, optionX, y + 14 + 19 * option, option, displayCost, true, true);
                    guiGraphics.drawWordWrap(this.font, randomName, textX, y + 16 + 19 * option, textWidth, (textColor & 16711422) >> 1);
                    textColor = 4226832;
                } else {
                    int hoverX = mouseX - (x + 60);
                    int hoverY = mouseY - (y + 14 + 19 * option);
                    boolean highlighted = hoverX >= 0 && hoverY >= 0 && hoverX < 108 && hoverY < 19;
                    this.renderEnchantmentSlot(guiGraphics, texture, optionX, y + 14 + 19 * option, option, displayCost, false, true, highlighted);
                    if (highlighted) {
                        textColor = 16777088;
                    }
                    guiGraphics.drawWordWrap(this.font, randomName, textX, y + 16 + 19 * option, textWidth, textColor);
                    textColor = 8453920;
                }

            }
        }

        this.renderApothicStats(guiGraphics, texture, x, y);
    }

    private void renderEnchantmentSlot(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int option,
            int displayCost,
            boolean disabled,
            boolean renderLevel
    ) {
        this.renderEnchantmentSlot(guiGraphics, texture, x, y, option, displayCost, disabled, renderLevel, false);
    }

    private void renderEnchantmentSlot(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int option,
            int displayCost,
            boolean disabled,
            boolean renderLevel,
            boolean highlighted
    ) {
        RenderSystem.enableBlend();
        if (this.menu.usesApothicLayout()) {
            int slotV = disabled ? 218 : highlighted ? 237 : 199;
            guiGraphics.blit(texture, x, y, 148, slotV, 108, 19);
            if (renderLevel) {
                int iconV = disabled ? 239 : 223;
                int iconU = 16 * (Mth.clamp(displayCost, 1, 3) - 1);
                guiGraphics.blit(texture, x + 1, y + 1, iconU, iconV, 16, 16);
            }
        } else {
            if (disabled) {
                guiGraphics.blitSprite(ENCHANTMENT_SLOT_DISABLED_SPRITE, x, y, 108, 19);
                if (renderLevel) {
                    guiGraphics.blitSprite(levelSprite(displayCost, true), x + 1, y + 1, 16, 16);
                }
            } else {
                guiGraphics.blitSprite(highlighted ? ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE : ENCHANTMENT_SLOT_SPRITE, x, y, 108, 19);
                if (renderLevel) {
                    guiGraphics.blitSprite(levelSprite(displayCost, false), x + 1, y + 1, 16, 16);
                }
            }
        }
        RenderSystem.disableBlend();
    }

    private void renderApothicStats(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        if (!this.menu.usesApothicLayout()) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
        if (this.apothicEterna > 0) {
            guiGraphics.blit(texture, x + 59, y + 75, 0, 197, apothicBarLength(this.apothicEterna), 5);
        }
        if (this.apothicQuanta > 0) {
            guiGraphics.blit(texture, x + 59, y + 85, 0, 202, apothicBarLength(this.apothicQuanta), 5);
        }
        if (this.apothicArcana > 0) {
            guiGraphics.blit(texture, x + 59, y + 95, 0, 207, apothicBarLength(this.apothicArcana), 5);
        }
    }

    private static int apothicBarLength(float stat) {
        return Mth.clamp((int) (stat / APOTHIC_STAT_BAR_MAX * APOTHIC_STAT_BAR_WIDTH), 0, APOTHIC_STAT_BAR_WIDTH);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.usesApothicLayout()) {
            super.renderLabels(guiGraphics, mouseX, mouseY);
            this.renderStatusLine(guiGraphics, STATUS_Y);
            return;
        }
        guiGraphics.drawString(this.font, this.title, 12, 5, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 7, this.imageHeight - 96 + 4, 4210752, false);
        guiGraphics.drawString(this.font, Component.literal("Eterna"), 19, 74, 0x3DB53D, false);
        guiGraphics.drawString(this.font, Component.literal("Quanta"), 19, 84, 0xFC5454, false);
        guiGraphics.drawString(this.font, Component.literal("Arcana"), 19, 94, 0xA800A8, false);
        this.renderStatusLine(guiGraphics, APOTHIC_STATUS_Y);
    }

    private void renderStatusLine(GuiGraphics guiGraphics, int y) {
        FeedbackLine line = this.statusLine();
        if (line == null) {
            return;
        }
        String text = this.font.plainSubstrByWidth(line.text().getString(), STATUS_WIDTH);
        int x = STATUS_X + (STATUS_WIDTH - this.font.width(text)) / 2;
        guiGraphics.drawString(this.font, text, x, y, line.color(), false);
    }

    private void renderBook(GuiGraphics guiGraphics, int x, int y, float partialTick) {
        float openProgress = Mth.lerp(partialTick, this.oOpen, this.open);
        float flipProgress = Mth.lerp(partialTick, this.oFlip, this.flip);
        Lighting.setupForEntityInInventory();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float) x + 33.0F, (float) y + 31.0F, 100.0F);
        guiGraphics.pose().scale(-40.0F, 40.0F, 40.0F);
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(25.0F));
        guiGraphics.pose().translate((1.0F - openProgress) * 0.2F, (1.0F - openProgress) * 0.1F, (1.0F - openProgress) * 0.25F);
        float rotation = -(1.0F - openProgress) * 90.0F - 90.0F;
        guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(rotation));
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(180.0F));
        float leftPageFlip = Mth.clamp(Mth.frac(flipProgress + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float rightPageFlip = Mth.clamp(Mth.frac(flipProgress + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        this.bookModel.setupAnim(0.0F, leftPageFlip, rightPageFlip, openProgress);
        VertexConsumer vertexConsumer = guiGraphics.bufferSource().getBuffer(this.bookModel.renderType(ENCHANTING_BOOK_LOCATION));
        this.bookModel.renderToBuffer(guiGraphics.pose(), vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY);
        guiGraphics.flush();
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderOptionTooltip(guiGraphics, mouseX, mouseY);
        this.renderSlotHelpTooltip(guiGraphics, mouseX, mouseY);
        this.renderApothicStatTooltip(guiGraphics, mouseX, mouseY);
    }

    private ResourceLocation enchantingTableTexture() {
        return this.menu.usesApothicLayout() ? APOTHIC_ENCHANTING_TABLE_LOCATION : ENCHANTING_TABLE_LOCATION;
    }

    private void renderOptionTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
            return;
        }

        boolean creative = this.minecraft.player.getAbilities().instabuild;
        int reagentCount = this.menu.getReagentCount();
        int reagentCost = this.menu.getReagentCost();

        for (int option = 0; option < 3; option++) {
            int requiredLevel = this.menu.requirements[option];
            int displayCost = this.menu.costs[option];
            if (this.isHovering(60, 14 + 19 * option, 108, 17, mouseX, mouseY)) {
                Optional<Holder.Reference<Enchantment>> clue = this.minecraft
                        .level
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolder(this.menu.enchantClue[option]);
                int clueLevel = this.menu.levelClue[option];
                List<Component> tooltip = Lists.newArrayList();
                boolean hasOfferPower = requiredLevel > 0 && displayCost > 0;
                OptionDetails details = this.menu.getOptionDetails(option);
                if (hasOfferPower) {
                    boolean specialOffer = this.menu.isApothicInfusionOffer(option) || this.menu.isOverlevelOffer(option);
                    if (this.menu.usesApothicLayout() && !specialOffer) {
                        if (!this.addRevealedClueLines(tooltip, option)) {
                            tooltip.add(Component.translatable("tooltip.betterenchanting.option.no_clue").withStyle(ChatFormatting.DARK_RED, ChatFormatting.UNDERLINE));
                        }
                    } else {
                        Component clueName = clue.<Component>map(holder -> coloredClueName(holder, clueLevel)).orElse(CommonComponents.EMPTY);
                        String clueKey = "container.enchant.clue";
                        if (this.menu.isApothicInfusionOffer(option)) {
                            clueKey = "tooltip.betterenchanting.option.infusion_clue";
                        } else if (this.menu.isOverlevelOffer(option)) {
                            clueKey = "tooltip.betterenchanting.option.overlevel_clue";
                        }
                        tooltip.add(Component.translatable(clueKey, clueName).withStyle(ChatFormatting.WHITE));
                    }
                } else {
                    tooltip.add(Component.translatable("tooltip.betterenchanting.option.unavailable").withStyle(ChatFormatting.WHITE));
                }

                addDisabledReasonLines(tooltip, option, details, requiredLevel, displayCost, reagentCost, reagentCount, creative, clue.isEmpty());

                if (hasOfferPower) {
                    tooltip.add(CommonComponents.EMPTY);
                    tooltip.add(Component.translatable(
                            "tooltip.betterenchanting.option.power_status",
                            requiredLevel
                    ).withStyle(ChatFormatting.GRAY));
                    if (clue.isPresent()) {
                        tooltip.add(Component.translatable(
                                creative
                                        ? "tooltip.betterenchanting.option.reagent.creative"
                                        : "tooltip.betterenchanting.option.reagent",
                                details.reagent().getHoverName()
                        ).withStyle(reagentCount >= reagentCost ? ChatFormatting.GRAY : ChatFormatting.RED));
                    }
                }

                tooltip.add(CommonComponents.EMPTY);
                this.addSystemTooltipLines(tooltip, option, details);
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    private boolean addRevealedClueLines(List<Component> tooltip, int option) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return false;
        }
        List<Component> clueLines = Lists.newArrayList();
        for (int clueIndex = 0; clueIndex < this.menu.getRevealedClueCount(option); clueIndex++) {
            int clueId = this.menu.getRevealedClueId(option, clueIndex);
            int clueLevel = this.menu.getRevealedClueLevel(option, clueIndex);
            this.minecraft
                    .level
                    .registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(clueId)
                    .ifPresent(holder -> clueLines.add(coloredClueName(holder, clueLevel)));
        }
        if (clueLines.isEmpty()) {
            return false;
        }

        tooltip.add(Component.translatable(
                this.menu.areAllCluesRevealed(option)
                        ? "tooltip.betterenchanting.option.clues_all"
                        : "tooltip.betterenchanting.option.clues"
        ).withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE));
        tooltip.addAll(clueLines);
        return true;
    }

    private void renderApothicStatTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.usesApothicLayout()) {
            return;
        }
        if (this.isHovering(60, 14 + 19 * 3 + 5, 110, 5, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    statLine("Eterna", this.menu.getApothicEterna(), ChatFormatting.GREEN),
                    Component.literal("Raises the base enchanting power from nearby shelves.").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        } else if (this.isHovering(60, 14 + 19 * 3 + 15, 110, 5, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    statLine("Quanta", this.menu.getApothicQuanta(), ChatFormatting.RED),
                    Component.literal(this.menu.isApothicStable() ? "Stable: rolls upward without negative variance." : "Adds controlled variance to the final roll power.").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        } else if (this.isHovering(60, 14 + 19 * 3 + 25, 110, 5, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    statLine("Arcana", this.menu.getApothicArcana(), ChatFormatting.DARK_PURPLE),
                    Component.literal("Biases the enchantment pool toward rarer and extra selections.").withStyle(ChatFormatting.GRAY),
                    Component.literal(this.menu.allowsApothicTreasure() ? "Rare enchantment shelves are active." : "Rare enchantment shelves are inactive.").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
    }

    private void renderSlotHelpTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (this.isHovering(15, 47, 18, 18, mouseX, mouseY) && !this.menu.getSlot(EnhancedEnchantingMenu.TARGET_SLOT).hasItem()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.betterenchanting.slot.target").withStyle(ChatFormatting.WHITE),
                    Component.translatable("tooltip.betterenchanting.slot.target.help").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
            return;
        }
        if (this.isHovering(35, 47, 18, 18, mouseX, mouseY) && !this.menu.getSlot(EnhancedEnchantingMenu.LAPIS_SLOT).hasItem()) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.betterenchanting.slot.reagent").withStyle(ChatFormatting.WHITE),
                    Component.translatable("tooltip.betterenchanting.slot.reagent.help").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
            return;
        }
        for (int slot = 0; slot < EnhancedEnchantingMenu.MODIFIER_SLOT_COUNT; slot++) {
            int menuSlot = EnhancedEnchantingMenu.FIRST_MODIFIER_SLOT + slot;
            if (this.isHovering(176, 15 + slot * 19, 18, 18, mouseX, mouseY) && !this.menu.getSlot(menuSlot).hasItem()) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.translatable("tooltip.betterenchanting.slot.modifier").withStyle(ChatFormatting.WHITE),
                        Component.translatable("tooltip.betterenchanting.slot.modifier.essence").withStyle(ChatFormatting.GRAY),
                        Component.translatable("tooltip.betterenchanting.slot.modifier.book").withStyle(ChatFormatting.GRAY),
                        Component.translatable("tooltip.betterenchanting.slot.modifier.overlevel").withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return;
            }
        }
    }

    private static Component statLine(String name, float value, ChatFormatting color) {
        return Component.literal(name + ": " + String.format(Locale.ROOT, "%.2f", value)).withStyle(color);
    }

    private void addSystemTooltipLines(List<Component> tooltip, int option) {
        addSystemTooltipLines(tooltip, option, this.menu.getOptionDetails(option));
    }

    private void addSystemTooltipLines(List<Component> tooltip, int option, OptionDetails details) {
        if (this.menu.isApothicInfusionOffer(option)) {
            tooltip.add(detailLine(
                    "tooltip.betterenchanting.option.mode",
                    Component.translatable("tooltip.betterenchanting.option.mode.infusion")
            ));
            return;
        }
        Component mode = this.menu.isOverlevelOffer(option)
                ? Component.translatable("tooltip.betterenchanting.option.mode.overlevel")
                : Component.translatable(
                        details.profile().restricted()
                                ? "tooltip.betterenchanting.option.mode.restricted"
                                : "tooltip.betterenchanting.option.mode.weighted"
                );
        if (!details.reagent().isEmpty()) {
            tooltip.add(detailLine("tooltip.betterenchanting.option.reagent_item", details.reagent().getHoverName()));
        }
        tooltip.add(detailLine(
                "tooltip.betterenchanting.option.mode",
                mode
        ));
        tooltip.add(detailLine(
                "tooltip.betterenchanting.option.active_tags",
                details.profile().essenceTags().isEmpty()
                        ? Component.translatable("tooltip.betterenchanting.none")
                        : Component.literal(EnchantingRoller.tagSummary(details.profile().essenceTags()))
        ));
        tooltip.add(detailLine(
                "tooltip.betterenchanting.option.target_tags",
                details.profile().targetTags().isEmpty()
                        ? Component.translatable("tooltip.betterenchanting.none")
                        : Component.literal(EnchantingRoller.tagSummary(details.profile().targetTags()))
        ));
        if (!details.directModifier().isEmpty()) {
            tooltip.add(detailLine("tooltip.betterenchanting.option.direct_modifier", details.directModifier().getHoverName()));
        }
        if (!details.blockingModifier().isEmpty()) {
            tooltip.add(detailLine("tooltip.betterenchanting.option.blocking_modifier", details.blockingModifier().getHoverName()));
        }
        if (!details.globalModifiers().isEmpty()) {
            tooltip.add(detailLine("tooltip.betterenchanting.option.global_modifier", joinedItemNames(details.globalModifiers())));
        }

        Component bookBoosts = joinedBookBoosts(details.bookModifiers());
        if (bookBoosts != null) {
            tooltip.add(detailLine("tooltip.betterenchanting.option.book_boost", bookBoosts));
        }
        tooltip.add(detailLine("tooltip.betterenchanting.option.pool", Component.literal(Integer.toString(this.menu.getPoolSize(option)))));
    }

    private void addDisabledReasonLines(
            List<Component> tooltip,
            int option,
            OptionDetails details,
            int requiredLevel,
            int displayCost,
            int reagentCost,
            int reagentCount,
            boolean creative,
            boolean missingClue
    ) {
        List<Component> reasons = Lists.newArrayList();
        int flags = this.menu.getDisabledReasonFlags(option);
        if ((flags & EnhancedEnchantingMenu.DISABLED_NO_REAGENT) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.no_reagent").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_BLOCKED_BY_MODIFIER) != 0) {
            Component blocker = details.blockingModifier().isEmpty()
                    ? Component.translatable("tooltip.betterenchanting.option.modifier")
                    : details.blockingModifier().getHoverName();
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.blocked_by_modifier", blocker).withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_NO_ROLL_POWER) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.no_roll_power").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_NO_COMPATIBLE_ENCHANTMENTS) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.no_compatible_enchantments").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_RESTRICTED_POOL_EMPTY) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.restricted_pool_empty").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_REMOVED_TAGS_EMPTY) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.removed_tags_empty").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_ENCHANTMENT_LIMIT) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.enchantment_limit").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_WEIGHT_SELECTION_FAILED) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.weight_selection_failed").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_DUPLICATE_OFFERS) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.duplicate_offers").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_NO_OFFER_POWER) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.no_offer_power").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_FUSION_LIMIT) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.fusion_limit").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_APOTHIC_INFUSION_UNMET) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.apothic_infusion_unmet").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_APOTHIC_INFUSION_MODIFIER) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.apothic_infusion_modifier").withStyle(ChatFormatting.RED));
        }
        if (requiredLevel > 0 && displayCost > 0 && reagentCost > 0 && reagentCount < reagentCost) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.not_enough_reagent").withStyle(ChatFormatting.RED));
        }
        if (flags == 0 && missingClue && requiredLevel > 0 && displayCost > 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.no_clue").withStyle(ChatFormatting.RED));
        }
        if (reasons.isEmpty()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(Component.translatable("tooltip.betterenchanting.option.disabled").withStyle(ChatFormatting.RED));
        tooltip.addAll(reasons);
    }

    private static Component detailLine(String key, Component value) {
        return Component.translatable(key, value).withStyle(ChatFormatting.GRAY);
    }

    private static Component joinedItemNames(List<ItemStack> stacks) {
        MutableComponent joined = Component.empty();
        for (int index = 0; index < stacks.size(); index++) {
            if (index > 0) {
                joined.append(Component.literal(", "));
            }
            joined.append(stacks.get(index).getHoverName());
        }
        return joined;
    }

    private static Component joinedBookBoosts(List<ItemStack> books) {
        MutableComponent joined = Component.empty();
        int count = 0;
        for (ItemStack book : books) {
            ItemEnchantments enchantments = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (count > 0) {
                    joined.append(Component.literal(", "));
                }
                joined.append(Enchantment.getFullname(entry.getKey(), entry.getIntValue()));
                count++;
            }
        }
        return count == 0 ? null : joined;
    }

    private FeedbackLine statusLine() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return null;
        }
        if (!this.menu.getSlot(EnhancedEnchantingMenu.TARGET_SLOT).hasItem()) {
            return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.add_item"), 0x7F7F7F);
        }

        boolean creative = this.minecraft.player.getAbilities().instabuild;
        int reagentCost = this.menu.getReagentCost();
        int reagentCount = this.menu.getReagentCount();
        int combinedFlags = 0;
        int poweredOffers = 0;
        int payableOffers = 0;
        int readyOffers = 0;

        for (int option = 0; option < 3; option++) {
            int requiredLevel = this.menu.requirements[option];
            int displayCost = this.menu.costs[option];
            int disabledFlags = this.menu.getDisabledReasonFlags(option);
            if (requiredLevel <= 0 || displayCost <= 0) {
                combinedFlags |= disabledFlags;
                continue;
            }

            poweredOffers++;
            combinedFlags |= disabledFlags;
            if (disabledFlags == 0 && this.menu.enchantClue[option] != -1) {
                payableOffers++;
            }
            if (!isOfferUnavailable(option, reagentCost, reagentCount, creative)) {
                readyOffers++;
            }
        }

        if (readyOffers > 0) {
            return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.ready", readyOffers), 0x3DB53D);
        }
        if ((combinedFlags & EnhancedEnchantingMenu.DISABLED_NO_REAGENT) != 0) {
            return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.need_reagent"), 0xFC5454);
        }
        if (poweredOffers == 0) {
            return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.no_power"), 0xFC5454);
        }
        if (payableOffers > 0 && reagentCost > 0 && reagentCount < reagentCost) {
            return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.need_reagent"), 0xFC5454);
        }
        if ((combinedFlags & (EnhancedEnchantingMenu.DISABLED_ENCHANTMENT_LIMIT | EnhancedEnchantingMenu.DISABLED_FUSION_LIMIT)) != 0) {
            return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.limit"), 0xFC5454);
        }
        if ((combinedFlags & EnhancedEnchantingMenu.DISABLED_BLOCKED_BY_MODIFIER) != 0) {
            return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.modifier_blocked"), 0xFC5454);
        }
        if ((combinedFlags & (EnhancedEnchantingMenu.DISABLED_RESTRICTED_POOL_EMPTY
                | EnhancedEnchantingMenu.DISABLED_REMOVED_TAGS_EMPTY
                | EnhancedEnchantingMenu.DISABLED_NO_COMPATIBLE_ENCHANTMENTS
                | EnhancedEnchantingMenu.DISABLED_WEIGHT_SELECTION_FAILED)) != 0) {
            return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.no_match"), 0xFC5454);
        }
        if ((combinedFlags & (EnhancedEnchantingMenu.DISABLED_APOTHIC_INFUSION_UNMET
                | EnhancedEnchantingMenu.DISABLED_APOTHIC_INFUSION_MODIFIER)) != 0) {
            return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.infusion"), 0xFC5454);
        }
        return new FeedbackLine(Component.translatable("tooltip.betterenchanting.status.hover"), 0xE0B45C);
    }

    private boolean isOfferUnavailable(int option, int reagentCost, int reagentCount, boolean creative) {
        return reagentCost > 0 && reagentCount < reagentCost
                || this.menu.enchantClue[option] == -1
                || this.menu.getDisabledReasonFlags(option) != 0;
    }

    private static Component coloredClueName(Holder<Enchantment> enchantment, int level) {
        return Enchantment.getFullname(enchantment, level)
                .copy()
                .withStyle(style -> style.withColor(ClientTooltipEvents.dominantAffinityColor(enchantment)));
    }

    private static ResourceLocation levelSprite(int cost, boolean disabled) {
        int index = Mth.clamp(cost, 1, ENABLED_LEVEL_SPRITES.length) - 1;
        return disabled ? DISABLED_LEVEL_SPRITES[index] : ENABLED_LEVEL_SPRITES[index];
    }

    private record FeedbackLine(Component text, int color) {
    }

    public void tickBook() {
        ItemStack itemStack = this.menu.getSlot(0).getItem();
        if (!ItemStack.matches(itemStack, this.last)) {
            this.last = itemStack;

            do {
                this.flipT = this.flipT + (float) (this.random.nextInt(4) - this.random.nextInt(4));
            } while (this.flip <= this.flipT + 1.0F && this.flip >= this.flipT - 1.0F);
        }

        this.time++;
        this.oFlip = this.flip;
        this.oOpen = this.open;
        boolean hasOffer = false;

        for (int option = 0; option < 3; option++) {
            if (this.menu.requirements[option] != 0 && this.menu.costs[option] != 0) {
                hasOffer = true;
            }
        }

        if (hasOffer) {
            this.open += 0.2F;
        } else {
            this.open -= 0.2F;
        }

        this.open = Mth.clamp(this.open, 0.0F, 1.0F);
        float flipVelocity = (this.flipT - this.flip) * 0.4F;
        flipVelocity = Mth.clamp(flipVelocity, -0.2F, 0.2F);
        this.flipA = this.flipA + (flipVelocity - this.flipA) * 0.9F;
        this.flip = this.flip + this.flipA;
    }

    private void tickApothicBars() {
        if (!this.menu.usesApothicLayout()) {
            return;
        }

        float current = this.menu.getApothicEterna();
        if (current != this.apothicEterna) {
            if (current > this.apothicEterna) {
                this.apothicEterna += Math.min(current - this.apothicEterna, Math.max(0.16F, (current - this.apothicEterna) * 0.1F));
            } else {
                this.apothicEterna = Math.max(this.apothicEterna - this.lastApothicEterna * 0.075F, current);
            }
        }
        if (current > 0) {
            this.lastApothicEterna = current;
        }

        current = this.menu.getApothicQuanta();
        if (current != this.apothicQuanta) {
            if (current > this.apothicQuanta) {
                this.apothicQuanta += Math.min(current - this.apothicQuanta, Math.max(0.04F, (current - this.apothicQuanta) * 0.1F));
            } else {
                this.apothicQuanta = Math.max(this.apothicQuanta - this.lastApothicQuanta * 0.075F, current);
            }
        }
        if (current > 0) {
            this.lastApothicQuanta = current;
        }

        current = this.menu.getApothicArcana();
        if (current != this.apothicArcana) {
            if (current > this.apothicArcana) {
                this.apothicArcana += Math.min(current - this.apothicArcana, Math.max(0.04F, (current - this.apothicArcana) * 0.1F));
            } else {
                this.apothicArcana = Math.max(this.apothicArcana - this.lastApothicArcana * 0.075F, current);
            }
        }
        if (current > 0) {
            this.lastApothicArcana = current;
        }
    }
}
