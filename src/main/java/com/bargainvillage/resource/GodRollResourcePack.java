package com.bargainvillage.resource;

import com.bargainvillage.trade.PriceResolver;
import com.bargainvillage.trade.TradeDefinition;
import com.bargainvillage.trade.TradeJsonBuilder;
import com.bargainvillage.trade.VanillaTrades;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GodRollResourcePack {
    private GodRollResourcePack() {}

    public static InputStream getTradeOverrideStream(String tradeId) {
        int price = PriceResolver.resolve(tradeId);
        if (price < 0) return null;

        TradeDefinition def = VanillaTrades.get(tradeId);
        if (def == null) return null;

        JsonObject fragment = TradeJsonBuilder.buildWantsFragment(def, "minecraft:emerald", price);
        byte[] bytes = TradeJsonBuilder.toJson(fragment).getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(bytes);
    }

    public static Map<String, byte[]> buildAllOverrides() {
        Map<String, byte[]> overrides = new LinkedHashMap<>();
        for (TradeDefinition def : VanillaTrades.getAll().values()) {
            int price = PriceResolver.resolve(def.tradeId());
            if (price < 0) continue;

            JsonObject fragment = TradeJsonBuilder.buildWantsFragment(def, "minecraft:emerald", price);
            String json = TradeJsonBuilder.toJson(fragment);
            String resourcePath = tradeIdToDataPath(def.tradeId());
            overrides.put(resourcePath, json.getBytes(StandardCharsets.UTF_8));
        }
        return Collections.unmodifiableMap(overrides);
    }

    public static String tradeIdToDataPath(String tradeId) {
        int colon = tradeId.indexOf(':');
        if (colon < 0) {
            return "data/minecraft/villager_trade/" + tradeId + ".json";
        }
        String namespace = tradeId.substring(0, colon);
        String path = tradeId.substring(colon + 1);
        return "data/" + namespace + "/villager_trade/" + path + ".json";
    }
}
