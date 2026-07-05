package com.betterenchanting.registry;

import com.betterenchanting.config.EffectiveBalance;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class CreativeTabEvents {
    private CreativeTabEvents() {
    }

    public static void addContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            ModItems.ESSENCES.forEach(item -> event.accept(item.get()));
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.ATTUNEMENT_FOCUS.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS && !EffectiveBalance.takesOverEnchantingTable()) {
            event.accept(ModItems.ARCANE_CRUCIBLE.get());
        }
    }
}
