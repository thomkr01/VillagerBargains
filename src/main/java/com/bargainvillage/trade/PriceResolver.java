package com.bargainvillage.trade;

import com.bargainvillage.config.BargainVillageConfig;

public final class PriceResolver {
    private PriceResolver() {}

    public static int resolve(String tradeId) {
        TradeDefinition def = VanillaTrades.get(tradeId);
        if (def == null) {
            return -1;
        }

        BargainVillageConfig cfg = BargainVillageConfig.getInstance();
        BargainVillageConfig.PriceMode mode = cfg.effectivePriceMode(tradeId);

        int raw = switch (mode) {
            case MINIMUM -> def.vanillaMin();
            case MAXIMUM -> def.vanillaMax();
            case CUSTOM -> cfg.effectiveCustomPrice(tradeId);
        };

        return def.clamp(raw);
    }
}
