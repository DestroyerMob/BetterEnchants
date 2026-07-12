package com.betterenchanting.compat;

import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/** Optional bridge to MoreWeapons' base Iaijutsu strike signal. */
public final class MoreWeaponsIaiCompat {
    private static Method fullyChargedStrike;
    private static boolean initialized;

    private MoreWeaponsIaiCompat() {
    }

    public static boolean isFullyChargedStrike(Player player, Entity target) {
        if (!ModList.get().isLoaded("mobsmoreweapons")) {
            return false;
        }
        initialize();
        if (fullyChargedStrike == null) {
            return false;
        }
        try {
            return (boolean) fullyChargedStrike.invoke(null, player, target);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> stanceSystem = Class.forName("org.destroyermob.mobsmoreweapons.combat.IaiStanceSystem");
            fullyChargedStrike = stanceSystem.getMethod("isFullyChargedStrike", Player.class, Entity.class);
        } catch (ReflectiveOperationException exception) {
            fullyChargedStrike = null;
        }
    }
}
