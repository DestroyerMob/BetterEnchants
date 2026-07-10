package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BetterEnchanting.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BETTER_ENCHANTING =
            CREATIVE_TABS.register("better_enchanting", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.betterenchanting"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.ATTUNEMENT_FOCUS.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ATTUNEMENT_FOCUS.get());
                        output.accept(ModItems.ARCANE_CRUCIBLE.get());
                        output.accept(ModItems.ATTUNEMENT_PEDESTAL.get());
                        ModItems.ESSENCES.forEach(item -> output.accept(item.get()));
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        CREATIVE_TABS.register(modBus);
    }
}
