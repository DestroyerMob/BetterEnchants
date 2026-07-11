package com.betterenchanting.integration.jei;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.data.EssenceDistillationRecipes;
import com.betterenchanting.registry.ModItems;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

@JeiPlugin
public final class BetterEnchantingJeiPlugin implements IModPlugin {
    public static final RecipeType<EnchantmentInfoRecipe> ENCHANTMENT_INFO = RecipeType.create(
            BetterEnchanting.MOD_ID,
            "enchantment_info",
            EnchantmentInfoRecipe.class
    );
    public static final RecipeType<EssenceDistillationRecipes.Recipe> ESSENCE_DISTILLATION = RecipeType.create(
            BetterEnchanting.MOD_ID,
            "essence_distillation",
            EssenceDistillationRecipes.Recipe.class
    );

    @Override
    public ResourceLocation getPluginUid() {
        return BetterEnchanting.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new EnchantmentInfoCategory(registration.getJeiHelpers().getGuiHelper()),
                new EssenceDistillationCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ENCHANTMENT_INFO, enchantmentInfoRecipes());
        registration.addRecipes(ESSENCE_DISTILLATION, EssenceDistillationRecipes.all());
        EssenceAcquisitionInfo.register(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Items.ENCHANTING_TABLE, ENCHANTMENT_INFO);
        registration.addRecipeCatalyst(ModItems.ARCANE_CRUCIBLE.get(), ESSENCE_DISTILLATION);
    }

    private static List<EnchantmentInfoRecipe> enchantmentInfoRecipes() {
        return registryAccess()
                .map(access -> {
                    Registry<Enchantment> enchantments = access.registryOrThrow(Registries.ENCHANTMENT);
                    return enchantments.holders()
                            .flatMap(enchantment -> EnchantmentInfoRecipe.createAll(enchantment).stream())
                            .sorted(Comparator
                                    .comparing((EnchantmentInfoRecipe recipe) -> recipe.sortName().toLowerCase(Locale.ROOT))
                                    .thenComparingInt(EnchantmentInfoRecipe::level))
                            .toList();
                })
                .orElse(List.of());
    }

    private static Optional<RegistryAccess> registryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return Optional.of(minecraft.level.registryAccess());
        }
        if (minecraft.getConnection() != null) {
            return Optional.of(minecraft.getConnection().registryAccess());
        }
        return Optional.empty();
    }
}
