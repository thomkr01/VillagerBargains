package com.villagerbargains.price;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadScopeTest {
    private final ThreadScope scope = new ThreadScope();

    @Test
    void activeOnlyInsideRun() {
        assertFalse(scope.isActive());
        assertTrue(scope.run(scope::isActive));
        assertFalse(scope.isActive());
    }

    @Test
    void nestedScopesStayActiveUntilTheOutermostEnds() {
        boolean innerAfterNested = scope.run(() -> {
            scope.run(() -> null);
            return scope.isActive();
        });
        assertTrue(innerAfterNested);
        assertFalse(scope.isActive());
    }

    @Test
    void scopeClosesWhenTheActionThrows() {
        assertThrows(IllegalStateException.class, () -> scope.run(() -> {
            throw new IllegalStateException("boom");
        }));
        assertFalse(scope.isActive());
    }

    @Test
    void scopesAreIndependent() {
        assertFalse(ThreadScope.TRADE_COST.run(ThreadScope.TRADE_BOOK::isActive));
    }
}
