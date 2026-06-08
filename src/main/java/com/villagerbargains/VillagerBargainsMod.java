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
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.util.Map;
import java.util.Optional;

/**
 * Mod entrypoint.
 *
 * What changes between MC versions:
 * - PackLocationInfo / Pack.Metadata constructor signatures (check Mojang mappings)
 * - GodRollResourcePack trade paths (update VanillaTrades.java)
 */
public final class VillagerBargainsMod implements ModInitializer {

    public static final String MOD_ID = "villagerbargains";

    @Override
    public void onInitialize() {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        ModLogger.get().info("[VillagerBargains] Config loaded. Global price mode: {}", config.globalPriceMode);

        Map<String, byte[]> overrides = GodRollResourcePack.buildAllOverrides();
        ModLogger.get().info("[VillagerBargains] Generated {} trade price override(s).", overrides.size());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            PackLocationInfo locationInfo = new PackLocationInfo(
                    MOD_ID + "_overrides",
                    Component.literal("VillagerBargains Trade Overrides"),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );

            InMemoryPack inMemoryPack = new InMemoryPack(locationInfo, overrides);

            Pack pack = Pack.create(
                    MOD_ID + "_overrides",
                    Component.literal("VillagerBargains Trade Overrides"),
                    true,
                    new Pack.ResourcesSupplier() {
                        @Override
                        public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo info) {
                            return inMemoryPack;
                        }
                        @Override
                        public net.minecraft.server.packs.PackResources openFull(
                                PackLocationInfo info, Pack.Metadata metadata) {
                            return inMemoryPack;
                        }
                    },
                    new PackSelectionConfig(true, Pack.Position.TOP, true),
                    PackSource.BUILT_IN
            );

            if (pack != null) {
                server.getPackRepository().addPack(pack);
                ModLogger.get().info("[VillagerBargains] Trade override pack injected.");
            } else {
                ModLogger.get().error("[VillagerBargains] Failed to create trade override pack.");
            }
        });
    }
}
