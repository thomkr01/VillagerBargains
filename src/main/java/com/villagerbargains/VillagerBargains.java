package com.villagerbargains;

import com.villagerbargains.config.VillagerBargainsConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint.
 *
 * <p>All behaviour lives in small mixins (see {@code com.villagerbargains.mixin}) that ask
 * {@link com.villagerbargains.price.PriceRolls} how to treat each vanilla price roll.
 * This class only loads the config so problems with it show up at startup.
 */
public final class VillagerBargains implements ModInitializer {
    public static final String MOD_ID = "villagerbargains";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        VillagerBargainsConfig.load();
        LOGGER.info("Villager Bargains loaded, pricing mode: {}", VillagerBargainsConfig.pricingMode());
    }
}
