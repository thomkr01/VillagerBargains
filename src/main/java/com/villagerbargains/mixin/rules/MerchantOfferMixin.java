package com.villagerbargains.mixin.rules;

import com.villagerbargains.price.DemandPinnable;
import com.villagerbargains.price.DemandRule;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps an offer's demand pinned (see {@link DemandRule}) every time a villager restocks.
 *
 * <p>Vanilla, in {@code MerchantOffer#updateDemand} (called on restock):
 * <pre>{@code
 * this.demand = this.demand + this.uses - (this.maxUses - this.uses);
 * }</pre>
 * The pin runs at the end of that method, so the new demand is vanilla's value moved into
 * the configured range. {@code getCostA()} then adds the pinned demand surcharge.
 */
@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin implements DemandPinnable {
    @Shadow
    private int demand;

    @Shadow
    @Final
    private int maxUses;

    @Inject(method = "updateDemand", at = @At("TAIL"))
    private void villagerbargains$pinDemandAfterRestock(CallbackInfo ci) {
        villagerbargains$pinDemand();
    }

    @Override
    public void villagerbargains$pinDemand() {
        this.demand = DemandRule.pin(this.demand, this.maxUses);
    }
}
