package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.world.level.block.ArcaneCrucibleBlock;
import com.betterenchanting.world.level.block.AttunementPedestalBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BetterEnchanting.MOD_ID);

    public static final DeferredBlock<ArcaneCrucibleBlock> ARCANE_CRUCIBLE = BLOCKS.register(
            "arcane_crucible",
            () -> new ArcaneCrucibleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE))
    );
    public static final DeferredBlock<AttunementPedestalBlock> ATTUNEMENT_PEDESTAL = BLOCKS.register(
            "attunement_pedestal",
            () -> new AttunementPedestalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE))
    );

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
