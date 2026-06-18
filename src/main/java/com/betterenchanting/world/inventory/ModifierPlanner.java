package com.betterenchanting.world.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

final class ModifierPlanner {
    private ModifierPlanner() {
    }

    static Plan plan(List<Input> modifiers, int slotCount, int enchantmentSeed) {
        List<Input> normalizedModifiers = modifiers.stream()
                .sorted(Comparator.comparing(Input::sortKey).thenComparingInt(Input::slot))
                .toList();
        List<Optional<Input>> directModifiers = new ArrayList<>(slotCount);
        boolean[] blockedOffers = new boolean[slotCount];
        List<Input> globalModifiers = new ArrayList<>();
        List<Integer> availableOffers = new ArrayList<>();
        for (int option = 0; option < slotCount; option++) {
            directModifiers.add(Optional.empty());
            availableOffers.add(option);
        }

        List<Input> shuffledModifiers = new ArrayList<>(normalizedModifiers);
        Random random = new Random(planSeed(enchantmentSeed, normalizedModifiers));
        shuffle(shuffledModifiers, random);
        for (Input modifier : shuffledModifiers) {
            if (modifier.appliesToAllOffers()) {
                globalModifiers.add(modifier);
                if (!modifier.blocksOffer()) {
                    continue;
                }
            }
            if (availableOffers.isEmpty()) {
                continue;
            }

            int offerIndex = availableOffers.remove(random.nextInt(availableOffers.size()));
            if (modifier.blocksOffer()) {
                blockedOffers[offerIndex] = true;
            } else {
                directModifiers.set(offerIndex, Optional.of(modifier));
            }
        }

        return new Plan(List.copyOf(globalModifiers), List.copyOf(directModifiers), blockedOffers);
    }

    private static long planSeed(int enchantmentSeed, List<Input> modifiers) {
        long seed = enchantmentSeed;
        for (Input modifier : modifiers) {
            seed = seed * 31L + modifier.sortKey().hashCode();
        }
        return seed;
    }

    private static void shuffle(List<Input> modifiers, Random random) {
        for (int index = modifiers.size() - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            if (swapIndex != index) {
                Input current = modifiers.get(index);
                modifiers.set(index, modifiers.get(swapIndex));
                modifiers.set(swapIndex, current);
            }
        }
    }

    record Input(int slot, String sortKey, boolean appliesToAllOffers, boolean blocksOffer) {
    }

    record Plan(List<Input> globalModifiers, List<Optional<Input>> directModifiers, boolean[] blockedOffers) {
        Plan {
            blockedOffers = blockedOffers.clone();
        }

        boolean blocksOffer(int option) {
            return option >= 0 && option < this.blockedOffers.length && this.blockedOffers[option];
        }
    }
}
