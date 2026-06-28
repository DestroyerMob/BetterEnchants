package com.betterenchanting.world.enchantment;

public enum VeinMinerMode {
    CONNECTED_MATCHING("vein_miner_mode.betterenchanting.connected_matching", true, true),
    RADIUS_MATCHING("vein_miner_mode.betterenchanting.radius_matching", false, true),
    CONNECTED_ANY_ORE("vein_miner_mode.betterenchanting.connected_any_ore", true, false),
    RADIUS_ANY_ORE("vein_miner_mode.betterenchanting.radius_any_ore", false, false);

    private static final VeinMinerMode[] VALUES = values();
    private final String translationKey;
    private final boolean connectedSearch;
    private final boolean matchingOre;

    VeinMinerMode(String translationKey, boolean connectedSearch, boolean matchingOre) {
        this.translationKey = translationKey;
        this.connectedSearch = connectedSearch;
        this.matchingOre = matchingOre;
    }

    public VeinMinerMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean connectedSearch() {
        return connectedSearch;
    }

    public boolean matchingOre() {
        return matchingOre;
    }
}
