package com.betterenchanting.world.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModifierPlannerTest {
    @Test
    void sameModifiersInDifferentPhysicalSlotsProduceSameSemanticPlan() {
        ModifierPlanner.Plan first = ModifierPlanner.plan(List.of(
                input(2, "betterenchanting:fire_essence"),
                input(3, "betterenchanting:mining_essence"),
                input(4, "minecraft:enchanted_book|minecraft:sharpness:3")
        ), 3, 12345);

        ModifierPlanner.Plan second = ModifierPlanner.plan(List.of(
                input(4, "betterenchanting:fire_essence"),
                input(2, "betterenchanting:mining_essence"),
                input(3, "minecraft:enchanted_book|minecraft:sharpness:3")
        ), 3, 12345);

        assertEquals(semanticDirectPlan(first), semanticDirectPlan(second));
        assertEquals(blockedOfferCount(first), blockedOfferCount(second));
    }

    @Test
    void duplicateIdenticalModifiersAreStable() {
        List<ModifierPlanner.Input> duplicates = List.of(
                input(2, "betterenchanting:fire_essence"),
                input(3, "betterenchanting:fire_essence"),
                input(4, "betterenchanting:mining_essence")
        );

        ModifierPlanner.Plan first = ModifierPlanner.plan(duplicates, 3, 9876);
        ModifierPlanner.Plan second = ModifierPlanner.plan(duplicates, 3, 9876);

        assertEquals(slotDirectPlan(first), slotDirectPlan(second));
        assertEquals(semanticDirectPlan(first), semanticDirectPlan(second));
    }

    @Test
    void purificationStyleModifierIsGlobalAndBlocksExactlyOneOffer() {
        ModifierPlanner.Plan plan = ModifierPlanner.plan(List.of(
                new ModifierPlanner.Input(2, "betterenchanting:purification_essence", true, true),
                input(3, "betterenchanting:fire_essence"),
                input(4, "betterenchanting:mining_essence")
        ), 3, 4321);

        assertEquals(List.of("betterenchanting:purification_essence"), plan.globalModifiers().stream()
                .map(ModifierPlanner.Input::sortKey)
                .toList());
        assertEquals(1, blockedOfferCount(plan));
    }

    private static ModifierPlanner.Input input(int slot, String sortKey) {
        return new ModifierPlanner.Input(slot, sortKey, false, false);
    }

    private static List<String> semanticDirectPlan(ModifierPlanner.Plan plan) {
        return plan.directModifiers().stream()
                .map(optional -> optional.map(ModifierPlanner.Input::sortKey))
                .map(optional -> optional.orElse("<empty>"))
                .toList();
    }

    private static List<Integer> slotDirectPlan(ModifierPlanner.Plan plan) {
        return plan.directModifiers().stream()
                .map(optional -> optional.map(ModifierPlanner.Input::slot))
                .map(optional -> optional.orElse(-1))
                .toList();
    }

    private static int blockedOfferCount(ModifierPlanner.Plan plan) {
        int count = 0;
        for (int option = 0; option < plan.directModifiers().size(); option++) {
            if (plan.blocksOffer(option)) {
                count++;
            }
        }
        return count;
    }
}
