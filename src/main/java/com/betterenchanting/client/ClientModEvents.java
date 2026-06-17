package com.betterenchanting.client;

import com.betterenchanting.data.TagDisplayRules;
import com.betterenchanting.registry.ModMenus;
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
        event.registerReloadListener(new TagDisplayRules.ReloadListener());
    }
}
