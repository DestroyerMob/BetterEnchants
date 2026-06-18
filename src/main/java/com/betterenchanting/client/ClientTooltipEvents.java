package com.betterenchanting.client;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.data.EnchantmentLimitRules;
import com.betterenchanting.data.EssenceDefinitions;
import com.betterenchanting.data.TagDisplayRules;
import com.betterenchanting.data.TagDisplayRules.TagLabel;
import com.betterenchanting.registry.ModEnchantments;
import com.betterenchanting.world.EnchantmentActivationEvents;
import com.betterenchanting.world.EnchantmentActivationEvents.InactiveReason;
import com.betterenchanting.world.EnchantmentActivationEvents.TooltipEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = BetterEnchanting.MOD_ID, value = Dist.CLIENT)
public final class ClientTooltipEvents {
    private static final List<SynergyDefinition> SYNERGY_DEFINITIONS = List.of(
            new SynergyDefinition(ModEnchantments.BEHEADING, Enchantments.LOOTING, "◆", 0xFFD44D),
            new SynergyDefinition(ModEnchantments.HEADSHOT, Enchantments.POWER, "◆◆", 0x55D6FF)
    );

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
        HolderLookup.Provider registries = tooltipRegistries(event);
        List<TooltipEntry> enchantmentEntries = registries == null
                ? List.of()
                : EnchantmentActivationEvents.tooltipEntries(stack, registries);
        List<ActiveSynergy> activeSynergies = activeSynergies(enchantmentEntries);
        Map<ResourceKey<Enchantment>, List<SynergyMarker>> synergyMarkers = synergyMarkers(activeSynergies);
        recolorEnchantmentTooltips(tooltip, enchantmentEntries, synergyMarkers);
        addSynergyLines(tooltip, activeSynergies);
        addEnchantmentLimitLine(tooltip, stack, enchantmentEntries);

        List<TagLabel> essenceTags = essenceTags(stack);
        List<TagLabel> itemTags = TagDisplayRules.itemLabels(stack);
        List<TagLabel> enchantmentTags = TagDisplayRules.enchantmentLabels(stack);
        List<TagLabel> targetTags = stack.is(Items.ENCHANTED_BOOK) ? TagDisplayRules.enchantmentTargetLabels(stack) : List.of();

        addTagLine(tooltip, Component.translatable("tooltip.betterenchanting.essence_tags"), essenceTags);
        addTagLine(tooltip, Component.translatable("tooltip.betterenchanting.item_tags"), itemTags);
        addTagLine(tooltip, Component.translatable("tooltip.betterenchanting.enchantment_tags"), enchantmentTags);
        addTagLine(tooltip, Component.translatable("tooltip.betterenchanting.can_apply_to"), targetTags);
    }

    private static void recolorEnchantmentTooltips(
            List<Component> tooltip,
            List<TooltipEntry> enchantmentEntries,
            Map<ResourceKey<Enchantment>, List<SynergyMarker>> synergyMarkers
    ) {
        if (enchantmentEntries.isEmpty()) {
            return;
        }

        int searchFrom = 0;
        for (TooltipEntry entry : enchantmentEntries) {
            int lineIndex = findTooltipLine(tooltip, entry, searchFrom);
            if (lineIndex < 0) {
                continue;
            }
            tooltip.set(lineIndex, styledEnchantmentLine(entry, markersFor(entry, synergyMarkers)));
            searchFrom = lineIndex + 1;
        }
    }

    private static List<ActiveSynergy> activeSynergies(List<TooltipEntry> enchantmentEntries) {
        if (enchantmentEntries.isEmpty()) {
            return List.of();
        }

        List<ActiveSynergy> synergies = new ArrayList<>();
        for (SynergyDefinition definition : SYNERGY_DEFINITIONS) {
            TooltipEntry first = activeEntry(enchantmentEntries, definition.first());
            TooltipEntry second = activeEntry(enchantmentEntries, definition.second());
            if (first != null && second != null) {
                synergies.add(new ActiveSynergy(definition, first, second));
            }
        }
        return synergies;
    }

    private static TooltipEntry activeEntry(List<TooltipEntry> enchantmentEntries, ResourceKey<Enchantment> key) {
        for (TooltipEntry entry : enchantmentEntries) {
            if (entry.status().active() && isEnchantment(entry, key)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean isEnchantment(TooltipEntry entry, ResourceKey<Enchantment> key) {
        return entry.enchantment().unwrapKey()
                .map(key::equals)
                .orElse(false);
    }

    private static Map<ResourceKey<Enchantment>, List<SynergyMarker>> synergyMarkers(List<ActiveSynergy> synergies) {
        if (synergies.isEmpty()) {
            return Map.of();
        }

        Map<ResourceKey<Enchantment>, List<SynergyMarker>> markers = new HashMap<>();
        for (ActiveSynergy synergy : synergies) {
            SynergyMarker marker = new SynergyMarker(synergy.definition().marker(), synergy.definition().color());
            addSynergyMarker(markers, synergy.definition().first(), marker);
            addSynergyMarker(markers, synergy.definition().second(), marker);
        }
        return markers;
    }

    private static void addSynergyMarker(
            Map<ResourceKey<Enchantment>, List<SynergyMarker>> markers,
            ResourceKey<Enchantment> enchantment,
            SynergyMarker marker
    ) {
        markers.computeIfAbsent(enchantment, ignored -> new ArrayList<>()).add(marker);
    }

    private static List<SynergyMarker> markersFor(
            TooltipEntry entry,
            Map<ResourceKey<Enchantment>, List<SynergyMarker>> synergyMarkers
    ) {
        return entry.enchantment().unwrapKey()
                .map(key -> synergyMarkers.getOrDefault(key, List.of()))
                .orElse(List.of());
    }

    private static void addSynergyLines(List<Component> tooltip, List<ActiveSynergy> synergies) {
        if (synergies.isEmpty()) {
            return;
        }

        tooltip.add(Component.translatable("tooltip.betterenchanting.synergy_links").withStyle(ChatFormatting.DARK_GRAY));
        for (ActiveSynergy synergy : synergies) {
            tooltip.add(Component.empty()
                    .append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(synergyMarkerComponent(new SynergyMarker(
                            synergy.definition().marker(),
                            synergy.definition().color()
                    )))
                    .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Enchantment.getFullname(synergy.first().enchantment(), synergy.first().level())
                            .copy()
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" + ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Enchantment.getFullname(synergy.second().enchantment(), synergy.second().level())
                            .copy()
                            .withStyle(ChatFormatting.GRAY)));
        }
    }

    private static void addEnchantmentLimitLine(List<Component> tooltip, ItemStack stack, List<TooltipEntry> enchantmentEntries) {
        if (!EnchantmentLimitRules.overridesVanillaLimits() || stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
            return;
        }

        int count = enchantmentEntries.isEmpty()
                ? EnchantmentLimitRules.currentEnchantmentCount(stack)
                : capacityRelevantEnchantmentCount(enchantmentEntries);
        if (count <= 0) {
            return;
        }

        int limit = EnchantmentLimitRules.maxEnchantments(stack);
        int baseLimit = EnchantmentLimitRules.baseMaxEnchantments(stack);
        ChatFormatting valueColor = capacityValueColor(enchantmentEntries, count, baseLimit, limit);
        MutableComponent line = Component.empty()
                .append(Component.translatable("tooltip.betterenchanting.enchantment_limit").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(count + "/" + limit).withStyle(valueColor));
        if (baseLimit != limit) {
            line.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable("tooltip.betterenchanting.enchantment_limit.base", baseLimit).withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(line);
    }

    private static int capacityRelevantEnchantmentCount(List<TooltipEntry> enchantmentEntries) {
        int count = 0;
        for (TooltipEntry entry : enchantmentEntries) {
            if (!entry.status().has(InactiveReason.WRONG_TAG)) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasOverLimitEntry(List<TooltipEntry> enchantmentEntries) {
        for (TooltipEntry entry : enchantmentEntries) {
            if (entry.status().has(InactiveReason.OVER_LIMIT)) {
                return true;
            }
        }
        return false;
    }

    private static ChatFormatting capacityValueColor(List<TooltipEntry> enchantmentEntries, int count, int baseLimit, int limit) {
        if (hasOverLimitEntry(enchantmentEntries) || count > limit) {
            return ChatFormatting.RED;
        }
        if (hasBonusCapacityEntry(enchantmentEntries) || count > baseLimit) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.GRAY;
    }

    private static boolean hasBonusCapacityEntry(List<TooltipEntry> enchantmentEntries) {
        for (TooltipEntry entry : enchantmentEntries) {
            if (entry.usesBonusCapacity()) {
                return true;
            }
        }
        return false;
    }

    private static HolderLookup.Provider tooltipRegistries(ItemTooltipEvent event) {
        HolderLookup.Provider registries = event.getContext().registries();
        if (registries != null) {
            return registries;
        }

        Player player = event.getEntity();
        if (player != null) {
            return player.level().registryAccess();
        }

        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : minecraft.level.registryAccess();
    }

    private static int findTooltipLine(List<Component> tooltip, TooltipEntry entry, int startIndex) {
        String vanillaText = Enchantment.getFullname(entry.enchantment(), entry.level()).getString();
        for (int index = startIndex; index < tooltip.size(); index++) {
            if (matchesTooltipLine(tooltip.get(index).getString(), vanillaText)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean matchesTooltipLine(String lineText, String vanillaText) {
        return lineText.equals(vanillaText) || lineText.endsWith(vanillaText);
    }

    private static Component styledEnchantmentLine(TooltipEntry entry, List<SynergyMarker> synergyMarkers) {
        MutableComponent line = Enchantment.getFullname(entry.enchantment(), entry.level()).copy()
                .withStyle(style -> style.withColor(dominantAffinityColor(entry.enchantment())));
        if (entry.usesBonusCapacity() || entry.status().has(InactiveReason.OVER_LIMIT)) {
            line.withStyle(style -> style.withItalic(true));
        }
        if (!entry.status().active()) {
            line.withStyle(style -> style.withStrikethrough(true));
            appendInactiveReason(line, entry, InactiveReason.WRONG_TAG);
            appendInactiveReason(line, entry, InactiveReason.OVER_LIMIT);
        }
        appendSynergyMarkers(line, synergyMarkers);
        return line;
    }

    private static void appendSynergyMarkers(MutableComponent line, List<SynergyMarker> synergyMarkers) {
        for (SynergyMarker marker : synergyMarkers) {
            line.append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(synergyMarkerComponent(marker));
        }
    }

    private static Component synergyMarkerComponent(SynergyMarker marker) {
        return Component.literal(marker.marker()).withStyle(style -> style
                .withColor(TextColor.fromRgb(marker.color()))
                .withBold(true)
                .withItalic(false));
    }

    private static void appendInactiveReason(MutableComponent line, TooltipEntry entry, InactiveReason reason) {
        if (entry.status().has(reason)) {
            line.append(Component.literal(" [").withStyle(ChatFormatting.RED)
                    .append(Component.translatable(reason.translationKey()).withStyle(ChatFormatting.RED))
                    .append(Component.literal("]").withStyle(ChatFormatting.RED)));
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

    private static void addTagLine(List<Component> tooltip, Component prefix, List<TagLabel> labels) {
        if (!labels.isEmpty()) {
            tooltip.add(tagLine(prefix, labels));
        }
    }

    private static Component tagLine(Component prefix, List<TagLabel> labels) {
        MutableComponent line = Component.empty()
                .append(prefix.copy().withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY));
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

    private record SynergyDefinition(
            ResourceKey<Enchantment> first,
            ResourceKey<Enchantment> second,
            String marker,
            int color
    ) {
    }

    private record ActiveSynergy(SynergyDefinition definition, TooltipEntry first, TooltipEntry second) {
    }

    private record SynergyMarker(String marker, int color) {
    }
}
