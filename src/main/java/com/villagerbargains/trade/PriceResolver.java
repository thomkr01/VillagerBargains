package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;

/**
 * Combines a TradeDefinition with the active config to produce
 * the final price for a given trade.
 *
 * Lookup order:
 *   1. REGISTRY      (all normal trades)
 *   2. BOOK_REGISTRY (enchanted book trades, key = "enchanted_book:minecraft:<id>")
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * @return the resolved price (vanillaMin or vanillaMax), or -1 if tradeId is unknown.
     */
    public static int resolve(String tradeId) {
        TradeDefinition def = VanillaTrades.get(tradeId);
        if (def == null) def = VanillaTrades.getByBook(tradeId);
        if (def == null) return -1;

        VillagerBargainsConfig cfg = VillagerBargainsConfig.getInstance();
        VillagerBargainsConfig.PriceMode mode = cfg.effectivePriceMode(tradeId);

        return switch (mode) {
            case MINIMUM -> def.vanillaMin();
            case MAXIMUM -> def.vanillaMax();
        };
    }
}
