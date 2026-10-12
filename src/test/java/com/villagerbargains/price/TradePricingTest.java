package com.villagerbargains.price;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradePricingTest {
    @Test
    void followsTheConfiguredPricingModeByDefault() {
        assertEquals(VillagerBargainsConfig.pricingMode(), TradePricing.priceMode());
        assertEquals(VillagerBargainsConfig.pricingMode(), TradePricing.demandMode());
    }

    @Test
    void aVanillaWanderingTraderGetsVanillaPricesAndDemand() {
        ThreadScope.VANILLA_TRADER.run(() -> {
            assertEquals(PricingMode.NORMAL, TradePricing.priceMode());
            assertEquals(PricingMode.NORMAL, TradePricing.demandMode());
            return null;
        });
        assertEquals(VillagerBargainsConfig.pricingMode(), TradePricing.priceMode());
    }
}
