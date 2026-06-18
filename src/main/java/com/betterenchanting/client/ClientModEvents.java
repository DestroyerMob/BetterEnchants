package com.betterenchanting.client;

import com.betterenchanting.data.EnchantmentLimitRules;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.data.TagDisplayRules;
import com.betterenchanting.data.TagSimplifier;
import com.betterenchanting.registry.ModMenus;
import com.betterenchanting.world.EnchantmentTargetTags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientModEvents::registerScreens);
        modBus.addListener(ClientModEvents::registerReloadListeners);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ENHANCED_ENCHANTING.get(), EnhancedEnchantingScreen::new);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new EssenceDefinitions.ReloadListener());
        event.registerReloadListener(new EnchantmentLimitRules.ReloadListener());
        event.registerReloadListener(new EnchantmentTargetTags.ReloadListener());
        event.registerReloadListener(new TagDisplayRules.ReloadListener());
        event.registerReloadListener(new TagSimplifier.ReloadListener());
    }
}
