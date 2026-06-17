package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BetterEnchanting.MOD_ID);

    public static final DeferredBlock<Block> ARCANE_CRUCIBLE = BLOCKS.registerSimpleBlock(
            "arcane_crucible",
            BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE)
    );

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
