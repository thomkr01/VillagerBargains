package com.villagerbargains;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.resource.GodRollResourcePack;
import com.villagerbargains.resource.InMemoryPack;
import com.villagerbargains.util.ModLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.util.Map;
import java.util.Optional;

/**
 * Mod entrypoint.
 *
 * What changes between MC versions:
 *  - PackLocationInfo / Pack.Metadata constructor signatures (check Mojang mappings)
 *  - GodRollResourcePack trade paths (update VanillaTrades.java)
 *  - Pack construction API (Pack constructor vs Pack.readMetaAndCreate)
 */
public final class VillagerBargainsMod implements ModInitializer {

    public static final String MOD_ID = "villagerbargains";

    @Override
    public void onInitialize() {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        ModLogger.get().info("[VillagerBargains] Config loaded. Global price mode: {}", config.globalPriceMode);

        // Build all override JSONs once at startup; they live in RAM for the lifetime of the server.
        Map<String, byte[]> overrides = GodRollResourcePack.buildAllOverrides();
        ModLogger.get().info("[VillagerBargains] Generated {} trade price override(s).", overrides.size());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            PackLocationInfo locationInfo = new PackLocationInfo(
                    MOD_ID + "_overrides",
                    Component.literal("VillagerBargains Trade Overrides"),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );

            // ResourcesSupplier — returns the same InMemoryPack for both openPrimary and openFull.
            Pack.ResourcesSupplier supplier = info -> new InMemoryPack(info, overrides);

            // Pack.Metadata is a record: description, supportedFormats, requestedFeatures, overlays.
            // supportedFormats uses InclusiveRange<Integer>; passing max-int means "all pack formats".
            Pack.Metadata metadata = Pack.readMetaAndCreate(
                    locationInfo,
                    supplier,
                    PackType.SERVER_DATA
            );

            if (metadata == null) {
                ModLogger.get().error("[VillagerBargains] Pack metadata could not be created — aborting injection.");
                return;
            }

            Pack pack = new Pack(
                    locationInfo,
                    supplier,
                    metadata,
                    new PackSelectionConfig(true, Pack.Position.TOP, true)
            );

            server.getPackRepository().addPack(pack);
            ModLogger.get().info("[VillagerBargains] Trade override pack injected.");
        });
    }
}
