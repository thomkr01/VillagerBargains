package com.villagerbargains.price;

/**
 * Added to {@code MerchantOffer} by {@code mixin.rules.MerchantOfferMixin} so other hooks can
 * pin the demand of an offer they just created.
 */
public interface DemandPinnable {
    /** Applies {@link DemandRule} to this offer's demand. */
    void villagerbargains$pinDemand();
}
