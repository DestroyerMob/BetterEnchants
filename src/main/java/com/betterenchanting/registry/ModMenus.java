package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.world.inventory.ArcaneCrucibleMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, BetterEnchanting.MOD_ID);

    public static final Supplier<MenuType<ArcaneCrucibleMenu>> ARCANE_CRUCIBLE = MENUS.register(
            ModBlocks.ARCANE_CRUCIBLE_ID,
            () -> IMenuTypeExtension.create(ArcaneCrucibleMenu::new)
    );

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
