package com.betterenchanting.network;

import com.betterenchanting.client.ResonanceHighlights;
import com.betterenchanting.client.ChainedMiningAnimationState;
import com.betterenchanting.client.InteractiveEnchantingOverlay;
import com.betterenchanting.compat.MobsToolForgingCompat;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedEnchantmentState;
import com.betterenchanting.registry.ModItems;
import com.betterenchanting.config.BetterEnchantingConfig;
import com.betterenchanting.config.EffectiveBalance;
import com.betterenchanting.world.inventory.EnhancedEnchantingMenu;
import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import com.betterenchanting.world.level.block.entity.ArcaneCrucibleBlockEntity;
import com.betterenchanting.world.level.block.EnchantingTableStorage;
import com.betterenchanting.world.level.block.InWorldMachineInteraction;
import com.betterenchanting.world.inventory.PedestalUpgradeRules.UpgradePlan;
import com.betterenchanting.world.enchantment.FlashStepEnchantmentEvents;
import com.betterenchanting.world.enchantment.VeinMinerModeState;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "9";

    private ModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(CycleVeinMinerModePayload.TYPE, CycleVeinMinerModePayload.STREAM_CODEC, ModNetworking::handleCycleVeinMinerMode);
        registrar.playToServer(FlashStepPayload.TYPE, FlashStepPayload.STREAM_CODEC, ModNetworking::handleFlashStep);
        registrar.playToClient(ResonanceHighlightPayload.TYPE, ResonanceHighlightPayload.STREAM_CODEC, ModNetworking::handleResonanceHighlights);
        registrar.playToClient(GeodeSearchPayload.TYPE, GeodeSearchPayload.STREAM_CODEC, ModNetworking::handleGeodeSearch);
        registrar.playToClient(ChainedMiningAnimationPayload.TYPE, ChainedMiningAnimationPayload.STREAM_CODEC, ModNetworking::handleChainedMiningAnimation);
        registrar.playToServer(PromoteRoutedEnchantmentPayload.TYPE, PromoteRoutedEnchantmentPayload.STREAM_CODEC, ModNetworking::handlePromoteRoutedEnchantment);
        registrar.playToServer(SelectPedestalUpgradePayload.TYPE, SelectPedestalUpgradePayload.STREAM_CODEC, ModNetworking::handleSelectPedestalUpgrade);
        registrar.playToServer(TakeMachineDisplayPayload.TYPE, TakeMachineDisplayPayload.STREAM_CODEC, ModNetworking::handleTakeMachineDisplay);
        registrar.playToServer(RequestInteractiveEnchantingStatePayload.TYPE, RequestInteractiveEnchantingStatePayload.STREAM_CODEC, ModNetworking::handleRequestInteractiveEnchantingState);
        registrar.playToServer(SelectInteractiveEnchantingOptionPayload.TYPE, SelectInteractiveEnchantingOptionPayload.STREAM_CODEC, ModNetworking::handleSelectInteractiveEnchantingOption);
        registrar.playToClient(InteractiveEnchantingStatePayload.TYPE, InteractiveEnchantingStatePayload.STREAM_CODEC, ModNetworking::handleInteractiveEnchantingState);
    }

    private static void handleInteractiveEnchantingState(InteractiveEnchantingStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> InteractiveEnchantingOverlay.acceptState(payload));
    }

    private static void handleRequestInteractiveEnchantingState(
            RequestInteractiveEnchantingStatePayload payload,
            IPayloadContext context
    ) {
        if (context.player() instanceof ServerPlayer player) {
            sendInteractiveEnchantingState(player, payload.tablePos(), -1);
        }
    }

    private static void handleSelectInteractiveEnchantingOption(
            SelectInteractiveEnchantingOptionPayload payload,
            IPayloadContext context
    ) {
        if (context.player() instanceof ServerPlayer player) {
            sendInteractiveEnchantingState(player, payload.tablePos(), payload.option());
        }
    }

    private static void sendInteractiveEnchantingState(ServerPlayer player, net.minecraft.core.BlockPos tablePos,
                                                        int selectedOption) {
        if ((selectedOption >= 0 && !BetterEnchantingConfig.usesInteractiveEnchanting())
                || !EffectiveBalance.takesOverEnchantingTable()
                || selectedOption < -1 || selectedOption >= 3
                || player.distanceToSqr(Vec3.atCenterOf(tablePos)) > 64.0D
                || !player.level().getBlockState(tablePos).is(Blocks.ENCHANTING_TABLE)
                || !(player.level().getBlockEntity(tablePos) instanceof EnchantingTableStorage)) {
            return;
        }

        EnhancedEnchantingMenu menu = new EnhancedEnchantingMenu(
                -1,
                player.getInventory(),
                ContainerLevelAccess.create(player.level(), tablePos),
                tablePos
        );
        try {
            if (selectedOption >= 0) {
                menu.clickMenuButton(player, selectedOption);
            }
            List<InteractiveEnchantingStatePayload.Option> options = new ArrayList<>(3);
            for (int option = 0; option < 3; option++) {
                List<InteractiveEnchantingStatePayload.Clue> clues = new ArrayList<>();
                for (int clueIndex = 0; clueIndex < menu.getRevealedClueCount(option); clueIndex++) {
                    clues.add(new InteractiveEnchantingStatePayload.Clue(
                            menu.getRevealedClueId(option, clueIndex),
                            menu.getRevealedClueLevel(option, clueIndex)
                    ));
                }
                options.add(new InteractiveEnchantingStatePayload.Option(
                        menu.requirements[option],
                        menu.costs[option],
                        menu.getDisabledReasonFlags(option),
                        menu.isOverlevelOffer(option),
                        menu.isApothicInfusionOffer(option),
                        menu.areAllCluesRevealed(option),
                        clues
                ));
            }
            PacketDistributor.sendToPlayer(player, new InteractiveEnchantingStatePayload(
                    tablePos,
                    menu.getReagentCount(),
                    options
            ));
        } finally {
            menu.releasePreviewListener();
        }
    }

    private static void handleTakeMachineDisplay(TakeMachineDisplayPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !player.getMainHandItem().isEmpty()
                || player.distanceToSqr(Vec3.atCenterOf(payload.machinePos())) > 64.0D) {
            return;
        }
        int takenStacks;
        ItemStack taken = ItemStack.EMPTY;
        if (player.level().getBlockEntity(payload.machinePos()) instanceof ArcaneCrucibleBlockEntity crucible) {
            if (payload.takeAll()) {
                takenStacks = InWorldMachineInteraction.takeAll(
                        crucible,
                        player,
                        ArcaneCrucibleBlockEntity.OUTPUT_SLOT,
                        ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT + 1,
                        ArcaneCrucibleBlockEntity.FIRST_CATALYST_SLOT,
                        ArcaneCrucibleBlockEntity.MEDIUM_SLOT
                );
            } else if (payload.slot() >= 0 && payload.slot() < ArcaneCrucibleBlockEntity.CONTAINER_SIZE) {
                taken = InWorldMachineInteraction.take(crucible, payload.slot(), player);
                takenStacks = taken.isEmpty() ? 0 : 1;
            } else {
                return;
            }
        } else if (player.level().getBlockEntity(payload.machinePos()) instanceof AttunementPedestalBlockEntity pedestal) {
            if (payload.takeAll()) {
                takenStacks = InWorldMachineInteraction.takeAll(
                        pedestal,
                        player,
                        AttunementPedestalBlockEntity.TARGET_SLOT,
                        AttunementPedestalBlockEntity.CATALYST_SLOT,
                        AttunementPedestalBlockEntity.ESSENCE_SLOT
                );
            } else if (payload.slot() >= 0 && payload.slot() < AttunementPedestalBlockEntity.CONTAINER_SIZE) {
                taken = InWorldMachineInteraction.take(pedestal, payload.slot(), player);
                takenStacks = taken.isEmpty() ? 0 : 1;
            } else {
                return;
            }
        } else if (BetterEnchantingConfig.usesInteractiveEnchanting()
                && player.level().getBlockState(payload.machinePos()).is(Blocks.ENCHANTING_TABLE)
                && player.level().getBlockEntity(payload.machinePos()) instanceof EnchantingTableStorage storage) {
            if (payload.takeAll()) {
                takenStacks = InWorldMachineInteraction.takeAll(
                        storage.betterenchanting$getEnchantingInventory(),
                        player,
                        EnhancedEnchantingMenu.TARGET_SLOT,
                        EnhancedEnchantingMenu.FIRST_MODIFIER_SLOT + 2,
                        EnhancedEnchantingMenu.FIRST_MODIFIER_SLOT + 1,
                        EnhancedEnchantingMenu.FIRST_MODIFIER_SLOT,
                        EnhancedEnchantingMenu.REAGENT_SLOT
                );
            } else if (payload.slot() >= 0 && payload.slot() < EnhancedEnchantingMenu.ENCHANTING_SLOT_COUNT) {
                taken = InWorldMachineInteraction.take(
                        storage.betterenchanting$getEnchantingInventory(),
                        payload.slot(),
                        player
                );
                takenStacks = taken.isEmpty() ? 0 : 1;
            } else {
                return;
            }
        } else {
            return;
        }
        if (takenStacks <= 0) {
            return;
        }
        player.level().playSound(null, payload.machinePos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.65F, 0.95F);
        player.displayClientMessage(
                payload.takeAll()
                        ? Component.translatable("message.betterenchanting.machine.cleared", takenStacks)
                        : Component.translatable("message.betterenchanting.machine.took", taken.getHoverName()),
                true
        );
    }

    private static void handleSelectPedestalUpgrade(SelectPedestalUpgradePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(Vec3.atCenterOf(payload.pedestalPos())) > 64.0D
                || (!player.getMainHandItem().is(ModItems.ATTUNEMENT_FOCUS.get())
                && !player.getOffhandItem().is(ModItems.ATTUNEMENT_FOCUS.get()))) {
            return;
        }
        if (!(player.level().getBlockEntity(payload.pedestalPos()) instanceof AttunementPedestalBlockEntity pedestal)) {
            return;
        }
        boolean alreadySelected = payload.enchantment().equals(pedestal.selectedEnchantment())
                && payload.partIndex() == pedestal.selectedPartIndex();
        if (alreadySelected) {
            UpgradePlan plan = pedestal.upgradePlan();
            if (pedestal.tryUpgrade(player)) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.betterenchanting.pedestal.upgraded",
                                Enchantment.getFullname(plan.enchantment(), plan.nextLevel())
                        ),
                        true
                );
            } else {
                player.displayClientMessage(upgradeFailureMessage(plan, player), true);
                player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.65F, 0.75F);
            }
            return;
        }
        if (!pedestal.selectEnchantment(payload.partIndex(), payload.enchantment())) {
            return;
        }
        player.level().playSound(
                null,
                payload.pedestalPos(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS,
                1.0F,
                1.3F
        );
        UpgradePlan selectedPlan = pedestal.upgradePlan();
        if (selectedPlan.enchantment() != null) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.betterenchanting.pedestal.selected",
                            Enchantment.getFullname(selectedPlan.enchantment(), selectedPlan.currentLevel())
                    ),
                    true
            );
        }
    }

    private static Component upgradeFailureMessage(UpgradePlan plan, ServerPlayer player) {
        if (!plan.validSelection() || plan.maximumReached()) {
            return Component.translatable("gui.betterenchanting.pedestal.maximum");
        }
        if (!plan.linkedTable()) {
            return Component.translatable("gui.betterenchanting.pedestal.no_table");
        }
        if (!plan.enoughPower()) {
            return Component.translatable(
                    "message.betterenchanting.pedestal.power_needed",
                    plan.availablePower(),
                    plan.requiredPower()
            );
        }
        if (!plan.matchingEssence()) {
            return Component.translatable("gui.betterenchanting.pedestal.wrong_essence");
        }
        if (!plan.enoughEssence()) {
            return Component.translatable("gui.betterenchanting.pedestal.need_essence", plan.essenceCost());
        }
        if (plan.catalystRequired() && !plan.hasCatalyst()) {
            return Component.translatable("gui.betterenchanting.pedestal.need_star");
        }
        return Component.translatable("gui.betterenchanting.pedestal.unavailable");
    }

    private static void handleCycleVeinMinerMode(CycleVeinMinerModePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            VeinMinerModeState.cycle(player);
        }
    }

    private static void handleFlashStep(FlashStepPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            FlashStepEnchantmentEvents.tryFlashStep(player);
        }
    }

    private static void handleResonanceHighlights(ResonanceHighlightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ResonanceHighlights.add(payload));
    }

    private static void handleGeodeSearch(GeodeSearchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ResonanceHighlights.add(payload));
    }

    private static void handleChainedMiningAnimation(ChainedMiningAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ChainedMiningAnimationState.update(payload));
    }

    private static void handlePromoteRoutedEnchantment(PromoteRoutedEnchantmentPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (player.distanceToSqr(Vec3.atCenterOf(payload.stationPos())) > 64.0D) {
            return;
        }

        boolean wasActive = isStationEnchantmentActive(player, payload);
        boolean hadBonus = isStationOverlevelBonusFocused(player, payload);
        if (MobsToolForgingCompat.promoteStationRoutedEnchantment(player.level(), payload.stationPos(), payload.partIndex(), payload.enchantment())) {
            player.containerMenu.broadcastChanges();
            boolean becameActive = !wasActive && isStationEnchantmentActive(player, payload);
            boolean gainedBonus = !hadBonus && isStationOverlevelBonusFocused(player, payload);
            if (becameActive || gainedBonus) {
                float pitch = 1.25F + player.level().random.nextFloat() * 0.15F;
                Vec3 particlePosition = activationParticlePosition(payload);
                ServerLevel level = player.serverLevel();
                level.sendParticles(
                        ParticleTypes.ENCHANT,
                        particlePosition.x,
                        particlePosition.y,
                        particlePosition.z,
                        34,
                        0.22D,
                        0.18D,
                        0.22D,
                        0.06D
                );
                player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.8F, pitch);
                player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.15F);
                player.level().playSound(
                        null,
                        payload.stationPos(),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.BLOCKS,
                        1.15F,
                        pitch
                );
                player.level().playSound(
                        null,
                        particlePosition.x,
                        particlePosition.y,
                        particlePosition.z,
                        SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS,
                        0.8F,
                        1.15F
                );
            }
        }
    }

    private static Vec3 activationParticlePosition(PromoteRoutedEnchantmentPayload payload) {
        Vec3 stationCenter = Vec3.atCenterOf(payload.stationPos());
        Vec3 position = new Vec3(payload.orbX(), payload.orbY(), payload.orbZ());
        if (!Double.isFinite(position.x)
                || !Double.isFinite(position.y)
                || !Double.isFinite(position.z)
                || position.distanceToSqr(stationCenter) > 16.0D) {
            return stationCenter.add(0.0D, 1.0D, 0.0D);
        }
        return position;
    }

    private static boolean isStationEnchantmentActive(ServerPlayer player, PromoteRoutedEnchantmentPayload payload) {
        return stationEnchantmentState(player, payload)
                .map(RoutedEnchantmentState::active)
                .orElse(false);
    }

    private static boolean isStationOverlevelBonusFocused(ServerPlayer player, PromoteRoutedEnchantmentPayload payload) {
        return stationEnchantmentState(player, payload)
                .map(RoutedEnchantmentState::overlevelBonusActive)
                .orElse(false);
    }

    private static Optional<RoutedEnchantmentState> stationEnchantmentState(
            ServerPlayer player,
            PromoteRoutedEnchantmentPayload payload
    ) {
        return MobsToolForgingCompat.stationRoutedEnchantmentPreview(player.level(), payload.stationPos())
                .flatMap(preview -> preview.breakdown())
                .flatMap(breakdown -> payload.partIndex() < 0
                        ? breakdown.toolEnchantments().stream()
                        .filter(enchantment -> enchantment.enchantmentId().equals(payload.enchantment()))
                        .findFirst()
                        : breakdown.parts().stream()
                        .filter(part -> part.partIndex() == payload.partIndex())
                        .flatMap(part -> part.enchantments().stream())
                        .filter(enchantment -> enchantment.enchantmentId().equals(payload.enchantment()))
                        .findFirst());
    }
}
