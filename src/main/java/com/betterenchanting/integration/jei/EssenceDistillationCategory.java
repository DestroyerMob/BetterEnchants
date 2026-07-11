package com.betterenchanting.integration.jei;

import com.betterenchanting.data.EssenceDistillationRecipes;
import com.betterenchanting.data.EssenceDistillationRecipes.IngredientSpec;
import com.betterenchanting.registry.ModItems;
import com.betterenchanting.world.level.block.entity.ArcaneCrucibleBlockEntity;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class EssenceDistillationCategory implements IRecipeCategory<EssenceDistillationRecipes.Recipe> {
    private static final int WIDTH = 160;
    private static final int HEIGHT = 58;
    private static final int TEXT = 0x404040;
    private static final int MUTED = 0x6f6f6f;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public EssenceDistillationCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(ModItems.ARCANE_CRUCIBLE.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<EssenceDistillationRecipes.Recipe> getRecipeType() {
        return BetterEnchantingJeiPlugin.ESSENCE_DISTILLATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.betterenchanting.essence_distillation");
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
    public void setRecipe(IRecipeLayoutBuilder builder, EssenceDistillationRecipes.Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 20)
                .setStandardSlotBackground()
                .addItemStacks(displayStacks(recipe.medium()));

        List<IngredientSpec> catalysts = recipe.catalysts();
        int firstCatalystX = catalysts.size() == 1 ? 57 : 47;
        for (int index = 0; index < catalysts.size(); index++) {
            builder.addSlot(RecipeIngredientRole.INPUT, firstCatalystX + index * 20, 20)
                    .setStandardSlotBackground()
                    .addItemStacks(displayStacks(catalysts.get(index)));
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 137, 20)
                .setOutputSlotBackground()
                .addItemStack(recipe.resultCopy());
    }

    @Override
    public void draw(
            EssenceDistillationRecipes.Recipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.translatable("jei.betterenchanting.distillation.medium"), 2, 3, TEXT, false);
        guiGraphics.drawString(font, Component.translatable("jei.betterenchanting.distillation.catalysts"), 43, 3, TEXT, false);
        guiGraphics.drawString(font, Component.translatable("jei.betterenchanting.distillation.result"), 126, 3, TEXT, false);
        guiGraphics.drawString(font, Component.literal("+"), 31, 24, MUTED, false);
        arrow.draw(guiGraphics, 99, 20);
        guiGraphics.drawCenteredString(
                font,
                Component.translatable(
                        "jei.betterenchanting.distillation.time",
                        ArcaneCrucibleBlockEntity.DISTILLATION_TIME / 20
                ),
                111,
                42,
                MUTED
        );
    }

    private static List<ItemStack> displayStacks(IngredientSpec ingredient) {
        if (ingredient.item() != null) {
            return List.of(new ItemStack(ingredient.item()));
        }
        if (ingredient.tag() == null) {
            return List.of();
        }
        return BuiltInRegistries.ITEM.getTag(ingredient.tag())
                .map(named -> named.stream().map(holder -> new ItemStack(holder.value())).toList())
                .orElse(List.of());
    }
}
