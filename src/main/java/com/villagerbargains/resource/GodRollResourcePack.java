package com.villagerbargains.resource;

import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.TradeJsonBuilder;
import com.villagerbargains.trade.VanillaTrades;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates in-memory JSON override bytes for every registered trade.
 * The result is fed into InMemoryPack and injected as a server data pack.
 */
public final class GodRollResourcePack {
    private GodRollResourcePack() {}

    public static Map<String, byte[]> buildAllOverrides() {
        Map<String, byte[]> overrides = new LinkedHashMap<>();
        for (TradeDefinition def : VanillaTrades.getAll().values()) {
            int price = PriceResolver.resolve(def.tradeId());
            if (price < 0) continue;

            JsonObject fragment = TradeJsonBuilder.buildWantsFragment(def, "minecraft:emerald", price);
            String json = TradeJsonBuilder.toJson(fragment);
            overrides.put(tradeIdToDataPath(def.tradeId()), json.getBytes(StandardCharsets.UTF_8));
        }
        return Collections.unmodifiableMap(overrides);
    }

    /** Converts "minecraft:librarian/level_1/enchanted_book" → "data/minecraft/villager_trade/librarian/level_1/enchanted_book.json" */
    public static String tradeIdToDataPath(String tradeId) {
        int colon = tradeId.indexOf(':');
        if (colon < 0) return "data/minecraft/villager_trade/" + tradeId + ".json";
        String namespace = tradeId.substring(0, colon);
        String path      = tradeId.substring(colon + 1);
        return "data/" + namespace + "/villager_trade/" + path + ".json";
    }
}
