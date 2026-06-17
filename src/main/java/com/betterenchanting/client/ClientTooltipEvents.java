package com.betterenchanting.client;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.data.TagDisplayRules;
import com.betterenchanting.data.TagDisplayRules.TagLabel;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
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
        List<TagLabel> essenceTags = essenceTags(stack);
        List<TagLabel> itemTags = TagDisplayRules.itemLabels(stack);
        List<TagLabel> enchantmentTags = TagDisplayRules.enchantmentLabels(stack);
        List<TagLabel> targetTags = stack.is(Items.ENCHANTED_BOOK) ? TagDisplayRules.enchantmentTargetLabels(stack) : List.of();

        addTagLine(tooltip, "Essence Tags", essenceTags);
        addTagLine(tooltip, "Item Tags", itemTags);
        addTagLine(tooltip, "Enchantment Tags", enchantmentTags);
        addTagLine(tooltip, "Can Apply To", targetTags);
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
