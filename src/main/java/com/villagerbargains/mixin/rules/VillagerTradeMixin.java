package com.villagerbargains.mixin.rules;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.villagerbargains.price.DemandPinnable;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Pins the demand of every newly generated trade offer (see
 * {@link com.villagerbargains.price.DemandRule}), so MAXIMUM already charges the sold-out
 * surcharge before the first restock.
 *
 * <p>Vanilla, at the end of {@code VillagerTrade#getOffer(LootContext)}:
 * <pre>{@code
 * return new MerchantOffer(...);   // or null when the trade cannot be made
 * }</pre>
 * A new offer starts with demand {@code 0}; this hook pins the returned offer when there is one.
 */
@Mixin(VillagerTrade.class)
public abstract class VillagerTradeMixin {
    @ModifyReturnValue(method = "getOffer", at = @At("RETURN"))
    private MerchantOffer villagerbargains$pinNewOfferDemand(MerchantOffer offer) {
        if (offer != null) {
            ((DemandPinnable) offer).villagerbargains$pinDemand();
        }
        return offer;
    }
}
