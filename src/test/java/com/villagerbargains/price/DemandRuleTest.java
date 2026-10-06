package com.villagerbargains.price;

import com.villagerbargains.config.PricingMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemandRuleTest {
    private static final int MAX_USES = 12;

    @ParameterizedTest
    @ValueSource(ints = {-24, -12, -1, 0, 1, 11, 12, 13, 40})
    void normalKeepsVanillaDemand(int demand) {
        assertEquals(demand, DemandRule.pin(PricingMode.NORMAL, demand, MAX_USES));
    }

    @Test
    void minimumNeverRaisesThePrice() {
        assertEquals(-12, DemandRule.pin(PricingMode.MINIMUM, -12, MAX_USES));
        assertEquals(0, DemandRule.pin(PricingMode.MINIMUM, 0, MAX_USES));
        assertEquals(0, DemandRule.pin(PricingMode.MINIMUM, 5, MAX_USES));
        assertEquals(0, DemandRule.pin(PricingMode.MINIMUM, 40, MAX_USES));
    }

    @Test
    void maximumIsAtLeastASoldOutRestock() {
        assertEquals(MAX_USES, DemandRule.pin(PricingMode.MAXIMUM, -12, MAX_USES));
        assertEquals(MAX_USES, DemandRule.pin(PricingMode.MAXIMUM, 0, MAX_USES));
        assertEquals(MAX_USES, DemandRule.pin(PricingMode.MAXIMUM, 12, MAX_USES));
        assertEquals(40, DemandRule.pin(PricingMode.MAXIMUM, 40, MAX_USES)); // Vanilla demand above it is kept.
    }

    /**
     * Vanilla {@code getCostA}: {@code base + max(0, floor(base * demand * multiplier))}, clamped to 1..64.
     * Base 10 emeralds, multiplier 0.05 and a sold-out restock ({@code demand = maxUses = 12}) give 16.
     */
    @Test
    void pinnedDemandGivesTheExpectedPrices() {
        // After one restock with every trade used: vanilla demand 12.
        assertEquals(10, cost(DemandRule.pin(PricingMode.MINIMUM, 12, MAX_USES)));
        assertEquals(16, cost(DemandRule.pin(PricingMode.NORMAL, 12, MAX_USES)));
        assertEquals(16, cost(DemandRule.pin(PricingMode.MAXIMUM, 12, MAX_USES)));
        // After one restock without any use: vanilla demand -12.
        assertEquals(10, cost(DemandRule.pin(PricingMode.MINIMUM, -12, MAX_USES)));
        assertEquals(10, cost(DemandRule.pin(PricingMode.NORMAL, -12, MAX_USES)));
        assertEquals(16, cost(DemandRule.pin(PricingMode.MAXIMUM, -12, MAX_USES)));
    }

    private static int cost(int demand) {
        int base = 10;
        int demandDiff = Math.max(0, (int) Math.floor(base * demand * 0.05f));
        return Math.clamp(base + demandDiff, 1, 64);
    }
}
