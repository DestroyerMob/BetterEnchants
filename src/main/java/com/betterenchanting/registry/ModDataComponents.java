package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, BetterEnchanting.MOD_ID);

    public static final Supplier<DataComponentType<Boolean>> OVERLEVELED = COMPONENTS.register(
            "overleveled",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );

    public static final Supplier<DataComponentType<List<ResourceLocation>>> ROUTED_ENCHANTMENT_PRIORITY = COMPONENTS.register(
            "routed_enchantment_priority",
            () -> DataComponentType.<List<ResourceLocation>>builder()
                    .persistent(ResourceLocation.CODEC.listOf())
                    .networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build()
    );

    public static final Supplier<DataComponentType<List<ResourceLocation>>> ROUTED_OVERLEVEL_BONUS_PRIORITY = COMPONENTS.register(
            "routed_overlevel_bonus_priority",
            () -> DataComponentType.<List<ResourceLocation>>builder()
                    .persistent(ResourceLocation.CODEC.listOf())
                    .networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build()
    );

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
