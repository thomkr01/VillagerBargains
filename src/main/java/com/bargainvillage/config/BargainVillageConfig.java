package com.bargainvillage.config;

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

public final class BargainVillageConfig {
    private static final String CONFIG_FILE_NAME = "bargainvillage.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BargainVillageConfig instance;

    public static BargainVillageConfig getInstance() {
        if (instance == null) {
            instance = loadOrCreate();
        }
        return instance;
    }

    @SerializedName("globalPriceMode")
    public PriceMode globalPriceMode = PriceMode.MINIMUM;

    @SerializedName("globalCustomPrice")
    public int globalCustomPrice = 1;

    @SerializedName("perTradePrices")
    public Map<String, TradeOverride> perTradePrices = new LinkedHashMap<>();

    public enum PriceMode {
        MINIMUM,
        MAXIMUM,
        CUSTOM
    }

    public static final class TradeOverride {
        @SerializedName("priceMode")
        public PriceMode priceMode = PriceMode.MINIMUM;

        @SerializedName("customPrice")
        public int customPrice = 1;
    }

    public PriceMode effectivePriceMode(String tradeId) {
        TradeOverride override = perTradePrices.get(tradeId);
        return override != null ? override.priceMode : globalPriceMode;
    }

    public int effectiveCustomPrice(String tradeId) {
        TradeOverride override = perTradePrices.get(tradeId);
        return override != null ? override.customPrice : globalCustomPrice;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    private static BargainVillageConfig loadOrCreate() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                BargainVillageConfig cfg = GSON.fromJson(reader, BargainVillageConfig.class);
                if (cfg != null) {
                    return cfg;
                }
            } catch (IOException e) {
                System.err.println("[BargainVillage] Failed to read config, using defaults: " + e.getMessage());
            }
        }

        BargainVillageConfig defaults = new BargainVillageConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(configPath())) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("[BargainVillage] Failed to save config: " + e.getMessage());
        }
    }
}
