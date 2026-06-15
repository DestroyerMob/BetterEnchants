package com.betterenchanting;

import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.registry.CreativeTabEvents;
import com.betterenchanting.registry.ModBlocks;
import com.betterenchanting.registry.ModItems;
import com.betterenchanting.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod(BetterEnchanting.MOD_ID)
public final class BetterEnchanting {
    public static final String MOD_ID = "betterenchanting";

    public BetterEnchanting(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModMenus.register(modBus);
        modBus.addListener(CreativeTabEvents::addContents);
        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EssenceDefinitions.ReloadListener());
    }
}
