package com.villagerbargains.trade;

/**
 * Describes a single villager trade.
 *
 * Normal trades:         vanillaMin/vanillaMax = flat emerald cost range.
 * Enchanted book trades: vanillaMin/vanillaMax = 0 (unused).
 *   treasure   — doubles the base price.
 *   maxPerLevel — per-level factor for MAXIMUM mode formula.
 *
 * Vanilla book price formulas (d=0, r=0, h=0 — no reputation/HotV applied):
 *   MINIMUM: min( 2 + 3*level,       64 )  — or doubled if treasure
 *   MAXIMUM: min( 2 + maxPerLevel*level, 64 )  — or doubled if treasure
 *
 * Source: https://minecraft.wiki/w/Trading#Price_formula
 *   min price = 2 + 3*level;  max price = 6 + 13*level  (non-treasure)
 *   treasure doubles the range; cap is 64.
 */
public final class TradeDefinition {

    private final String tradeId;
    private final int    vanillaMin;
    private final int    vanillaMax;
    private final String sellKey;
    private final int    maxPerLevel; // for MAXIMUM book formula
    private final boolean treasure;

    /** For normal (non-book) trades. */
    public TradeDefinition(String tradeId, int vanillaMin, int vanillaMax) {
        this(tradeId, vanillaMin, vanillaMax, null, 0, false);
    }

    /** For enchanted book trades. */
    public TradeDefinition(String tradeId, int vanillaMin, int vanillaMax,
                           String sellKey, int maxPerLevel, boolean treasure) {
        this.tradeId     = tradeId;
        this.vanillaMin  = vanillaMin;
        this.vanillaMax  = vanillaMax;
        this.sellKey     = sellKey;
        this.maxPerLevel = maxPerLevel;
        this.treasure    = treasure;
    }

    public String  tradeId()    { return tradeId; }
    public int     vanillaMin() { return vanillaMin; }
    public int     vanillaMax() { return vanillaMax; }
    public String  sellKey()    { return sellKey; }
    public int     maxPerLevel(){ return maxPerLevel; }
    public boolean isTreasure() { return treasure; }

    /** True if this is an enchanted book trade. */
    public boolean isBookTrade() { return sellKey != null; }

    /**
     * MINIMUM price for this trade.
     * Books: 2 + 3*level, doubled for treasure, capped at 64.
     * Normal: vanillaMin.
     */
    public int resolveMin(int level) {
        if (!isBookTrade()) return vanillaMin;
        int base = 2 + 3 * level;
        return Math.min(treasure ? base * 2 : base, 64);
    }

    /**
     * MAXIMUM price for this trade.
     * Books: 2 + maxPerLevel*level, doubled for treasure, capped at 64.
     * Normal: vanillaMax.
     */
    public int resolveMax(int level) {
        if (!isBookTrade()) return vanillaMax;
        int base = 2 + maxPerLevel * level;
        return Math.min(treasure ? base * 2 : base, 64);
    }

    /** Clamps a requested price to vanilla range (normal trades only). */
    public int clamp(int requested) {
        return Math.max(vanillaMin, Math.min(vanillaMax, requested));
    }
}
