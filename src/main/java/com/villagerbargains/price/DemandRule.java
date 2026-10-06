package com.villagerbargains.price;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;

/**
 * Pins the demand of a trade offer: the in-game rule that raises a price after the trade
 * has been bought a lot.
 *
 * <p>Vanilla adds {@code max(0, floor(basePrice * demand * priceMultiplier))} to the price.
 * Demand grows with how much a trade was used between restocks and has no upper bound.
 * <ul>
 *   <li>MINIMUM: demand never raises the price ({@code demand <= 0}).</li>
 *   <li>NORMAL: vanilla demand.</li>
 *   <li>MAXIMUM: at least the surcharge of a trade that was completely sold out before one
 *       restock ({@code demand >= maxUses}).</li>
 * </ul>
 * Only the demand value changes. It is synced to clients, so they show the same price even
 * without the mod. Reputation discounts and Hero of the Village stay vanilla.
 */
public final class DemandRule {
    private DemandRule() {}

    /** Pins {@code demand} for the configured pricing mode. */
    public static int pin(int demand, int maxUses) {
        return pin(VillagerBargainsConfig.pricingMode(), demand, maxUses);
    }

    /** Pins {@code demand} for {@code mode}. */
    public static int pin(PricingMode mode, int demand, int maxUses) {
        return switch (mode) {
            case MINIMUM -> Math.min(demand, 0);
            case NORMAL -> demand;
            case MAXIMUM -> Math.max(demand, maxUses);
        };
    }
}
