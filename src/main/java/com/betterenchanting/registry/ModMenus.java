package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.world.inventory.EnhancedEnchantingMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, BetterEnchanting.MOD_ID);

    public static final Supplier<MenuType<EnhancedEnchantingMenu>> ENHANCED_ENCHANTING = MENUS.register(
            "enhanced_enchanting",
            () -> IMenuTypeExtension.create(EnhancedEnchantingMenu::new)
    );

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
