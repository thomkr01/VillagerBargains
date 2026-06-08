package com.bargainvillage.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class TradeJsonBuilder {
    private TradeJsonBuilder() {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static JsonObject buildWantsFragment(TradeDefinition def, String wantsItemId, int price) {
        JsonObject root = new JsonObject();
        JsonObject wants = new JsonObject();
        wants.addProperty("id", wantsItemId);

        JsonObject countProvider = new JsonObject();
        countProvider.addProperty("type", "minecraft:constant");
        countProvider.addProperty("value", price);
        wants.add("count", countProvider);

        root.add("wants", wants);
        return root;
    }

    public static String toJson(JsonElement element) {
        return GSON.toJson(element);
    }
}
