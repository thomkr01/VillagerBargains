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
 * Mod entrypoint. Wires config → trade overrides → in-memory data pack.
 *
 * NOTE for MC version updates:
 *   - Pack.readMetaAndCreate signature lives here; update if MC changes it.
 *   - PackSelectionConfig(enabled, position, fixed) — adjust as needed.
 */
public final class VillagerBargainsMod implements ModInitializer {

    public static final String MOD_ID = "villagerbargains";

    @Override
    public void onInitialize() {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        ModLogger.get().info("[VillagerBargains] Config loaded. Global price mode: {}",
                config.globalPriceMode);

        // Build all override JSONs once at startup — cheap, pure in-memory.
        Map<String, byte[]> overrides = GodRollResourcePack.buildAllOverrides();
        ModLogger.get().info("[VillagerBargains] Generated {} trade price override(s).",
                overrides.size());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {

            PackLocationInfo locationInfo = new PackLocationInfo(
                    MOD_ID + "_overrides",
                    Component.literal("VillagerBargains Trade Overrides"),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );

            // ResourcesSupplier has two abstract methods in MC 26.x — cannot use a lambda.
            Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
                @Override
                public net.minecraft.server.packs.PackResources openPrimary(
                        PackLocationInfo info) {
                    return new InMemoryPack(info, overrides);
                }

                @Override
                public net.minecraft.server.packs.PackResources openFull(
                        PackLocationInfo info, Pack.Metadata metadata) {
                    return new InMemoryPack(info, overrides);
                }
            };

            // PackSelectionConfig(alwaysEnabled, position, fixedPosition)
            PackSelectionConfig selectionConfig =
                    new PackSelectionConfig(true, Pack.Position.TOP, false);

            // readMetaAndCreate reads pack.mcmeta from the supplier and returns
            // Optional.empty() only if the pack metadata is unreadable.
            Optional<Pack> pack = Pack.readMetaAndCreate(
                    locationInfo, supplier, PackType.SERVER_DATA, selectionConfig);

            pack.ifPresentOrElse(
                    p -> {
                        server.getPackRepository().addPack(p);
                        ModLogger.get().info(
                                "[VillagerBargains] Trade override pack injected.");
                    },
                    () -> ModLogger.get().error(
                            "[VillagerBargains] Failed to create override pack — " +
                            "readMetaAndCreate returned empty. Check pack metadata.")
            );
        });
    }
}
