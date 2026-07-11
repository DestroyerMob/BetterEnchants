package com.betterenchanting.integration.jei;

import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EnchantmentInfoCategory implements IRecipeCategory<EnchantmentInfoRecipe> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 78;
    private static final int TEXT = 0x404040;
    private static final int MUTED = 0x6f6f6f;
    private static final int MAX_VISIBLE_ESSENCES = 4;

    private final IDrawable background;
    private final IDrawable icon;

    public EnchantmentInfoCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.ENCHANTING_TABLE));
    }

    @Override
    public RecipeType<EnchantmentInfoRecipe> getRecipeType() {
        return BetterEnchantingJeiPlugin.ENCHANTMENT_INFO;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.betterenchanting.enchantment_info");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EnchantmentInfoRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 3, 5)
                .setOutputSlotBackground()
                .addItemStack(recipe.book());

        List<ItemStack> matchingEssences = recipe.matchingEssences();
        int visibleEssences = Math.min(MAX_VISIBLE_ESSENCES, matchingEssences.size());
        for (int index = 0; index < visibleEssences; index++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 94 + index * 20, 59)
                    .setStandardSlotBackground()
                    .addItemStack(matchingEssences.get(index));
        }
        if (matchingEssences.size() > visibleEssences) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                    .addItemStacks(matchingEssences.subList(visibleEssences, matchingEssences.size()));
        }
    }

    @Override
    public void draw(EnchantmentInfoRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        String title = font.plainSubstrByWidth(recipe.title().getString(), WIDTH - 35);
        guiGraphics.drawString(font, Component.literal(title), 33, 5, TEXT, false);

        EnchantmentInfoRecipe.PowerBand band = recipe.activePowerBand().orElseThrow();
        String power = band.minPower() == band.maxPower()
                ? Integer.toString(band.minPower())
                : band.minPower() + "–" + band.maxPower();
        guiGraphics.drawString(font, Component.literal("Roll power: " + power), 33, 18, TEXT, false);
        if (!recipe.apothicBands().isEmpty() && band.maxPower() > 100) {
            guiGraphics.drawString(font, Component.literal("Eterna caps at 100."), 3, 35, MUTED, false);
            guiGraphics.drawString(font, Component.literal("Quanta can raise power to 200."), 3, 45, MUTED, false);
        }

        List<ItemStack> matchingEssences = recipe.matchingEssences();
        String poolLabel = matchingEssences.isEmpty() ? "Added by: no essence" : "Added to pool by:";
        guiGraphics.drawString(font, Component.literal(poolLabel), 3, 63, MUTED, false);
    }
}
