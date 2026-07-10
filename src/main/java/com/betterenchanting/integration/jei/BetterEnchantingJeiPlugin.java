package com.betterenchanting.integration.jei;

import com.betterenchanting.BetterEnchanting;
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

    @Override
    public ResourceLocation getPluginUid() {
        return BetterEnchanting.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new EnchantmentInfoCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ENCHANTMENT_INFO, enchantmentInfoRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Items.ENCHANTING_TABLE, ENCHANTMENT_INFO);
    }

    private static List<EnchantmentInfoRecipe> enchantmentInfoRecipes() {
        return registryAccess()
                .map(access -> {
                    Registry<Enchantment> enchantments = access.registryOrThrow(Registries.ENCHANTMENT);
                    return enchantments.holders()
                            .map(EnchantmentInfoRecipe::create)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .sorted(Comparator.comparing(recipe -> recipe.sortName().toLowerCase(Locale.ROOT)))
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
