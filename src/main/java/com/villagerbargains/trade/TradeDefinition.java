package com.villagerbargains.trade;

/**
 * Describes a single villager trade.
 *
 * Normal trades:        vanillaMin/vanillaMax = flat emerald cost range.
 *                       minPerLevel/maxPerLevel = 0 (unused).
 * Enchanted book trades: vanillaMin/vanillaMax = 0 (unused).
 *                        minPerLevel/maxPerLevel = per-level cost factors.
 *                        Formula: cost = min(2 + level * factor, 64)
 *
 * sellKey — "enchanted_book:minecraft:<id>" for book trades, null otherwise.
 */
public final class TradeDefinition {

    private final String tradeId;
    private final int vanillaMin;
    private final int vanillaMax;
    private final String sellKey;
    private final int minPerLevel;
    private final int maxPerLevel;

    /** For normal (non-book) trades. */
    public TradeDefinition(String tradeId, int vanillaMin, int vanillaMax) {
        this(tradeId, vanillaMin, vanillaMax, null, 0, 0);
    }

    /** For enchanted book trades. */
    public TradeDefinition(String tradeId, int vanillaMin, int vanillaMax,
                           String sellKey, int minPerLevel, int maxPerLevel) {
        this.tradeId      = tradeId;
        this.vanillaMin   = vanillaMin;
        this.vanillaMax   = vanillaMax;
        this.sellKey      = sellKey;
        this.minPerLevel  = minPerLevel;
        this.maxPerLevel  = maxPerLevel;
    }

    public String tradeId()    { return tradeId; }
    public int vanillaMin()    { return vanillaMin; }
    public int vanillaMax()    { return vanillaMax; }
    public String sellKey()    { return sellKey; }
    public int minPerLevel()   { return minPerLevel; }
    public int maxPerLevel()   { return maxPerLevel; }

    /** True if this is an enchanted book trade. */
    public boolean isBookTrade() { return sellKey != null; }

    /** Cost for a given enchantment level. Normal trades ignore level. */
    public int resolveMin(int level) {
        return isBookTrade() ? Math.min(2 + level * minPerLevel, 64) : vanillaMin;
    }

    public int resolveMax(int level) {
        return isBookTrade() ? Math.min(2 + level * maxPerLevel, 64) : vanillaMax;
    }

    /** Clamps a requested price to vanilla range (normal trades only). */
    public int clamp(int requested) {
        return Math.max(vanillaMin, Math.min(vanillaMax, requested));
    }
}
