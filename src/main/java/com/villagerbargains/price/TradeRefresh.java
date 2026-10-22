package com.villagerbargains.price;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Applies the current settings to the offers a villager or wandering trader already has,
 * without changing the trades. Only the demand is stored in a form the settings decide; the
 * price rolls, book level and gear strength are baked into the offer when it is made.
 */
public final class TradeRefresh {
    private TradeRefresh() {}

    /** Pins the demand of every offer of {@code trader}, as the next restock would. */
    public static void refresh(AbstractVillager trader) {
        TradePricing.forTrader(trader, () -> {
            for (MerchantOffer offer : trader.getOffers()) {
                ((DemandPinnable) offer).villagerbargains$pinDemand();
            }
            return null;
        });
    }
}
