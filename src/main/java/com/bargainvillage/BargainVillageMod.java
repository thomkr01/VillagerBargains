package com.bargainvillage;

import com.bargainvillage.config.BargainVillageConfig;
import com.bargainvillage.resource.GodRollResourcePack;
import com.bargainvillage.resource.InMemoryPack;
import com.bargainvillage.util.ModLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.PackSource;

import java.util.Map;
import java.util.Optional;

public final class BargainVillageMod implements ModInitializer {
    public static final String MOD_ID = "bargainvillage";

    @Override
    public void onInitialize() {
        BargainVillageConfig config = BargainVillageConfig.getInstance();
        ModLogger.get().info("[BargainVillage] Config loaded. Global price mode: {}", config.globalPriceMode);

        Map<String, byte[]> overrides = GodRollResourcePack.buildAllOverrides();
        ModLogger.get().info("[BargainVillage] Generated {} trade price override(s).", overrides.size());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            PackLocationInfo locationInfo = new PackLocationInfo(
                    MOD_ID + "_overrides",
                    net.minecraft.network.chat.Component.literal("BargainVillage Trade Overrides"),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );

            InMemoryPack pack = new InMemoryPack(locationInfo, overrides);

            server.getPackRepository().addPack(
                new net.minecraft.server.packs.repository.Pack(
                    locationInfo,
                    new net.minecraft.server.packs.repository.Pack.ResourcesSupplier() {
                        @Override
                        public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo info) {
                            return pack;
                        }
                        @Override
                        public net.minecraft.server.packs.PackResources openFull(
                                PackLocationInfo info,
                                net.minecraft.server.packs.repository.Pack.Metadata metadata) {
                            return pack;
                        }
                    },
                    new net.minecraft.server.packs.repository.Pack.Metadata(
                        net.minecraft.network.chat.Component.literal("BargainVillage trade price overrides"),
                        net.minecraft.server.packs.PackCompatibility.COMPATIBLE,
                        net.minecraft.server.packs.FeatureFlagSet.of(),
                        java.util.List.of()
                    ),
                    new PackSelectionConfig(true, net.minecraft.server.packs.repository.Pack.Position.TOP, true)
                )
            );

            ModLogger.get().info("[BargainVillage] Trade override pack injected into server data repository.");
        });
    }
}
