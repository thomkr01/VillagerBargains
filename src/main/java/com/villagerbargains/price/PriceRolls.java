package com.villagerbargains.price;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;

/**
 * The single place where a vanilla price roll is turned into the configured result.
 *
 * <p>Mixins always let vanilla perform its roll first and pass the result in here.
 * Because the random number is still drawn, the random sequence used for everything
 * that happens afterwards (other trades, enchantments, levels) is exactly the same as
 * in vanilla. Only the price itself changes.
 */
public final class PriceRolls {
    private PriceRolls() {}

    /** Resolves an integer price roll between {@code lowest} and {@code highest} (both inclusive). */
    public static int resolve(int rolled, int lowest, int highest) {
        return mode().pick(rolled, lowest, highest);
    }

    /** Resolves a float price roll in {@code [lowest, highestExclusive)}. */
    public static float resolve(float rolled, float lowest, float highestExclusive) {
        return mode().pick(rolled, lowest, highestExclusive);
    }

    private static PricingMode mode() {
        return VillagerBargainsConfig.pricingMode();
    }
}
