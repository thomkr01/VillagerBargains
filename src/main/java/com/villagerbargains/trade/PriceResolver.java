package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;

/**
 * Resolves the final clamped price for a trade given a config.
 *
 * Rules:
 *  - MINIMUM : vanilla minimum (godroll) — never goes lower than vanilla min
 *  - MAXIMUM : vanilla maximum
 *  - CUSTOM  : user-supplied value, clamped to [vanillaMin, vanillaMax]
 *
 * PriceResolver is the single source of price-resolution logic.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * @param def     vanilla trade definition (min/max values)
     * @param config  the loaded VillagerBargainsConfig
     * @return final price, always within [def.vanillaMin(), def.vanillaMax()]
     */
    public static int resolve(TradeDefinition def, VillagerBargainsConfig config) {
        String tradeId = def.tradeId();
        VillagerBargainsConfig.PriceMode mode = config.effectivePriceMode(tradeId);
        int customPrice = config.effectiveCustomPrice(tradeId);

        return switch (mode) {
            case MINIMUM -> def.vanillaMin();
            case MAXIMUM -> def.vanillaMax();
            case CUSTOM -> Math.max(def.vanillaMin(), Math.min(def.vanillaMax(), customPrice));
        };
    }
}
