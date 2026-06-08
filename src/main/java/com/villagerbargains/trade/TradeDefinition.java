package com.villagerbargains.trade;

/**
 * Immutable record describing a single villager trade.
 *
 * sellKey  — optional secondary lookup key used when the buy item alone
 *            is ambiguous (e.g. all enchanted book trades buy with emeralds).
 *            Format: "<item_id>:<enchantment_id>"  e.g. "enchanted_book:minecraft:piercing"
 *            Null for all normal (non-enchanted-book) trades.
 *
 * vanillaMin / vanillaMax come from VanillaTrades — the only file
 * that needs updating when Minecraft changes trade ranges.
 */
public record TradeDefinition(String tradeId, int vanillaMin, int vanillaMax, String sellKey) {

    /** Constructor without sellKey — for all normal trades. */
    public TradeDefinition(String tradeId, int vanillaMin, int vanillaMax) {
        this(tradeId, vanillaMin, vanillaMax, null);
    }

    /** True if this trade is identified by its sell item rather than buy item. */
    public boolean hasSellKey() { return sellKey != null; }

    /** Clamps a requested price to the valid vanilla range. */
    public int clamp(int requested) {
        return Math.max(vanillaMin, Math.min(vanillaMax, requested));
    }
}
