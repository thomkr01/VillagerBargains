package com.villagerbargains.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.trade.TradeDefinition;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the in-memory JSON override files that are injected as a data pack.
 * One JSON file per trade: data/minecraft/trades/<tradeId>.json
 *
 * To add support for new trade types: add entries to VanillaTrades.
 * To change override format: edit buildJson() below.
 */
public final class TradeOverrideJson {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TradeOverrideJson() {}

    /** Builds all override JSON files keyed by their data pack path. */
    public static Map<String, byte[]> buildAll() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (Map.Entry<String, TradeDefinition> entry : VanillaTrades.getAll().entrySet()) {
            String tradeId = entry.getKey();
            int price = PriceResolver.resolve(tradeId);
            if (price < 0) continue;
            String path = "data/minecraft/trades/" + tradeId.replace(':', '/') + ".json";
            files.put(path, buildJson(price).getBytes(StandardCharsets.UTF_8));
        }
        return files;
    }

    private static String buildJson(int price) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("price", price);
        return GSON.toJson(root);
    }
}
