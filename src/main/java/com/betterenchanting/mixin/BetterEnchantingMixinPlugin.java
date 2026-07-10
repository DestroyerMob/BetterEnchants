package com.betterenchanting.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class BetterEnchantingMixinPlugin implements IMixinConfigPlugin {
    private static final String ANVIL_SCREEN_MIXIN = "com.betterenchanting.mixin.client.AnvilScreenMixin";
    private static final String APOTHIC_ENCHANTMENT_INFO_MIXIN = "com.betterenchanting.mixin.ApothicEnchantmentInfoMixin";
    private static final String APOTHIC_ENCHANTING_RESOURCE = "dev/shadowsoffire/apothic_enchanting/table/ApothEnchantmentScreen.class";
    private static final String APOTHIC_ENCHANTMENT_INFO_RESOURCE = "dev/shadowsoffire/apothic_enchanting/EnchantmentInfo.class";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (ANVIL_SCREEN_MIXIN.equals(mixinClassName)) {
            return !isResourceAvailable(APOTHIC_ENCHANTING_RESOURCE);
        }
        if (APOTHIC_ENCHANTMENT_INFO_MIXIN.equals(mixinClassName)) {
            return isResourceAvailable(APOTHIC_ENCHANTMENT_INFO_RESOURCE);
        }
        return true;
    }

    private static boolean isResourceAvailable(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader != null && loader.getResource(resource) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
