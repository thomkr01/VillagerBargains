package com.villagerbargains.price;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeCostScopeTest {
    @Test
    void activeOnlyInsideRun() {
        assertFalse(TradeCostScope.isActive());
        assertTrue(TradeCostScope.run(TradeCostScope::isActive));
        assertFalse(TradeCostScope.isActive());
    }

    @Test
    void nestedScopesStayActiveUntilTheOutermostEnds() {
        boolean innerAfterNested = TradeCostScope.run(() -> {
            TradeCostScope.run(() -> null);
            return TradeCostScope.isActive();
        });
        assertTrue(innerAfterNested);
        assertFalse(TradeCostScope.isActive());
    }

    @Test
    void scopeClosesWhenTheActionThrows() {
        assertThrows(IllegalStateException.class, () -> TradeCostScope.run(() -> {
            throw new IllegalStateException("boom");
        }));
        assertFalse(TradeCostScope.isActive());
    }
}
