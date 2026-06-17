package com.betterenchanting.registry;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> RULE_PLAYER_GRIEFING = GameRules.register(
            "playerGriefing",
            GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(true)
    );

    private ModGameRules() {
    }

    public static void init() {
    }
}
