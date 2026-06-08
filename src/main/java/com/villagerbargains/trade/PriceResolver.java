package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;

/**
 * Combines a TradeDefinition with the active config to produce
 * the final price for a given trade.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * @return the resolved price (vanillaMin or vanillaMax), or -1 if tradeId is not in VanillaTrades.
     */
    public static int resolve(String tradeId) {
        TradeDefinition def = VanillaTrades.get(tradeId);
        if (def == null) return -1;

        VillagerBargainsConfig cfg = VillagerBargainsConfig.getInstance();
        VillagerBargainsConfig.PriceMode mode = cfg.effectivePriceMode(tradeId);

        return switch (mode) {
            case MINIMUM -> def.vanillaMin();
            case MAXIMUM -> def.vanillaMax();
        };
    }
}
