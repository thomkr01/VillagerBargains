package com.villagerbargains.price;

import com.villagerbargains.config.PricingMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookLevelsTest {
    @ParameterizedTest
    @EnumSource(PricingMode.class)
    void outsideRunTheVanillaLevelIsKept(PricingMode mode) {
        assertEquals(3, BookLevels.resolve(3, 1, 5, mode));
    }

    @Test
    void insideRunTheLevelIsPinned() {
        assertEquals(1, (int) BookLevels.run(() -> BookLevels.resolve(3, 1, 5, PricingMode.MINIMUM)));
        assertEquals(3, (int) BookLevels.run(() -> BookLevels.resolve(3, 1, 5, PricingMode.NORMAL)));
        assertEquals(5, (int) BookLevels.run(() -> BookLevels.resolve(3, 1, 5, PricingMode.MAXIMUM)));
    }

    @Test
    void singleLevelEnchantmentsKeepTheirLevel() {
        // Mending, Silk Touch, ...: lowest == highest == 1.
        for (PricingMode mode : PricingMode.values()) {
            assertEquals(1, (int) BookLevels.run(() -> BookLevels.resolve(1, 1, 1, mode)));
        }
    }

    @Test
    void nestedRunsStayPinnedUntilTheOutermostEnds() {
        int afterNested = BookLevels.run(() -> {
            int inner = BookLevels.run(() -> BookLevels.resolve(2, 1, 4, PricingMode.MAXIMUM));
            assertEquals(4, inner);
            return BookLevels.resolve(2, 1, 4, PricingMode.MAXIMUM);
        });
        assertEquals(4, afterNested);
        assertEquals(2, BookLevels.resolve(2, 1, 4, PricingMode.MAXIMUM));
    }

    @Test
    void scopeClosesWhenTheActionThrows() {
        assertThrows(IllegalStateException.class, () -> BookLevels.run(() -> {
            throw new IllegalStateException("boom");
        }));
        assertEquals(2, BookLevels.resolve(2, 1, 4, PricingMode.MINIMUM));
    }
}
