package com.bargainvillage.trade;

public record TradeDefinition(String tradeId, int vanillaMin, int vanillaMax) {
    public int clamp(int requested) {
        return Math.max(vanillaMin, Math.min(vanillaMax, requested));
    }
}
