package com.villagerbargains.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingModeTest {
    @Test
    void integerRollsUseTheConfiguredEndOfTheRange() {
        assertEquals(3, PricingMode.MINIMUM.pick(7, 3, 9));
        assertEquals(7, PricingMode.NORMAL.pick(7, 3, 9));
        assertEquals(9, PricingMode.MAXIMUM.pick(7, 3, 9));
    }

    @Test
    void degenerateRangesKeepTheVanillaRoll() {
        for (PricingMode mode : PricingMode.values()) {
            assertEquals(5, mode.pick(5, 5, 5));
            assertEquals(5, mode.pick(5, 5, 2)); // Mth.nextInt returns min when min >= max.
        }
    }

    @Test
    void floatMaximumStaysBelowTheExclusiveBound() {
        float max = PricingMode.MAXIMUM.pick(10f, 0f, 35f);
        assertTrue(max < 35f);
        // 26.2 Trade Rebalance books: floor(11 + uniform[0, 35)) tops out at 45, like vanilla.
        assertEquals(45, (int) Math.floor(11f + max));
        assertEquals(11, (int) Math.floor(11f + PricingMode.MINIMUM.pick(10f, 0f, 35f)));
    }

    /**
     * Vanilla enchanted book price: {@code 2 + nextInt(5 + 10 * level) + 3 * level}.
     * The mixin resolves the nextInt roll over {@code [0, bound - 1]}.
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void enchantedBookPricesMatchTheVanillaRange(int level) {
        int bound = 5 + level * 10;
        int cheapest = 2 + PricingMode.MINIMUM.pick(bound / 2, 0, bound - 1) + 3 * level;
        int dearest = 2 + PricingMode.MAXIMUM.pick(bound / 2, 0, bound - 1) + 3 * level;
        assertEquals(2 + 3 * level, cheapest);
        assertEquals(6 + 13 * level, dearest);
    }

    @Test
    void nextCyclesThroughAllModes() {
        assertEquals(PricingMode.NORMAL, PricingMode.MINIMUM.next());
        assertEquals(PricingMode.MAXIMUM, PricingMode.NORMAL.next());
        assertEquals(PricingMode.MINIMUM, PricingMode.MAXIMUM.next());
    }
}
