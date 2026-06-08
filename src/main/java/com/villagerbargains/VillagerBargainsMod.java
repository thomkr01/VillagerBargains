package com.villagerbargains;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.util.ModLogger;
import net.fabricmc.api.ModInitializer;

/**
 * Entry point. Just loads config and logs startup.
 * All price logic is handled by VillagerTradesMixin.
 */
public final class VillagerBargainsMod implements ModInitializer {
    public static final String MOD_ID = "villagerbargains";

    @Override
    public void onInitialize() {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        ModLogger.get().info("VillagerBargains loaded. Global price mode: {}", config.globalPriceMode);
    }
}
