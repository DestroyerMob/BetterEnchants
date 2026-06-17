package com.betterenchanting.data;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record EssenceDefinition(
        ResourceLocation item,
        List<ResourceLocation> tags,
        double weightMultiplier,
        boolean restrictsPool,
        boolean removesCurses,
        boolean appliesToAllOffers,
        boolean blocksOffer
) {
}
