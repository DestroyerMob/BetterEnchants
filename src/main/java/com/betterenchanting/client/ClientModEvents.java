package com.betterenchanting.client;

import com.betterenchanting.data.EnchantmentGuideEntries;
import com.betterenchanting.data.EnchantmentLimitRules;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.data.EssenceDistillationRecipes;
import com.betterenchanting.data.PartEnchantmentRoutes;
import com.betterenchanting.data.TagDisplayRules;
import com.betterenchanting.data.TagSimplifier;
import com.betterenchanting.registry.ModMenus;
import com.betterenchanting.world.EnchantmentTargetTags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.betterenchanting.registry.ModBlockEntities;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientModEvents::registerScreens);
        modBus.addListener(ClientModEvents::registerBlockEntityRenderers);
        modBus.addListener(ClientModEvents::registerReloadListeners);
        modBus.addListener(ClientModEvents::registerRenderBuffers);
        modBus.addListener(ClientInputEvents::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientInputEvents::detectFlashStep);
        NeoForge.EVENT_BUS.addListener(ChainedMiningAnimationState::tick);
        NeoForge.EVENT_BUS.addListener(ResonanceHighlights::render);
        NeoForge.EVENT_BUS.addListener(MachineDisplayInteraction::render);
        NeoForge.EVENT_BUS.addListener(MachineDisplayInteraction::renderGui);
        NeoForge.EVENT_BUS.addListener(MachineDisplayInteraction::handleInteraction);
        NeoForge.EVENT_BUS.addListener(PedestalUpgradeOrbOverlay::render);
        NeoForge.EVENT_BUS.addListener(PedestalUpgradeOrbOverlay::renderGui);
        NeoForge.EVENT_BUS.addListener(PedestalUpgradeOrbOverlay::handleInteraction);
        NeoForge.EVENT_BUS.addListener(RoutedEnchantmentOrbOverlay::render);
        NeoForge.EVENT_BUS.addListener(RoutedEnchantmentOrbOverlay::renderGui);
        NeoForge.EVENT_BUS.addListener(RoutedEnchantmentOrbOverlay::handleInteraction);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ENHANCED_ENCHANTING.get(), EnhancedEnchantingScreen::new);
    }

    private static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ATTUNEMENT_PEDESTAL.get(), AttunementPedestalRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_CRUCIBLE.get(), ArcaneCrucibleRenderer::new);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new EssenceDefinitions.ReloadListener());
        event.registerReloadListener(new EssenceDistillationRecipes.ReloadListener());
        event.registerReloadListener(new EnchantmentGuideEntries.ReloadListener());
        event.registerReloadListener(new EnchantmentLimitRules.ReloadListener());
        event.registerReloadListener(new PartEnchantmentRoutes.ReloadListener());
        event.registerReloadListener(new EnchantmentTargetTags.ReloadListener());
        event.registerReloadListener(new TagDisplayRules.ReloadListener());
        event.registerReloadListener(new TagSimplifier.ReloadListener());
    }

    private static void registerRenderBuffers(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(OverleveledGlintRenderTypes.armorEntityGlint());
        event.registerRenderBuffer(OverleveledGlintRenderTypes.glint());
        event.registerRenderBuffer(OverleveledGlintRenderTypes.glintTranslucent());
        event.registerRenderBuffer(OverleveledGlintRenderTypes.entityGlint());
        event.registerRenderBuffer(OverleveledGlintRenderTypes.entityGlintDirect());
        event.registerRenderBuffer(OverleveledGlintRenderTypes.layeredToolGlint());
        event.registerRenderBuffer(OverleveledGlintRenderTypes.layeredToolGlintTranslucent());
        event.registerRenderBuffer(OverleveledGlintRenderTypes.layeredToolEntityGlint());
        event.registerRenderBuffer(OverleveledGlintRenderTypes.layeredToolEntityGlintDirect());
        event.registerRenderBuffer(ResonanceRenderTypes.lines());
    }
}
