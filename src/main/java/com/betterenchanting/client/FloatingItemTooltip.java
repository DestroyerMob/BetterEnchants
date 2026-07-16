package com.betterenchanting.client;

import com.betterenchanting.compat.MobsToolForgingCompat;
import com.betterenchanting.compat.MobsToolForgingCompat.RoutedEnchantmentState;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** One hover-card style and one enchantment-reading path for every floating station item. */
final class FloatingItemTooltip {
    private FloatingItemTooltip() {
    }

    static List<Component> enchantmentLines(ClientLevel level, ItemStack stack) {
        Map<ResourceLocation, EnchantmentLine> lines = new LinkedHashMap<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry
                : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            addLine(lines, entry.getKey(), entry.getIntValue());
        }
        MobsToolForgingCompat.routedEnchantmentBreakdown(level.registryAccess(), stack).ifPresent(breakdown -> {
            for (RoutedEnchantmentState state : breakdown.parts().stream()
                    .flatMap(part -> part.enchantments().stream())
                    .filter(RoutedEnchantmentState::active)
                    .toList()) {
                addLine(lines, state.enchantment(), Math.max(state.level(), state.effectiveLevel()));
            }
        });

        List<Component> result = new ArrayList<>(lines.size());
        for (EnchantmentLine line : lines.values()) {
            result.add(Enchantment.getFullname(line.enchantment(), line.level()).copy()
                    .withStyle(style -> style.withColor(
                            ClientTooltipEvents.dominantAffinityColor(line.enchantment()))));
        }
        return List.copyOf(result);
    }

    static void renderCard(GuiGraphics graphics, Font font, ItemStack stack, List<Component> enchantments,
                           Component action, int screenWidth, int screenHeight) {
        Component name = stack.getHoverName();
        int contentWidth = Math.max(font.width(name), font.width(action));
        for (Component enchantment : enchantments) {
            contentWidth = Math.max(contentWidth, font.width(enchantment));
        }
        int width = Math.min(contentWidth + 20, screenWidth - 16);
        int height = 28 + enchantments.size() * 11;
        int x = Math.max(8, Math.min(screenWidth - width - 8, screenWidth / 2 + 13));
        int y = Math.max(8, Math.min(screenHeight - height - 8, screenHeight / 2 + 12));
        graphics.fill(x, y, x + width, y + height, AttunementUiTheme.HOVER_FILL);
        graphics.renderOutline(x, y, width, height, AttunementUiTheme.HOVER_BORDER);
        graphics.drawString(font, name, x + 8, y + 5, AttunementUiTheme.TEXT_COLOR, true);
        for (int index = 0; index < enchantments.size(); index++) {
            graphics.drawString(font, enchantments.get(index), x + 8, y + 16 + index * 11,
                    AttunementUiTheme.ENCHANTMENT_TEXT, false);
        }
        graphics.drawString(font, action, x + 8, y + 16 + enchantments.size() * 11,
                AttunementUiTheme.MUTED_COLOR, false);
    }

    private static void addLine(Map<ResourceLocation, EnchantmentLine> lines,
                                Holder<Enchantment> enchantment, int level) {
        ResourceLocation id = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (id == null || level <= 0) {
            return;
        }
        EnchantmentLine existing = lines.get(id);
        if (existing == null || level > existing.level()) {
            lines.put(id, new EnchantmentLine(enchantment, level));
        }
    }

    private record EnchantmentLine(Holder<Enchantment> enchantment, int level) {
    }
}
