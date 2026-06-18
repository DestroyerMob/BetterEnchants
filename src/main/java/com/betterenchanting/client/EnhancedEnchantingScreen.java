package com.betterenchanting.client;

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
    private static final int BASE_IMAGE_WIDTH = 176;
    private static final int EXTENDED_IMAGE_WIDTH = 201;
    private static final int MODIFIER_PANEL_X = 176;
    private static final int MODIFIER_PANEL_Y = 10;
    private static final int MODIFIER_PANEL_WIDTH = 25;
    private static final int MODIFIER_PANEL_HEIGHT = 64;
    private static final int MODIFIER_SLOT_FRAME_X = 179;
    private static final int MODIFIER_SLOT_FRAME_Y = 14;
    private static final int MODIFIER_SLOT_GAP = 19;
    private static final int VANILLA_SLOT_TEXTURE_X = 14;
    private static final int VANILLA_SLOT_TEXTURE_Y = 46;
    private static final int VANILLA_SLOT_SIZE = 18;
    private static final int VANILLA_PANEL = 0xFFC6C6C6;
    private static final int VANILLA_BORDER = 0xFF000000;
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
    private static final ResourceLocation ENCHANTING_TABLE_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/enchanting_table.png");
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
        guiGraphics.blit(ENCHANTING_TABLE_LOCATION, x, y, 0, 0, BASE_IMAGE_WIDTH, this.imageHeight);
        this.renderModifierPanel(guiGraphics, x, y);
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
                RenderSystem.enableBlend();
                guiGraphics.blitSprite(ENCHANTMENT_SLOT_DISABLED_SPRITE, optionX, y + 14 + 19 * option, 108, 19);
                RenderSystem.disableBlend();
            } else {
                String requirementText = requiredLevel + "";
                int textWidth = 86 - this.font.width(requirementText);
                FormattedText randomName = EnchantmentNames.getInstance().getRandomName(this.font, textWidth);
                int textColor = 6839882;
                boolean disabled = (lapisCost > 0 && lapisCount < lapisCost || this.minecraft.player.experienceLevel < Math.max(requiredLevel, xpCost))
                        && !this.minecraft.player.getAbilities().instabuild
                        || this.menu.enchantClue[option] == -1;

                if (disabled) {
                    RenderSystem.enableBlend();
                    guiGraphics.blitSprite(ENCHANTMENT_SLOT_DISABLED_SPRITE, optionX, y + 14 + 19 * option, 108, 19);
                    guiGraphics.blitSprite(levelSprite(xpCost, true), optionX + 1, y + 15 + 19 * option, 16, 16);
                    RenderSystem.disableBlend();
                    guiGraphics.drawWordWrap(this.font, randomName, textX, y + 16 + 19 * option, textWidth, (textColor & 16711422) >> 1);
                    textColor = 4226832;
                } else {
                    int hoverX = mouseX - (x + 60);
                    int hoverY = mouseY - (y + 14 + 19 * option);
                    RenderSystem.enableBlend();
                    if (hoverX >= 0 && hoverY >= 0 && hoverX < 108 && hoverY < 19) {
                        guiGraphics.blitSprite(ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE, optionX, y + 14 + 19 * option, 108, 19);
                        textColor = 16777088;
                    } else {
                        guiGraphics.blitSprite(ENCHANTMENT_SLOT_SPRITE, optionX, y + 14 + 19 * option, 108, 19);
                    }

                    guiGraphics.blitSprite(levelSprite(xpCost, false), optionX + 1, y + 15 + 19 * option, 16, 16);
                    RenderSystem.disableBlend();
                    guiGraphics.drawWordWrap(this.font, randomName, textX, y + 16 + 19 * option, textWidth, textColor);
                    textColor = 8453920;
                }

                guiGraphics.drawString(this.font, requirementText, textX + 86 - this.font.width(requirementText), y + 16 + 19 * option + 7, textColor);
            }
        }
    }

    private void renderModifierPanel(GuiGraphics guiGraphics, int x, int y) {
        int panelX = x + MODIFIER_PANEL_X;
        int panelY = y + MODIFIER_PANEL_Y;
        guiGraphics.fill(panelX, panelY, panelX + MODIFIER_PANEL_WIDTH, panelY + MODIFIER_PANEL_HEIGHT, VANILLA_PANEL);
        guiGraphics.fill(panelX, panelY, panelX + MODIFIER_PANEL_WIDTH, panelY + 1, VANILLA_BORDER);
        guiGraphics.fill(panelX, panelY + MODIFIER_PANEL_HEIGHT - 1, panelX + MODIFIER_PANEL_WIDTH, panelY + MODIFIER_PANEL_HEIGHT, VANILLA_BORDER);
        guiGraphics.fill(panelX + MODIFIER_PANEL_WIDTH - 1, panelY, panelX + MODIFIER_PANEL_WIDTH, panelY + MODIFIER_PANEL_HEIGHT, VANILLA_BORDER);

        for (int slot = 0; slot < EnhancedEnchantingMenu.MODIFIER_SLOT_COUNT; slot++) {
            guiGraphics.blit(
                    ENCHANTING_TABLE_LOCATION,
                    x + MODIFIER_SLOT_FRAME_X,
                    y + MODIFIER_SLOT_FRAME_Y + slot * MODIFIER_SLOT_GAP,
                    VANILLA_SLOT_TEXTURE_X,
                    VANILLA_SLOT_TEXTURE_Y,
                    VANILLA_SLOT_SIZE,
                    VANILLA_SLOT_SIZE
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
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
            Optional<Holder.Reference<Enchantment>> clue = this.minecraft
                    .level
                    .registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(this.menu.enchantClue[option]);

            if (this.isHovering(60, 14 + 19 * option, 108, 17, mouseX, mouseY) && requiredLevel > 0 && xpCost > 0) {
                int clueLevel = this.menu.levelClue[option];
                int lapisCost = this.menu.getLapisCost();
                List<Component> tooltip = Lists.newArrayList();
                tooltip.add(Component.translatable(
                        "container.enchant.clue",
                        clue.<Component>map(holder -> coloredClueName(holder, clueLevel)).orElse(CommonComponents.EMPTY)
                ).withStyle(ChatFormatting.WHITE));

                if (clue.isEmpty()) {
                    tooltip.add(CommonComponents.EMPTY);
                    tooltip.add(Component.translatable("neoforge.container.enchant.limitedEnchantability").withStyle(ChatFormatting.RED));
                } else if (!creative) {
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
                this.addSystemTooltipLines(tooltip, option);
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    private void addSystemTooltipLines(List<Component> tooltip, int option) {
        OptionDetails details = this.menu.getOptionDetails(option);
        tooltip.add(detailLine(
                "tooltip.betterenchanting.option.mode",
                Component.translatable(details.profile().restricted()
                        ? "tooltip.betterenchanting.option.mode.restricted"
                        : "tooltip.betterenchanting.option.mode.weighted")
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
        if (!details.globalModifiers().isEmpty()) {
            tooltip.add(detailLine("tooltip.betterenchanting.option.global_modifier", joinedItemNames(details.globalModifiers())));
        }

        Component bookBoosts = joinedBookBoosts(details.bookModifiers());
        if (bookBoosts != null) {
            tooltip.add(detailLine("tooltip.betterenchanting.option.book_boost", bookBoosts));
        }
        tooltip.add(detailLine("tooltip.betterenchanting.option.pool", Component.literal(Integer.toString(this.menu.getPoolSize(option)))));
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
