package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;

/**
 * Combines a TradeDefinition with the active config to produce
 * the final clamped price for a given trade.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * @return the resolved price, or -1 if tradeId is not in VanillaTrades.
     */
    public static int resolve(String tradeId) {
        TradeDefinition def = VanillaTrades.get(tradeId);
        if (def == null) return -1;

        VillagerBargainsConfig cfg = VillagerBargainsConfig.getInstance();
        VillagerBargainsConfig.PriceMode mode = cfg.effectivePriceMode(tradeId);

        int raw = switch (mode) {
            case MINIMUM -> def.vanillaMin();
            case MAXIMUM -> def.vanillaMax();
            case CUSTOM  -> cfg.effectiveCustomPrice(tradeId);
        };

        return def.clamp(raw);
    }
}
