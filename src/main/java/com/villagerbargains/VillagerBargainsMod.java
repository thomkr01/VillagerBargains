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
 * Mod entrypoint. Wires config -> trade overrides -> in-memory data pack.
 *
 * NOTE for MC version updates:
 *   - Pack.readMetaAndCreate signature may change; verify against Mojang mappings.
 *   - PackSelectionConfig(alwaysEnabled, position, fixedPosition)
 */
public final class VillagerBargainsMod implements ModInitializer {

    public static final String MOD_ID = "villagerbargains";

    @Override
    public void onInitialize() {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        ModLogger.get().info("[VillagerBargains] Config loaded. Global price mode: {}",
                config.globalPriceMode);

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

            // ResourcesSupplier has two abstract methods in MC 26.x - cannot use a lambda.
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

            PackSelectionConfig selectionConfig =
                    new PackSelectionConfig(true, Pack.Position.TOP, false);

            // In MC 26.x Pack.readMetaAndCreate returns Pack directly (not Optional).
            Pack pack = Pack.readMetaAndCreate(
                    locationInfo, supplier, PackType.SERVER_DATA, selectionConfig);

            if (pack == null) {
                ModLogger.get().error(
                        "[VillagerBargains] Failed to create override pack " +
                        "(readMetaAndCreate returned null). Trade prices will be vanilla.");
                return;
            }

            // addPack signature in MC 26.x: addPack(String id, Supplier<Pack>)
            server.getPackRepository().addPack(MOD_ID + "_overrides", () -> pack);
            ModLogger.get().info("[VillagerBargains] Trade override pack injected.");
        });
    }
}
