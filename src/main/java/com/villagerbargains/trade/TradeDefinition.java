package com.villagerbargains.trade;

/**
 * Immutable record describing a single villager trade.
 * vanillaMin / vanillaMax come from VanillaTrades — the only file
 * that needs updating when Minecraft changes trade ranges.
 */
public record TradeDefinition(String tradeId, int vanillaMin, int vanillaMax) {
    /** Clamps a requested price to the valid vanilla range. */
    public int clamp(int requested) {
        return Math.max(vanillaMin, Math.min(vanillaMax, requested));
    }
}
