package com.villagerbargains.trade;

/**
 * Immutable record describing a single villager trade.
 *
 * Normal trades:       vanillaMin / vanillaMax are the flat emerald cost range.
 * Enchanted book trades: vanillaMin / vanillaMax are ignored for price calculation.
 *                        Use minPerLevel / maxPerLevel instead.
 *                        Formula: cost = min(2 + level * factor, 64)
 *                        Treasure enchantments cost double (handled in VanillaTrades
 *                        by setting higher factors).
 *
 * sellKey — "enchanted_book:minecraft:<id>" for book trades, null otherwise.
 */
public record TradeDefinition(
        String tradeId,
        int vanillaMin,
        int vanillaMax,
        String sellKey,
        int minPerLevel,
        int maxPerLevel
) {
    /** Constructor for normal (non-book) trades. */
    public TradeDefinition(String tradeId, int vanillaMin, int vanillaMax) {
        this(tradeId, vanillaMin, vanillaMax, null, 0, 0);
    }

    /** Constructor for enchanted book trades. */
    public TradeDefinition(String tradeId, int vanillaMin, int vanillaMax, String sellKey,
                           int minPerLevel, int maxPerLevel) {
        this(tradeId, vanillaMin, vanillaMax, sellKey, minPerLevel, maxPerLevel, (Void) null);
    }

    /** Canonical constructor — kept private via sentinel to avoid ambiguity. */
    private TradeDefinition(String tradeId, int vanillaMin, int vanillaMax, String sellKey,
                            int minPerLevel, int maxPerLevel, Void ignored) {
        this.tradeId     = tradeId;
        this.vanillaMin  = vanillaMin;
        this.vanillaMax  = vanillaMax;
        this.sellKey     = sellKey;
        this.minPerLevel = minPerLevel;
        this.maxPerLevel = maxPerLevel;
    }

    /** True if this is an enchanted book trade (has per-level scaling). */
    public boolean isBookTrade() { return sellKey != null; }

    /**
     * Resolves the actual emerald cost for a given enchantment level.
     * Formula: min(2 + level * factor, 64)
     * For normal trades, level is ignored and vanillaMin/vanillaMax are used.
     */
    public int resolveMin(int level) {
        if (!isBookTrade()) return vanillaMin;
        return Math.min(2 + level * minPerLevel, 64);
    }

    public int resolveMax(int level) {
        if (!isBookTrade()) return vanillaMax;
        return Math.min(2 + level * maxPerLevel, 64);
    }

    /** Clamps a requested price to the valid vanilla range (for normal trades). */
    public int clamp(int requested) {
        return Math.max(vanillaMin, Math.min(vanillaMax, requested));
    }
}
