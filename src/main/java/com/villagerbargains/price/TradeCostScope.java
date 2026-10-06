package com.villagerbargains.price;

import java.util.function.Supplier;

/**
 * Marks that the current thread is calculating what a villager wants for a trade.
 *
 * <p>Generic random number providers (such as {@code minecraft:uniform}) are used by loot
 * tables and many other systems. They should only be pinned while a trade cost is being
 * evaluated, and this scope is how the mixins tell those cases apart.
 */
public final class TradeCostScope {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private TradeCostScope() {}

    /** True while a trade cost is being evaluated on this thread. */
    public static boolean isActive() {
        return DEPTH.get() > 0;
    }

    /** Runs {@code action} with the scope active. Nested calls are allowed. */
    public static <T> T run(Supplier<T> action) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            return action.get();
        } finally {
            DEPTH.set(DEPTH.get() - 1);
        }
    }
}
