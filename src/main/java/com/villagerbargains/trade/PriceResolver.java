package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;

/**
 * Resolves the final price for a trade.
 *
 * For now this is intentionally trivial: when the mod is enabled, every
 * known trade is forced to its vanilla minimum price (godroll). When the
 * mod is disabled, callers should simply skip applying any override.
 *
 * Keeping this as a dedicated class means we can reintroduce additional
 * modes or per-trade overrides later without touching mixins.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * @param def     vanilla trade definition (min/max values)
     * @param config  the loaded VillagerBargainsConfig
     * @return final price, or -1 if the mod is disabled for this world
     */
    public static int resolve(TradeDefinition def, VillagerBargainsConfig config) {
        if (!config.enabled) return -1;
        return def.vanillaMin();
    }
}
