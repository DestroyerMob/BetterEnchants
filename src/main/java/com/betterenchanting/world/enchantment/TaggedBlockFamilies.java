package com.betterenchanting.world.enchantment;

import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class TaggedBlockFamilies {
    private TaggedBlockFamilies() {
    }

    static Set<TagKey<Block>> logFamilies(BlockState state) {
        return state.getTags()
                .filter(tag -> isLogFamilyTag(tag.location()))
                .collect(Collectors.toUnmodifiableSet());
    }

    static Set<TagKey<Block>> oreFamilies(BlockState state) {
        return state.getTags()
                .filter(tag -> isOreFamilyTag(tag.location()))
                .collect(Collectors.toUnmodifiableSet());
    }

    static boolean matches(BlockState origin, BlockState candidate, Set<TagKey<Block>> familyTags) {
        if (candidate.is(origin.getBlock())) {
            return true;
        }
        for (TagKey<Block> familyTag : familyTags) {
            if (candidate.is(familyTag)) {
                return true;
            }
        }
        return false;
    }

    static boolean isLogFamilyTag(ResourceLocation id) {
        return BlockFamilyTagNames.isLogFamilyPath(id.getPath());
    }

    static boolean isOreFamilyTag(ResourceLocation id) {
        return BlockFamilyTagNames.isOreFamilyPath(id.getPath());
    }
}
