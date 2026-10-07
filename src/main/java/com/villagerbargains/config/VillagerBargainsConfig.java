package com.villagerbargains.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.villagerbargains.VillagerBargains;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes {@code config/villagerbargains.json}.
 *
 * <pre>{@code
 * {
 *   "pricing": "MINIMUM",     // MINIMUM, NORMAL or MAXIMUM
 *   "bookLevels": "NORMAL"    // MINIMUM, NORMAL or MAXIMUM
 * }
 * }</pre>
 *
 * <p>Each mode is held in a volatile field so the Mod Menu screen (render thread) and the
 * server thread that generates trades always see the same value.
 */
public final class VillagerBargainsConfig {
    private static final String FILE_NAME = VillagerBargains.MOD_ID + ".json";
    private static final String KEY_PRICING = "pricing";
    /** Key used by 1.x releases; read once so existing configs keep their choice. */
    private static final String LEGACY_KEY_PRICING = "globalPriceMode";
    private static final String KEY_BOOK_LEVELS = "bookLevels";
    private static final PricingMode DEFAULT_MODE = PricingMode.MINIMUM;
    /** Vanilla by default: changing which book levels villagers sell is opt-in. */
    private static final PricingMode DEFAULT_BOOK_LEVEL_MODE = PricingMode.NORMAL;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile PricingMode pricingMode = DEFAULT_MODE;
    private static volatile PricingMode bookLevelMode = DEFAULT_BOOK_LEVEL_MODE;

    private VillagerBargainsConfig() {}

    /** The pricing mode used for every newly generated trade. */
    public static PricingMode pricingMode() {
        return pricingMode;
    }

    /** Changes the pricing mode and writes it to disk. */
    public static void setPricingMode(PricingMode mode) {
        pricingMode = mode;
        save();
    }

    /** Which level traded enchanted books get: the lowest, the vanilla roll or the highest. */
    public static PricingMode bookLevelMode() {
        return bookLevelMode;
    }

    /** Changes the book level mode and writes it to disk. */
    public static void setBookLevelMode(PricingMode mode) {
        bookLevelMode = mode;
        save();
    }

    /** Loads the config file, creating it with defaults when missing or unreadable. */
    public static void load() {
        Path path = path();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                String pricingKey = json != null && json.has(KEY_PRICING) ? KEY_PRICING : LEGACY_KEY_PRICING;
                pricingMode = parseMode(json, pricingKey, DEFAULT_MODE);
                bookLevelMode = parseMode(json, KEY_BOOK_LEVELS, DEFAULT_BOOK_LEVEL_MODE);
            } catch (IOException | JsonParseException | IllegalStateException e) {
                VillagerBargains.LOGGER.warn("Could not read {}, using defaults: {}", path, e.getMessage());
                pricingMode = DEFAULT_MODE;
                bookLevelMode = DEFAULT_BOOK_LEVEL_MODE;
            }
        }
        save(); // Normalises the file (adds missing keys, drops legacy ones).
    }

    /** Reads the mode stored under {@code key}, or {@code fallback} when it is missing or invalid. */
    static PricingMode parseMode(JsonObject json, String key, PricingMode fallback) {
        if (json == null || !json.has(key)) {
            return fallback;
        }
        JsonElement element = json.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            VillagerBargains.LOGGER.warn("'{}' must be a string like \"MINIMUM\", got {}; using {}", key, element, fallback);
            return fallback;
        }
        String value = element.getAsString();
        try {
            return PricingMode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            VillagerBargains.LOGGER.warn("Unknown mode '{}' for '{}', using {}", value, key, fallback);
            return fallback;
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty(KEY_PRICING, pricingMode.name());
        json.addProperty(KEY_BOOK_LEVELS, bookLevelMode.name());
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            VillagerBargains.LOGGER.warn("Could not write {}: {}", path, e.getMessage());
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
