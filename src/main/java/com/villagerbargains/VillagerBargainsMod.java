package com.villagerbargains;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.util.ModLogger;
import net.fabricmc.api.ModInitializer;

/**
 * Mod entrypoint.
 *
 * Trade price overrides are pre-generated at build time by the Gradle
 * `generateTradeResources` task and bundled as static data files inside
 * the mod jar under data/minecraft/villager_trade/...
 *
 * Fabric loads those files automatically as part of the mod’s built-in
 * data pack — no runtime pack injection or ServerLifecycleEvents needed.
 *
 * The only work done here is:
 *   1. Loading / creating the config file so it exists on first launch.
 *   2. Logging the active price mode for diagnostics.
 *
 * To update trade ranges for a new MC version: edit VanillaTrades.java,
 * then re-run `./gradlew generateTradeResources build`.
 */
public final class VillagerBargainsMod implements ModInitializer {

    public static final String MOD_ID = "villagerbargains";

    @Override
    public void onInitialize() {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        ModLogger.get().info(
                "[VillagerBargains] Loaded. Global price mode: {}",
                config.globalPriceMode);
        ModLogger.get().info(
                "[VillagerBargains] Trade overrides are bundled as static data files. "
                + "Re-run generateTradeResources + build to apply config changes.");
    }
}
