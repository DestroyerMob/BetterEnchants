package com.betterenchanting.client;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.data.TagDisplayRules;
import com.betterenchanting.data.TagDisplayRules.TagLabel;
import com.betterenchanting.world.EnchantmentActivationEvents;
import com.betterenchanting.world.EnchantmentActivationEvents.InactiveReason;
import com.betterenchanting.world.EnchantmentActivationEvents.TooltipEntry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = BetterEnchanting.MOD_ID, value = Dist.CLIENT)
public final class ClientTooltipEvents {
    private ClientTooltipEvents() {
    }

    public static TextColor dominantAffinityColor(Holder<Enchantment> enchantment) {
        return TagDisplayRules.dominantAffinityColor(enchantment);
    }

    @SubscribeEvent
    public static void addTagTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        recolorEnchantmentTooltips(event, tooltip);

        List<TagLabel> essenceTags = essenceTags(stack);
        List<TagLabel> itemTags = TagDisplayRules.itemLabels(stack);
        List<TagLabel> enchantmentTags = TagDisplayRules.enchantmentLabels(stack);
        List<TagLabel> targetTags = stack.is(Items.ENCHANTED_BOOK) ? TagDisplayRules.enchantmentTargetLabels(stack) : List.of();

        addTagLine(tooltip, "Essence Tags", essenceTags);
        addTagLine(tooltip, "Item Tags", itemTags);
        addTagLine(tooltip, "Enchantment Tags", enchantmentTags);
        addTagLine(tooltip, "Can Apply To", targetTags);
    }

    private static void recolorEnchantmentTooltips(ItemTooltipEvent event, List<Component> tooltip) {
        HolderLookup.Provider registries = event.getContext().registries();
        if (registries == null) {
            return;
        }

        int searchFrom = 0;
        for (TooltipEntry entry : EnchantmentActivationEvents.tooltipEntries(event.getItemStack(), registries)) {
            String vanillaText = Enchantment.getFullname(entry.enchantment(), entry.level()).getString();
            int lineIndex = findTooltipLine(tooltip, vanillaText, searchFrom);
            if (lineIndex < 0) {
                continue;
            }
            tooltip.set(lineIndex, styledEnchantmentLine(entry));
            searchFrom = lineIndex + 1;
        }
    }

    private static int findTooltipLine(List<Component> tooltip, String text, int startIndex) {
        for (int index = startIndex; index < tooltip.size(); index++) {
            if (tooltip.get(index).getString().equals(text)) {
                return index;
            }
        }
        return -1;
    }

    private static Component styledEnchantmentLine(TooltipEntry entry) {
        MutableComponent line = Enchantment.getFullname(entry.enchantment(), entry.level()).copy()
                .withStyle(style -> style.withColor(dominantAffinityColor(entry.enchantment())));
        if (!entry.status().active()) {
            line.withStyle(style -> style.withStrikethrough(true));
            appendInactiveReason(line, entry, InactiveReason.WRONG_TAG);
            appendInactiveReason(line, entry, InactiveReason.OVER_LIMIT);
        }
        return line;
    }

    private static void appendInactiveReason(MutableComponent line, TooltipEntry entry, InactiveReason reason) {
        if (entry.status().has(reason)) {
            line.append(Component.literal(" [" + reason.label() + "]").withStyle(ChatFormatting.RED));
        }
    }

    private static List<TagLabel> essenceTags(ItemStack stack) {
        return EssenceDefinitions.get(stack)
                .map(definition -> definition.tags().stream()
                        .map(ClientTooltipEvents::essenceTagLabel)
                        .toList())
                .orElse(List.of());
    }

    private static TagLabel essenceTagLabel(ResourceLocation tag) {
        return TagDisplayRules.labelFor(tag);
    }

    private static void addTagLine(List<Component> tooltip, String prefix, List<TagLabel> labels) {
        if (!labels.isEmpty()) {
            tooltip.add(tagLine(prefix, labels));
        }
    }

    private static Component tagLine(String prefix, List<TagLabel> labels) {
        MutableComponent line = Component.literal(prefix + ": ").withStyle(ChatFormatting.DARK_GRAY);
        for (int index = 0; index < labels.size(); index++) {
            if (index > 0) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            }
            TagLabel label = labels.get(index);
            line.append(Component.literal(label.text()).withStyle(style -> style
                    .withColor(label.color())
                    .withUnderlined(true)));
        }
        return line;
    }
}
