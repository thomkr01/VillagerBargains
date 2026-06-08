package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;

/**
 * Resolves the final emerald cost for a trade.
 *
 * Normal trades:       returns vanillaMin or vanillaMax.
 * Enchanted book trades: returns min(2 + level * factor, 64) using the actual
 *                        enchantment level passed by the mixin.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /** Resolves price for normal (non-book) trades. Returns -1 if unknown. */
    public static int resolve(String tradeId) {
        TradeDefinition def = VanillaTrades.get(tradeId);
        if (def == null) return -1;

        VillagerBargainsConfig.PriceMode mode = VillagerBargainsConfig.getInstance().effectivePriceMode(tradeId);
        return switch (mode) {
            case MINIMUM -> def.vanillaMin();
            case MAXIMUM -> def.vanillaMax();
        };
    }

    /** Resolves price for enchanted book trades with the actual enchantment level. Returns -1 if unknown. */
    public static int resolveBook(String sellKey, int enchantmentLevel) {
        TradeDefinition def = VanillaTrades.getByBook(sellKey);
        if (def == null) return -1;

        VillagerBargainsConfig.PriceMode mode = VillagerBargainsConfig.getInstance().effectivePriceMode(sellKey);
        return switch (mode) {
            case MINIMUM -> def.resolveMin(enchantmentLevel);
            case MAXIMUM -> def.resolveMax(enchantmentLevel);
        };
    }
}
