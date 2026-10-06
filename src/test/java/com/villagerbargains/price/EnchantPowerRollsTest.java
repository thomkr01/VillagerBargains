package com.villagerbargains.price;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnchantPowerRollsTest {
    private static final EnchantPowerRolls.Resolver MINIMUM = (rolled, lowest, highest) -> lowest;
    private static final EnchantPowerRolls.Resolver NORMAL = (rolled, lowest, highest) -> rolled;
    private static final EnchantPowerRolls.Resolver MAXIMUM = (rolled, lowest, highest) -> highest;

    @Test
    void gearCostIsPinnedToTheRangeOfTheLevelsRoll() {
        // levels = uniform(5, 19) rolled 12: vanilla adds 12 to the price.
        int[] costs = EnchantPowerRolls.run(() -> {
            EnchantPowerRolls.record(12, 5, 19);
            return new int[] {
                    EnchantPowerRolls.pinnedCost(12, MINIMUM),
                    EnchantPowerRolls.pinnedCost(12, NORMAL),
                    EnchantPowerRolls.pinnedCost(12, MAXIMUM)};
        });
        assertEquals(5, costs[0]);
        assertEquals(12, costs[1]);
        assertEquals(19, costs[2]);
    }

    @Test
    void everyRecordedRollIsPinned() {
        // e.g. levels = add(3, uniform(0, 10), uniform(1, 4)) rolled 3 + 7 + 2.
        int cost = EnchantPowerRolls.run(() -> {
            EnchantPowerRolls.record(7, 0, 10);
            EnchantPowerRolls.record(2, 1, 4);
            return EnchantPowerRolls.pinnedCost(12, MAXIMUM);
        });
        assertEquals(3 + 10 + 4, cost);
    }

    @Test
    void pinnedCostIsNeverNegative() {
        int cost = EnchantPowerRolls.run(() -> {
            EnchantPowerRolls.record(3, -5, 10);
            return EnchantPowerRolls.pinnedCost(3, MINIMUM);
        });
        assertEquals(0, cost);
    }

    @Test
    void nothingIsRecordedOrPinnedOutsideARecording() {
        EnchantPowerRolls.record(12, 5, 19);
        assertEquals(12, EnchantPowerRolls.pinnedCost(12, MAXIMUM));
        int insideAfterStrayRecord = EnchantPowerRolls.run(() -> EnchantPowerRolls.pinnedCost(12, MAXIMUM));
        assertEquals(12, insideAfterStrayRecord);
    }

    @Test
    void nestedRecordingsKeepTheirOwnRolls() {
        int[] costs = EnchantPowerRolls.run(() -> {
            EnchantPowerRolls.record(10, 5, 19);
            int inner = EnchantPowerRolls.run(() -> {
                EnchantPowerRolls.record(2, 1, 3);
                return EnchantPowerRolls.pinnedCost(2, MAXIMUM);
            });
            return new int[] {inner, EnchantPowerRolls.pinnedCost(10, MAXIMUM)};
        });
        assertEquals(3, costs[0]);
        assertEquals(19, costs[1]);
    }

    @Test
    void recordingClosesWhenTheActionThrows() {
        assertThrows(IllegalStateException.class, () -> EnchantPowerRolls.run(() -> {
            EnchantPowerRolls.record(12, 5, 19);
            throw new IllegalStateException("boom");
        }));
        assertEquals(12, EnchantPowerRolls.pinnedCost(12, MAXIMUM));
    }
}
