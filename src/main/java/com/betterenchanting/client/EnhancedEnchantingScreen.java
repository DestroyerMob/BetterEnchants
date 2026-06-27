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
        int lapisCount = this.menu.getLapisCount();
        int lapisCost = this.menu.getLapisCost();

        for (int option = 0; option < 3; option++) {
            int optionX = x + 60;
            int textX = optionX + 20;
            int requiredLevel = this.menu.requirements[option];
            int xpCost = this.menu.costs[option];
            if (requiredLevel == 0 || xpCost == 0) {
                this.renderEnchantmentSlot(guiGraphics, texture, optionX, y + 14 + 19 * option, option, xpCost, true, false);
            } else {
                String requirementText = requiredLevel + "";
                int textWidth = 86 - this.font.width(requirementText);
                FormattedText randomName = EnchantmentNames.getInstance().getRandomName(this.font, textWidth);
                int textColor = 6839882;
                boolean disabled = (lapisCost > 0 && lapisCount < lapisCost || this.minecraft.player.experienceLevel < Math.max(requiredLevel, xpCost))
                        && !this.minecraft.player.getAbilities().instabuild
                        || this.menu.enchantClue[option] == -1
                        || this.menu.getDisabledReasonFlags(option) != 0;

                if (disabled) {
                    this.renderEnchantmentSlot(guiGraphics, texture, optionX, y + 14 + 19 * option, option, xpCost, true, true);
                    guiGraphics.drawWordWrap(this.font, randomName, textX, y + 16 + 19 * option, textWidth, (textColor & 16711422) >> 1);
                    textColor = 4226832;
                } else {
                    int hoverX = mouseX - (x + 60);
                    int hoverY = mouseY - (y + 14 + 19 * option);
                    boolean highlighted = hoverX >= 0 && hoverY >= 0 && hoverX < 108 && hoverY < 19;
                    this.renderEnchantmentSlot(guiGraphics, texture, optionX, y + 14 + 19 * option, option, xpCost, false, true, highlighted);
                    if (highlighted) {
                        textColor = 16777088;
                    }
                    guiGraphics.drawWordWrap(this.font, randomName, textX, y + 16 + 19 * option, textWidth, textColor);
                    textColor = 8453920;
                }

                guiGraphics.drawString(this.font, requirementText, textX + 86 - this.font.width(requirementText), y + 16 + 19 * option + 7, textColor);
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
            int xpCost,
            boolean disabled,
            boolean renderLevel
    ) {
        this.renderEnchantmentSlot(guiGraphics, texture, x, y, option, xpCost, disabled, renderLevel, false);
    }

    private void renderEnchantmentSlot(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int x,
            int y,
            int option,
            int xpCost,
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
                int iconU = 16 * (Mth.clamp(xpCost, 1, 3) - 1);
                guiGraphics.blit(texture, x + 1, y + 1, iconU, iconV, 16, 16);
            }
        } else {
            if (disabled) {
                guiGraphics.blitSprite(ENCHANTMENT_SLOT_DISABLED_SPRITE, x, y, 108, 19);
                if (renderLevel) {
                    guiGraphics.blitSprite(levelSprite(xpCost, true), x + 1, y + 1, 16, 16);
                }
            } else {
                guiGraphics.blitSprite(highlighted ? ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE : ENCHANTMENT_SLOT_SPRITE, x, y, 108, 19);
                if (renderLevel) {
                    guiGraphics.blitSprite(levelSprite(xpCost, false), x + 1, y + 1, 16, 16);
                }
            }
        }
        RenderSystem.disableBlend();
    }

    private void renderApothicStats(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        if (!this.menu.usesApothicLayout()) {
            return;
        }
        if (this.menu.getApothicEterna() > 0) {
            guiGraphics.blit(texture, x + 59, y + 75, 0, 197, apothicBarLength(this.menu.getApothicEterna()), 5);
        }
        if (this.menu.getApothicQuanta() > 0) {
            guiGraphics.blit(texture, x + 59, y + 85, 0, 202, apothicBarLength(this.menu.getApothicQuanta()), 5);
        }
        if (this.menu.getApothicArcana() > 0) {
            guiGraphics.blit(texture, x + 59, y + 95, 0, 207, apothicBarLength(this.menu.getApothicArcana()), 5);
        }
    }

    private static int apothicBarLength(float stat) {
        return Mth.clamp((int) (stat / APOTHIC_STAT_BAR_MAX * APOTHIC_STAT_BAR_WIDTH), 0, APOTHIC_STAT_BAR_WIDTH);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.usesApothicLayout()) {
            super.renderLabels(guiGraphics, mouseX, mouseY);
            return;
        }
        guiGraphics.drawString(this.font, this.title, 12, 5, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 7, this.imageHeight - 96 + 4, 4210752, false);
        guiGraphics.drawString(this.font, Component.literal("Eterna"), 19, 74, 0x3DB53D, false);
        guiGraphics.drawString(this.font, Component.literal("Quanta"), 19, 84, 0xFC5454, false);
        guiGraphics.drawString(this.font, Component.literal("Arcana"), 19, 94, 0xA800A8, false);
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
        int lapisCount = this.menu.getLapisCount();

        for (int option = 0; option < 3; option++) {
            int requiredLevel = this.menu.requirements[option];
            int xpCost = this.menu.costs[option];
            if (this.isHovering(60, 14 + 19 * option, 108, 17, mouseX, mouseY)) {
                Optional<Holder.Reference<Enchantment>> clue = this.minecraft
                        .level
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolder(this.menu.enchantClue[option]);
                int clueLevel = this.menu.levelClue[option];
                int lapisCost = this.menu.getLapisCost();
                List<Component> tooltip = Lists.newArrayList();
                boolean hasOfferPower = requiredLevel > 0 && xpCost > 0;
                OptionDetails details = this.menu.getOptionDetails(option);
                if (hasOfferPower) {
                    Component clueName = clue.<Component>map(holder -> coloredClueName(holder, clueLevel)).orElse(CommonComponents.EMPTY);
                    String clueKey = "container.enchant.clue";
                    if (this.menu.isApothicInfusionOffer(option)) {
                        clueKey = "tooltip.betterenchanting.option.infusion_clue";
                    } else if (this.menu.isOverlevelOffer(option)) {
                        clueKey = "tooltip.betterenchanting.option.overlevel_clue";
                    }
                    tooltip.add(Component.translatable(clueKey, clueName).withStyle(ChatFormatting.WHITE));
                } else {
                    tooltip.add(Component.translatable("tooltip.betterenchanting.option.unavailable").withStyle(ChatFormatting.WHITE));
                }

                addDisabledReasonLines(tooltip, option, details, requiredLevel, xpCost, lapisCost, lapisCount, creative, clue.isEmpty());

                if (clue.isPresent() && hasOfferPower && !creative) {
                    tooltip.add(CommonComponents.EMPTY);
                    boolean meetsRequirement = this.minecraft.player.experienceLevel >= requiredLevel;
                    boolean canPayLevels = this.minecraft.player.experienceLevel >= xpCost;
                    tooltip.add(Component.translatable("container.enchant.level.requirement", requiredLevel).withStyle(meetsRequirement ? ChatFormatting.GRAY : ChatFormatting.RED));
                    if (meetsRequirement) {
                        if (lapisCost > 0) {
                            MutableComponent lapisLine = lapisCost == 1
                                    ? Component.translatable("container.enchant.lapis.one")
                                    : Component.translatable("container.enchant.lapis.many", lapisCost);
                            tooltip.add(lapisLine.withStyle(lapisCount >= lapisCost ? ChatFormatting.GRAY : ChatFormatting.RED));
                        }

                        MutableComponent levelLine = xpCost == 1
                                ? Component.translatable("container.enchant.level.one")
                                : Component.translatable("container.enchant.level.many", xpCost);
                        tooltip.add(levelLine.withStyle(canPayLevels ? ChatFormatting.GRAY : ChatFormatting.RED));
                    }
                }

                tooltip.add(CommonComponents.EMPTY);
                this.addSystemTooltipLines(tooltip, option, details);
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                return;
            }
        }
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
                    Component.literal(this.menu.allowsApothicTreasure() ? "Treasure shelves are active." : "Treasure shelves are inactive.").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
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
            int xpCost,
            int lapisCost,
            int lapisCount,
            boolean creative,
            boolean missingClue
    ) {
        List<Component> reasons = Lists.newArrayList();
        int flags = this.menu.getDisabledReasonFlags(option);
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
        if ((flags & EnhancedEnchantingMenu.DISABLED_NO_OFFER_POWER) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.no_offer_power").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_FUSION_LIMIT) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.fusion_limit").withStyle(ChatFormatting.RED));
        }
        if ((flags & EnhancedEnchantingMenu.DISABLED_APOTHIC_INFUSION_UNMET) != 0) {
            reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.apothic_infusion_unmet").withStyle(ChatFormatting.RED));
        }
        if (!creative && requiredLevel > 0 && xpCost > 0) {
            if (lapisCost > 0 && lapisCount < lapisCost) {
                reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.not_enough_lapis", lapisCost).withStyle(ChatFormatting.RED));
            }
            if (requiredLevel > 0 && this.minecraft.player.experienceLevel < requiredLevel) {
                reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.not_enough_level", requiredLevel).withStyle(ChatFormatting.RED));
            } else if (xpCost > 0 && this.minecraft.player.experienceLevel < xpCost) {
                reasons.add(Component.translatable("tooltip.betterenchanting.option.disabled.not_enough_xp_cost", xpCost).withStyle(ChatFormatting.RED));
            }
        }
        if (flags == 0 && missingClue && requiredLevel > 0 && xpCost > 0) {
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

    private static Component coloredClueName(Holder<Enchantment> enchantment, int level) {
        return Enchantment.getFullname(enchantment, level)
                .copy()
                .withStyle(style -> style.withColor(ClientTooltipEvents.dominantAffinityColor(enchantment)));
    }

    private static ResourceLocation levelSprite(int cost, boolean disabled) {
        int index = Mth.clamp(cost, 1, ENABLED_LEVEL_SPRITES.length) - 1;
        return disabled ? DISABLED_LEVEL_SPRITES[index] : ENABLED_LEVEL_SPRITES[index];
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
}
