package com.villagerbargains;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.util.ModLogger;
import net.fabricmc.api.ModInitializer;

/**
 * Mod entrypoint.
 *
 * Trade price overrides are applied at RUNTIME via a Mixin on the villager
 * trade-offer generation path. Only newly generated trade offers are affected;
 * offers already stored in a villager's NBT data are never touched.
 *
 * Works server-side only in multiplayer (clients do not need this mod).
 * Works in singleplayer because the client runs the integrated server.
 *
 * Config: config/villagerbargains.json (created on first launch).
 * To update trade ranges for a new MC version: edit VanillaTrades.java only.
 */
public final class VillagerBargainsMod implements ModInitializer {

    public static final String MOD_ID = "villagerbargains";

    @Override
    public void onInitialize() {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        ModLogger.get().info(
                "[VillagerBargains] Loaded. Global price mode: {}",
                config.globalPriceMode);
    }
}
