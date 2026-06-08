package com.villagerbargains;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.resource.InMemoryResourcePack;
import com.villagerbargains.resource.TradeOverrideJson;
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

public final class VillagerBargainsMod implements ModInitializer {
    public static final String MOD_ID = "villagerbargains";

    @Override
    public void onInitialize() {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        ModLogger.get().info("VillagerBargains loaded. Global price mode: {}", config.globalPriceMode);

        Map<String, byte[]> files = TradeOverrideJson.buildAll();
        ModLogger.get().info("VillagerBargains: generated {} trade override(s).", files.size());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            PackLocationInfo info = new PackLocationInfo(
                MOD_ID + "_overrides",
                Component.literal("VillagerBargains Trade Overrides"),
                PackSource.BUILT_IN,
                Optional.empty()
            );
            InMemoryResourcePack pack = new InMemoryResourcePack(info, files);
            server.getPackRepository().addPack(
                Pack.readMetaAndCreate(info, new Pack.ResourcesSupplier() {
                    @Override
                    public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo i) { return pack; }
                    @Override
                    public net.minecraft.server.packs.PackResources openFull(PackLocationInfo i, Pack.Metadata m) { return pack; }
                }, net.minecraft.server.packs.PackType.SERVER_DATA,
                   new PackSelectionConfig(true, Pack.Position.TOP, true))
            );
            ModLogger.get().info("VillagerBargains: pack injected.");
        });
    }
}
