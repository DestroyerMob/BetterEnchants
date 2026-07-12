package com.betterenchanting.compat;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;

public final class EnchantmentDescriptionsCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile boolean reflectionAttempted;
    private static volatile boolean runtimeWarningLogged;
    private static volatile Reflection reflection;

    private EnchantmentDescriptionsCompat() {
    }

    public static void appendApothicDescription(
            Holder<Enchantment> enchantment,
            int level,
            ItemEnchantments enchantments,
            Consumer<Component> tooltip
    ) {
        if (level <= 0) {
            return;
        }

        Reflection access = reflection();
        if (access == null) {
            return;
        }

        try {
            if (access.apothicInlineDescriptions().getBoolean(null)) {
                return;
            }

            Object heldStack = access.enchantmentContextStack().invoke(enchantments);
            if (!(heldStack instanceof ItemStack stack) || stack.isEmpty()) {
                return;
            }

            Object mod = access.enchantmentDescriptionsInstance();
            if (!(boolean) access.canDisplayDescription().invoke(mod, stack)
                    || !(boolean) access.isKeybindConditionMet().invoke(mod)) {
                return;
            }

            access.insertDescriptions().invoke(mod, enchantment, level, tooltip);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logRuntimeWarning(exception);
        }
    }

    private static Reflection reflection() {
        if (!reflectionAttempted) {
            synchronized (EnchantmentDescriptionsCompat.class) {
                if (!reflectionAttempted) {
                    reflection = loadReflection();
                    reflectionAttempted = true;
                }
            }
        }
        return reflection;
    }

    private static Reflection loadReflection() {
        try {
            Class<?> descriptionsClass = Class.forName("net.darkhax.enchdesc.common.impl.EnchdescMod");
            Object descriptionsInstance = descriptionsClass.getMethod("getInstance").invoke(null);
            Method canDisplayDescription = descriptionsClass.getMethod("canDisplayDescription", ItemStack.class);
            Method isKeybindConditionMet = descriptionsClass.getMethod("isKeybindConditionMet");
            Method insertDescriptions = descriptionsClass.getMethod(
                    "insertDescriptions",
                    Holder.class,
                    int.class,
                    Consumer.class
            );
            Method enchantmentContextStack = ItemEnchantments.class.getMethod("enchdesc$getStack");
            Field apothicInlineDescriptions = Class.forName(
                    "dev.shadowsoffire.apothic_enchanting.ApothEnchConfig"
            ).getField("enableInlineEnchDescs");
            return new Reflection(
                    descriptionsInstance,
                    canDisplayDescription,
                    isKeybindConditionMet,
                    insertDescriptions,
                    enchantmentContextStack,
                    apothicInlineDescriptions
            );
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static void logRuntimeWarning(Throwable exception) {
        if (!runtimeWarningLogged) {
            runtimeWarningLogged = true;
            LOGGER.warn("Unable to bridge Apothic Enchanting tooltips to Enchantment Descriptions", exception);
        }
    }

    private record Reflection(
            Object enchantmentDescriptionsInstance,
            Method canDisplayDescription,
            Method isKeybindConditionMet,
            Method insertDescriptions,
            Method enchantmentContextStack,
            Field apothicInlineDescriptions
    ) {
    }
}
