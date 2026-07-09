package com.betterenchanting.integration.jei;

import com.betterenchanting.data.TagDisplayRules;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
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
    private static final int HEIGHT = 72;
    private static final int TEXT = 0x404040;
    private static final int MUTED = 0x6f6f6f;
    private static final int HIGHLIGHT = 0x4f3b86;
    private static final int MAX_VISIBLE_LEVELS = 4;

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
    }

    @Override
    public void draw(EnchantmentInfoRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, recipe.title(), 25, 5, TEXT, false);

        OptionalInt minimumPower = recipe.minimumPower();
        String power = minimumPower.isPresent() ? minimumPower.getAsInt() + "+" : "not table-rollable";
        guiGraphics.drawString(font, Component.literal("Power " + power), 25, 17, HIGHLIGHT, false);

        String shelfText = recipe.minimumBookshelfPower().isPresent()
                ? "Shelves " + recipe.minimumBookshelfPower().getAsInt() + "+"
                : "Use modifiers";
        guiGraphics.drawString(font, Component.literal(shelfText), 91, 17, MUTED, false);

        int y = 32;
        y = drawLine(guiGraphics, font, tableLine(recipe.vanillaBands()), 3, y, TEXT);
        String apothicLine = apothicLine(recipe.vanillaBands(), recipe.apothicBands());
        if (!apothicLine.isEmpty()) {
            y = drawLine(guiGraphics, font, apothicLine, 3, y, MUTED);
        }
        y = drawLine(guiGraphics, font, labelLine(recipe), 3, y, MUTED);
        drawLine(guiGraphics, font, flagLine(recipe), 3, y, MUTED);
    }

    private static String tableLine(List<EnchantmentInfoRecipe.PowerBand> bands) {
        if (bands.isEmpty()) {
            return "Not in table pool";
        }
        return "Table: " + compactLevels(bands);
    }

    private static String apothicLine(List<EnchantmentInfoRecipe.PowerBand> vanillaBands, List<EnchantmentInfoRecipe.PowerBand> apothicBands) {
        if (apothicBands.isEmpty() || sameMinimums(vanillaBands, apothicBands)) {
            return "";
        }
        return "Apothic: " + compactLevels(apothicBands);
    }

    private static boolean sameMinimums(List<EnchantmentInfoRecipe.PowerBand> first, List<EnchantmentInfoRecipe.PowerBand> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            if (first.get(index).level() != second.get(index).level()
                    || first.get(index).minPower() != second.get(index).minPower()) {
                return false;
            }
        }
        return true;
    }

    private static String compactLevels(List<EnchantmentInfoRecipe.PowerBand> bands) {
        StringBuilder builder = new StringBuilder();
        int count = Math.min(MAX_VISIBLE_LEVELS, bands.size());
        for (int index = 0; index < count; index++) {
            EnchantmentInfoRecipe.PowerBand band = bands.get(index);
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append('L').append(band.level()).append(' ').append(band.minPower()).append('+');
        }
        if (bands.size() > MAX_VISIBLE_LEVELS) {
            builder.append(", ...");
        }
        return builder.toString();
    }

    private static String labelLine(EnchantmentInfoRecipe recipe) {
        List<TagDisplayRules.TagLabel> labels = new ArrayList<>();
        labels.addAll(recipe.targets());
        labels.addAll(recipe.affinities());
        return labels.isEmpty() ? "" : joinLabels(labels);
    }

    private static String flagLine(EnchantmentInfoRecipe recipe) {
        List<String> flags = new ArrayList<>();
        if (!recipe.inEnchantingTable()) {
            flags.add("not in table pool");
        }
        if (recipe.curse()) {
            flags.add("curse");
        }
        if (recipe.treasure()) {
            flags.add("treasure");
        }
        return String.join(", ", flags);
    }

    private static String joinLabels(List<TagDisplayRules.TagLabel> labels) {
        StringBuilder builder = new StringBuilder();
        for (TagDisplayRules.TagLabel label : labels) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(label.text());
        }
        return builder.toString();
    }

    private static int drawLine(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
        if (text.isBlank()) {
            return y;
        }
        String fitted = font.plainSubstrByWidth(text, WIDTH - x - 3);
        guiGraphics.drawString(font, Component.literal(fitted), x, y, color, false);
        return y + 10;
    }
}
