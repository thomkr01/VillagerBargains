package com.villagerbargains.price;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;

import java.util.function.Supplier;

/**
 * The pricing modes in force for the trade being made right now. They equal the configured
 * {@code pricing} mode except where an opt-in setting asks for vanilla: wandering trader offers
 * ({@code vanillaWanderingTrader}) and the demand surcharge ({@code vanillaDemand}).
 */
public final class TradePricing {
    private TradePricing() {}

    /**
     * Runs {@code action} for {@code trader}'s offers: inside {@link ThreadScope#VANILLA_TRADER}
     * when it is a wandering trader and {@code vanillaWanderingTrader} is on.
     */
    public static <T> T forTrader(Entity trader, Supplier<T> action) {
        if (VillagerBargainsConfig.vanillaWanderingTrader() && trader != null
                && trader.getType() == EntityTypes.WANDERING_TRADER) {
            return ThreadScope.VANILLA_TRADER.run(action);
        }
        return action.get();
    }

    /** Mode for random price rolls: NORMAL while a wandering trader set to vanilla makes its offers. */
    public static PricingMode priceMode() {
        return ThreadScope.VANILLA_TRADER.isActive() ? PricingMode.NORMAL : VillagerBargainsConfig.pricingMode();
    }

    /** Mode for the demand surcharge: NORMAL when {@code vanillaDemand} is on, else the price mode. */
    public static PricingMode demandMode() {
        return VillagerBargainsConfig.vanillaDemand() ? PricingMode.NORMAL : priceMode();
    }
}
