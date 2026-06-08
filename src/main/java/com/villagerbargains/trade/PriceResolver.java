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
 * This class is the single source of price-resolution logic.
 * If the pricing rules change, edit only this class.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * @param tradeId  e.g. "minecraft:armorer/level_1/coal_buy"
     * @param config   the loaded VillagerBargainsConfig
     * @return final price, always within [def.vanillaMin(), def.vanillaMax()]
     */
    public static int resolve(String tradeId, VillagerBargainsConfig config) {
        TradeDefinition def = VanillaTrades.get(tradeId);
        if (def == null) return -1; // unknown trade — caller should skip

        VillagerBargainsConfig.PriceMode mode  = config.effectivePriceMode(tradeId);
        int customPrice                         = config.effectiveCustomPrice(tradeId);

        return switch (mode) {
            case MINIMUM -> def.vanillaMin();
            case MAXIMUM -> def.vanillaMax();
            case CUSTOM  -> Math.max(def.vanillaMin(), Math.min(def.vanillaMax(), customPrice));
        };
    }
}
