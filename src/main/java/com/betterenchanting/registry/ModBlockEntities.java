package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.world.level.block.entity.ArcaneCrucibleBlockEntity;
import com.betterenchanting.world.level.block.entity.AttunementPedestalBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BetterEnchanting.MOD_ID);

    public static final Supplier<BlockEntityType<ArcaneCrucibleBlockEntity>> ARCANE_CRUCIBLE = BLOCK_ENTITY_TYPES.register(
            "arcane_crucible",
            () -> BlockEntityType.Builder.of(ArcaneCrucibleBlockEntity::new, ModBlocks.ARCANE_CRUCIBLE.get()).build(null)
    );
    public static final Supplier<BlockEntityType<AttunementPedestalBlockEntity>> ATTUNEMENT_PEDESTAL = BLOCK_ENTITY_TYPES.register(
            "attunement_pedestal",
            () -> BlockEntityType.Builder.of(AttunementPedestalBlockEntity::new, ModBlocks.ATTUNEMENT_PEDESTAL.get()).build(null)
    );

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
    }
}
