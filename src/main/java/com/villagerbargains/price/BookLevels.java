package com.villagerbargains.price;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;

/**
 * Turns the vanilla level roll of a traded enchanted book into the configured level.
 *
 * <p>The level is only pinned while an {@code enchant_randomly} function runs for a trade (see
 * {@link ThreadScope#TRADE_BOOK}); chest loot and other uses of the function keep their vanilla
 * level. As with prices, vanilla draws its random number first, so the random sequence (and with it the chosen
 * enchantment and every later roll) is exactly the same as in vanilla. The book's price is
 * computed by vanilla from the resulting level, so it always matches the level that is sold.
 */
public final class BookLevels {
    private BookLevels() {}

    /** Resolves a level roll between {@code lowest} and {@code highest} (both inclusive). */
    public static int resolve(int rolled, int lowest, int highest) {
        return resolve(rolled, lowest, highest, VillagerBargainsConfig.bookLevelMode());
    }

    /** {@link #resolve(int, int, int)} with an explicit mode. Unchanged outside a trade book. */
    public static int resolve(int rolled, int lowest, int highest, PricingMode mode) {
        if (!ThreadScope.TRADE_BOOK.isActive()) {
            return rolled; // Not a trade (chest loot, mob gear): keep the vanilla level.
        }
        return mode.pick(rolled, lowest, highest);
    }
}
