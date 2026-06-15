package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.world.level.block.ArcaneCrucibleBlock;
import java.util.function.Supplier;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final String ARCANE_CRUCIBLE_ID = "arcane_crucible";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BetterEnchanting.MOD_ID);

    public static final Supplier<ArcaneCrucibleBlock> ARCANE_CRUCIBLE = BLOCKS.register(
            ARCANE_CRUCIBLE_ID,
            () -> new ArcaneCrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)
                    .sound(SoundType.STONE)
                    .noOcclusion())
    );

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
