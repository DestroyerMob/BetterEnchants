package com.betterenchanting.data;

import com.betterenchanting.BetterEnchanting;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class AffinityRoles {
    private static final String PRIMARY_PREFIX = "primary/";
    private static final String SECONDARY_PREFIX = "secondary/";

    private AffinityRoles() {
    }

    public static Set<ResourceLocation> primaryAffinities(Holder<Enchantment> enchantment) {
        Set<ResourceLocation> affinities = new LinkedHashSet<>();
        enchantment.tags()
                .map(TagKey::location)
                .map(AffinityRoles::primaryAffinityId)
                .flatMap(java.util.Optional::stream)
                .forEach(affinities::add);
        return Set.copyOf(affinities);
    }

    public static AffinityRole role(Holder<Enchantment> enchantment, ResourceLocation affinity) {
        if (!enchantment.is(TagKey.create(Registries.ENCHANTMENT, affinity))) {
            return AffinityRole.NONE;
        }
        if (enchantment.is(classificationTag(PRIMARY_PREFIX, affinity))) {
            return AffinityRole.PRIMARY;
        }
        if (enchantment.is(classificationTag(SECONDARY_PREFIX, affinity))) {
            return AffinityRole.SECONDARY;
        }
        if (!primaryAffinities(enchantment).isEmpty()) {
            return AffinityRole.SECONDARY;
        }
        return AffinityRole.UNCLASSIFIED;
    }

    public static boolean isClassificationTag(ResourceLocation tagId) {
        if (!tagId.getNamespace().equals(BetterEnchanting.MOD_ID)) {
            return false;
        }
        return tagId.getPath().startsWith(PRIMARY_PREFIX) || tagId.getPath().startsWith(SECONDARY_PREFIX);
    }

    private static java.util.Optional<ResourceLocation> primaryAffinityId(ResourceLocation tagId) {
        if (!tagId.getNamespace().equals(BetterEnchanting.MOD_ID) || !tagId.getPath().startsWith(PRIMARY_PREFIX)) {
            return java.util.Optional.empty();
        }
        String path = tagId.getPath().substring(PRIMARY_PREFIX.length());
        return path.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(BetterEnchanting.id(path));
    }

    private static TagKey<Enchantment> classificationTag(String prefix, ResourceLocation affinity) {
        return TagKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(affinity.getNamespace(), prefix + affinity.getPath())
        );
    }

    public enum AffinityRole {
        PRIMARY,
        SECONDARY,
        UNCLASSIFIED,
        NONE
    }
}
