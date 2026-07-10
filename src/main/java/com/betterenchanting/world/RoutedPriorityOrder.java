package com.betterenchanting.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

final class RoutedPriorityOrder {
    private RoutedPriorityOrder() {
    }

    static <T, K> List<T> apply(List<K> priority, List<T> available, Function<T, K> key) {
        if (priority == null || priority.isEmpty() || available.isEmpty()) {
            return List.copyOf(available);
        }

        List<T> ordered = new ArrayList<>(available.size());
        Set<T> added = new HashSet<>();
        for (K preferred : priority) {
            for (T candidate : available) {
                if (Objects.equals(preferred, key.apply(candidate)) && added.add(candidate)) {
                    ordered.add(candidate);
                    break;
                }
            }
        }
        for (T candidate : available) {
            if (added.add(candidate)) {
                ordered.add(candidate);
            }
        }
        return List.copyOf(ordered);
    }
}
