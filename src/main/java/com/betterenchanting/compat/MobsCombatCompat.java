package com.betterenchanting.compat;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

public final class MobsCombatCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "mobscombat";
    private static volatile boolean reflectionAttempted;
    private static volatile boolean runtimeWarningLogged;
    private static volatile Reflection reflection;

    private MobsCombatCompat() {
    }

    public static Optional<ParryCounter> parryCounter(ServerPlayer player) {
        Reflection access = reflection();
        if (access == null) {
            return Optional.empty();
        }
        try {
            Object state = access.getCombatState().invoke(null, player);
            if (state == null) {
                return Optional.empty();
            }
            boolean active = (Boolean) access.hasParryCounterWindow().invoke(state);
            long sequence = ((Number) access.successfulParryCount().invoke(state)).longValue();
            return Optional.of(new ParryCounter(active, sequence));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
            return Optional.empty();
        }
    }

    public static void applyFlatSlashPostureDamage(
            ServerPlayer player,
            LivingEntity target,
            float amount
    ) {
        if (amount <= 0.0F) {
            return;
        }
        Reflection access = reflection();
        if (access == null) {
            return;
        }
        try {
            access.applyFlatPostureDamage().invoke(null, player, target, amount, access.slashDamageKind(), true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
        }
    }

    private static Reflection reflection() {
        if (reflectionAttempted) {
            return reflection;
        }
        synchronized (MobsCombatCompat.class) {
            if (reflectionAttempted) {
                return reflection;
            }
            try {
                if (!ModList.get().isLoaded(MOD_ID)) {
                    return null;
                }
                Class<?> managerClass = Class.forName("org.destroyermob.mobscombat.combat.CombatStateManager");
                Class<?> stateClass = Class.forName("org.destroyermob.mobscombat.combat.CombatState");
                Class<?> postureClass = Class.forName("org.destroyermob.mobscombat.combat.PostureSystem");
                Class<?> damageKindClass = Class.forName("org.destroyermob.mobscombat.combat.CombatDamageKind");
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object slash = Enum.valueOf((Class) damageKindClass.asSubclass(Enum.class), "SLASH");
                reflection = new Reflection(
                        managerClass.getMethod("get", LivingEntity.class),
                        stateClass.getMethod("hasParryCounterWindow"),
                        stateClass.getMethod("successfulParryCount"),
                        postureClass.getMethod(
                                "applyFlatPostureDamage",
                                LivingEntity.class,
                                LivingEntity.class,
                                float.class,
                                damageKindClass,
                                boolean.class
                        ),
                        slash
                );
                LOGGER.info("Enabled Mobs Combat parry compatibility");
            } catch (ClassNotFoundException exception) {
                LOGGER.debug("Mobs Combat not detected; Moonlit Reversal compatibility disabled");
            } catch (ReflectiveOperationException | LinkageError exception) {
                LOGGER.warn("Mobs Combat was detected, but Moonlit Reversal compatibility could not be enabled", exception);
            } finally {
                reflectionAttempted = true;
            }
            return reflection;
        }
    }

    private static void logRuntimeWarning(Throwable exception) {
        if (runtimeWarningLogged) {
            return;
        }
        runtimeWarningLogged = true;
        LOGGER.warn("Moonlit Reversal could not read or apply Mobs Combat state", exception);
    }

    public record ParryCounter(boolean active, long sequence) {
    }

    private record Reflection(
            Method getCombatState,
            Method hasParryCounterWindow,
            Method successfulParryCount,
            Method applyFlatPostureDamage,
            Object slashDamageKind
    ) {
    }
}
