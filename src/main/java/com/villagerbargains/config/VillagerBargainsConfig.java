package com.villagerbargains.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves config from config/villagerbargains.json.
 *
 * Two simple options:
 *  - enabled: true/false toggle.
 *  - priceMode: MINIMUM (cheapest possible) or MAXIMUM (most expensive possible).
 *
 * This keeps the mod tiny and modular. Extend this class and PriceResolver
 * if more modes are needed in the future.
 */
public final class VillagerBargainsConfig {
    private static final String CONFIG_FILE_NAME = "villagerbargains.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static VillagerBargainsConfig instance;

    public static VillagerBargainsConfig getInstance() {
        if (instance == null) instance = loadOrCreate();
        return instance;
    }

    // ── Price Mode ─────────────────────────────────────────────────────────────

    public enum PriceMode {
        /** Force all trades to their cheapest possible vanilla price. */
        MINIMUM,
        /** Force all trades to their most expensive possible vanilla price. */
        MAXIMUM
    }

    // ── Config Fields ────────────────────────────────────────────────────────

    /** When false, the mod is effectively disabled and prices remain vanilla. */
    @SerializedName("enabled")
    public boolean enabled = true;

    /** Whether to force trades to their MINIMUM or MAXIMUM vanilla price. */
    @SerializedName("price_mode")
    public PriceMode priceMode = PriceMode.MINIMUM;

    // ── I/O ─────────────────────────────────────────────────────────────────

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    private static VillagerBargainsConfig loadOrCreate() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                VillagerBargainsConfig cfg = GSON.fromJson(reader, VillagerBargainsConfig.class);
                if (cfg != null) return cfg;
            } catch (IOException e) {
                System.err.println("[VillagerBargains] Failed to read config, using defaults: " + e.getMessage());
            }
        }
        VillagerBargainsConfig defaults = new VillagerBargainsConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(configPath())) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("[VillagerBargains] Failed to save config: " + e.getMessage());
        }
    }
}
