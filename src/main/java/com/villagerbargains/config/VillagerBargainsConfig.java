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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads and saves config from config/villagerbargains.json.
 * Two pricing modes: MINIMUM (godroll, default) or MAXIMUM.
 * To override per-trade, add entries to "perTradePrices".
 * To add new config fields: add a field here, update defaults below.
 */
public final class VillagerBargainsConfig {
    // ── Constants ──────────────────────────────────────────────────────────────
    private static final String CONFIG_FILE_NAME = "villagerbargains.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static VillagerBargainsConfig instance;

    // ── Singleton ──────────────────────────────────────────────────────────────
    public static VillagerBargainsConfig getInstance() {
        if (instance == null) instance = loadOrCreate();
        return instance;
    }

    // ── Config Fields ──────────────────────────────────────────────────────────
    @SerializedName("globalPriceMode")
    public PriceMode globalPriceMode = PriceMode.MINIMUM;

    /** Key: tradeId (e.g. "minecraft:librarian/level_1/enchanted_book") */
    @SerializedName("perTradePrices")
    public Map<String, TradeOverride> perTradePrices = new LinkedHashMap<>();

    // ── Enums & Nested Types ───────────────────────────────────────────────────
    public enum PriceMode {
        /** Vanilla minimum price (godroll) — default */
        MINIMUM,
        /** Vanilla maximum price */
        MAXIMUM
    }

    public static final class TradeOverride {
        @SerializedName("priceMode")
        public PriceMode priceMode = PriceMode.MINIMUM;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    public PriceMode effectivePriceMode(String tradeId) {
        TradeOverride override = perTradePrices.get(tradeId);
        return override != null ? override.priceMode : globalPriceMode;
    }

    // ── I/O ────────────────────────────────────────────────────────────────────
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
