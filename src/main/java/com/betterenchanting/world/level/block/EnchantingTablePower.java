package com.betterenchanting.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;

public final class EnchantingTablePower {
    private EnchantingTablePower() {
    }

    public static int bookshelfPower(Level level, BlockPos pos) {
        int power = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                power += (int) level.getBlockState(pos.offset(offset)).getEnchantPowerBonus(level, pos.offset(offset));
            }
        }
        return power;
    }
}
