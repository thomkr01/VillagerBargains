package com.villagerbargains.price;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;

import java.util.function.Supplier;

/**
 * Turns the vanilla level roll of a traded enchanted book into the configured level.
 *
 * <p>The level is only pinned while an {@code enchant_randomly} function runs for a trade (see
 * {@link #run}); chest loot and other uses of the function keep their vanilla level. As with
 * prices, vanilla draws its random number first, so the random sequence (and with it the chosen
 * enchantment and every later roll) is exactly the same as in vanilla. The book's price is
 * computed by vanilla from the resulting level, so it always matches the level that is sold.
 */
public final class BookLevels {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private BookLevels() {}

    /** Runs {@code action} while an enchanted book for a trade is being made. Nested calls are allowed. */
    public static <T> T run(Supplier<T> action) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            return action.get();
        } finally {
            DEPTH.set(DEPTH.get() - 1);
        }
    }

    /** Resolves a level roll between {@code lowest} and {@code highest} (both inclusive). */
    public static int resolve(int rolled, int lowest, int highest) {
        return resolve(rolled, lowest, highest, VillagerBargainsConfig.bookLevelMode());
    }

    /** {@link #resolve(int, int, int)} with an explicit mode. Unchanged outside {@link #run}. */
    public static int resolve(int rolled, int lowest, int highest, PricingMode mode) {
        if (DEPTH.get() == 0) {
            return rolled; // Not a trade (chest loot, mob gear): keep the vanilla level.
        }
        return mode.pick(rolled, lowest, highest);
    }
}
