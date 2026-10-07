package com.villagerbargains.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillagerBargainsConfigTest {
    private static final String KEY = "bookLevels";

    @Test
    void missingKeyUsesTheFallback() {
        JsonObject json = new JsonObject();
        json.addProperty("pricing", "MAXIMUM");
        assertEquals(PricingMode.NORMAL, VillagerBargainsConfig.parseMode(json, KEY, PricingMode.NORMAL));
    }

    @Test
    void nullJsonUsesTheFallback() {
        // An empty file makes Gson return null.
        assertEquals(PricingMode.MINIMUM, VillagerBargainsConfig.parseMode(null, KEY, PricingMode.MINIMUM));
    }

    @Test
    void nonStringValuesUseTheFallback() {
        JsonObject json = new JsonObject();
        json.addProperty(KEY, 2);
        assertEquals(PricingMode.NORMAL, VillagerBargainsConfig.parseMode(json, KEY, PricingMode.NORMAL));

        json.addProperty(KEY, true);
        assertEquals(PricingMode.NORMAL, VillagerBargainsConfig.parseMode(json, KEY, PricingMode.NORMAL));

        json.add(KEY, new JsonArray());
        assertEquals(PricingMode.NORMAL, VillagerBargainsConfig.parseMode(json, KEY, PricingMode.NORMAL));
    }

    @Test
    void unknownValuesUseTheFallback() {
        JsonObject json = new JsonObject();
        json.addProperty(KEY, "HIGHEST");
        assertEquals(PricingMode.NORMAL, VillagerBargainsConfig.parseMode(json, KEY, PricingMode.NORMAL));

        json.addProperty(KEY, "");
        assertEquals(PricingMode.NORMAL, VillagerBargainsConfig.parseMode(json, KEY, PricingMode.NORMAL));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAXIMUM", "maximum", "Maximum", "  maximum ", "\tMAXIMUM\n"})
    void valuesAreCaseInsensitiveAndTrimmed(String value) {
        JsonObject json = new JsonObject();
        json.addProperty(KEY, value);
        assertEquals(PricingMode.MAXIMUM, VillagerBargainsConfig.parseMode(json, KEY, PricingMode.NORMAL));
    }

    @Test
    void eachKeyIsReadOnItsOwn() {
        JsonObject json = new JsonObject();
        json.addProperty("pricing", "MAXIMUM");
        json.addProperty(KEY, "MINIMUM");
        assertEquals(PricingMode.MAXIMUM, VillagerBargainsConfig.parseMode(json, "pricing", PricingMode.MINIMUM));
        assertEquals(PricingMode.MINIMUM, VillagerBargainsConfig.parseMode(json, KEY, PricingMode.NORMAL));
    }
}
