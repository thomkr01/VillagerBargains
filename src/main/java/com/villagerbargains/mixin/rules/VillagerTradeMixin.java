package com.villagerbargains.mixin.rules;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.price.DemandPinnable;
import com.villagerbargains.price.ThreadScope;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
 *
 * <p>The whole method also runs inside {@link ThreadScope#VANILLA_TRADER} when the trader in the
 * loot context ({@code this} entity) is a wandering trader and {@code vanillaWanderingTrader} is on,
 * so every price roll and the demand of that offer stay vanilla.
 */
@Mixin(VillagerTrade.class)
public abstract class VillagerTradeMixin {
    @WrapMethod(method = "getOffer")
    private MerchantOffer villagerbargains$markVanillaTrader(LootContext context, Operation<MerchantOffer> original) {
        Entity trader = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (VillagerBargainsConfig.vanillaWanderingTrader() && trader != null
                && trader.getType() == EntityTypes.WANDERING_TRADER) {
            return ThreadScope.VANILLA_TRADER.run(() -> original.call(context));
        }
        return original.call(context);
    }

    @ModifyReturnValue(method = "getOffer", at = @At("RETURN"))
    private MerchantOffer villagerbargains$pinNewOfferDemand(MerchantOffer offer) {
        if (offer != null) {
            ((DemandPinnable) offer).villagerbargains$pinDemand();
        }
        return offer;
    }
}
