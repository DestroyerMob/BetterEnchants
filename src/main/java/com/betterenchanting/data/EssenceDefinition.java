package com.betterenchanting.data;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record EssenceDefinition(
        ResourceLocation item,
        List<ResourceLocation> tags,
        List<ResourceLocation> removedTags,
        double weightMultiplier,
        boolean restrictsPool,
        boolean appliesToAllOffers,
        boolean blocksOffer
) {
}
