package com.villagerbargains.price;

import java.util.function.Supplier;

/**
 * Marks that the current thread is inside some piece of trade generation.
 *
 * <p>Generic vanilla code (random number providers, {@code enchant_randomly}) is also used by
 * loot tables and many other systems. It should only be pinned for trades, and these scopes are
 * how the mixins tell those cases apart.
 */
public final class ThreadScope {
    /** A villager's trade cost ({@code wants} / {@code additional_wants}) is being evaluated. */
    public static final ThreadScope TRADE_COST = new ThreadScope();
    /** An enchanted book for a trade is being made. */
    public static final ThreadScope TRADE_BOOK = new ThreadScope();

    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    ThreadScope() {}

    /** True while this scope is open on this thread. */
    public boolean isActive() {
        return depth.get() > 0;
    }

    /** Runs {@code action} with the scope open. Nested calls are allowed. */
    public <T> T run(Supplier<T> action) {
        depth.set(depth.get() + 1);
        try {
            return action.get();
        } finally {
            depth.set(depth.get() - 1);
        }
    }
}
