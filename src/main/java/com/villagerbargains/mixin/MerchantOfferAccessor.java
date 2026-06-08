package com.villagerbargains.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes MerchantOffer's private 'demand' field so we can reset it.
 * MC's displayed price formula:
 *   displayedCost = max(1, baseCostA + specialPriceDiff + floor(baseCostA * priceMultiplier * demand))
 * By zeroing demand and specialPriceDiff, displayedCost == baseCostA (our desired price).
 */
@Mixin(MerchantOffer.class)
public interface MerchantOfferAccessor {
    @Accessor("demand")
    int getDemand();

    @Accessor("demand")
    void setDemand(int demand);
}
