package com.villagerbargains.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Builds the JSON fragment for a single villager trade override.
 * Produces a constant count provider so the price is always exact.
 *
 * To change the JSON structure for a future MC version:
 *  edit buildWantsFragment() below.
 */
public final class TradeJsonBuilder {
    private TradeJsonBuilder() {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static JsonObject buildWantsFragment(TradeDefinition def, String wantsItemId, int price) {
        JsonObject root   = new JsonObject();
        JsonObject wants  = new JsonObject();
        wants.addProperty("id", wantsItemId);

        // "minecraft:constant" provider — no randomness
        JsonObject countProvider = new JsonObject();
        countProvider.addProperty("type",  "minecraft:constant");
        countProvider.addProperty("value", price);
        wants.add("count", countProvider);

        root.add("wants", wants);
        return root;
    }

    public static String toJson(JsonElement element) {
        return GSON.toJson(element);
    }
}
