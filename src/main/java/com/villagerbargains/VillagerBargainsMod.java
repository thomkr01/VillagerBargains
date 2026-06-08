package com.villagerbargains;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.resource.GodRollResourcePack;
import com.villagerbargains.resource.InMemoryPack;
import com.villagerbargains.util.ModLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VillagerBargainsMod implements ModInitializer {
    public static final String MOD_ID = "villagerbargains";

    @Override
    public void onInitialize() {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        ModLogger.get().info("VillagerBargains loaded. Global price mode: {}", config.globalPriceMode);

        Map<String, byte[]> overrides = GodRollResourcePack.buildAllOverrides();
        ModLogger.get().info("VillagerBargains: generated {} trade price override(s).", overrides.size());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            PackLocationInfo locationInfo = new PackLocationInfo(
                    MOD_ID + "_overrides",
                    Component.literal("VillagerBargains Trade Overrides"),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );

            InMemoryPack pack = new InMemoryPack(locationInfo, overrides);

            server.getPackRepository().addPack(
                new Pack(
                    locationInfo,
                    new Pack.ResourcesSupplier() {
                        @Override
                        public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo info) {
                            return pack;
                        }
                        @Override
                        public net.minecraft.server.packs.PackResources openFull(
                                PackLocationInfo info, Pack.Metadata metadata) {
                            return pack;
                        }
                    },
                    new Pack.Metadata(
                        Component.literal("VillagerBargains trade price overrides"),
                        net.minecraft.server.packs.PackCompatibility.COMPATIBLE,
                        net.minecraft.server.packs.FeatureFlagSet.of(),
                        List.of()
                    ),
                    new PackSelectionConfig(true, Pack.Position.TOP, true)
                )
            );

            ModLogger.get().info("VillagerBargains: trade override pack injected into server data repository.");
        });
    }
}
