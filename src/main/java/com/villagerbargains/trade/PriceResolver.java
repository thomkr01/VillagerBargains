package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves the final price for a trade.
 *
 * When the mod is enabled, trades are forced to the official vanilla
 * MINIMUM value for every trade type:
 *
 *  - For normal trades: uses vanillaMin from TradeDefinition.
 *  - For librarian enchanted books: uses a per-enchantment, per-level
 *    minimum table where provided, falling back to Mojang's documented
 *    minimum formula based on level and treasure status.
 *
 * Keeping this as a dedicated class means price logic stays decoupled from
 * mixins and can be changed without touching villager injection code.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * Manual override table for enchanted book minimum prices.
     *
     * Key:   enchantment registry id string (e.g. "minecraft:depth_strider").
     * Value: int[] of per-level minimum emerald costs, index = level-1.
     *
     * If an entry exists and contains a positive value for the requested level,
     * that value is used instead of the fallback formula.
     *
     * HOW TO ADD AN OVERRIDE:
     *   ENCHANT_MIN_OVERRIDES.put("minecraft:enchant_name", new int[] { lvl1, lvl2, lvl3 });
     * Leave array shorter than max level and the formula will handle remaining levels.
     */
    private static final Map<String, int[]> ENCHANT_MIN_OVERRIDES = new HashMap<>();

    static {
        // Non-treasure, up to level 3 — these match the formula but are explicit for clarity.
        ENCHANT_MIN_OVERRIDES.put("minecraft:depth_strider", new int[] { 5, 8, 11 });
        // Treasure, level 1 only
        ENCHANT_MIN_OVERRIDES.put("minecraft:mending",       new int[] { 10 });
        // Add more entries here as needed, one line per enchantment id.
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves the minimum price for a non-book trade.
     * Returns the vanilla minimum from the TradeDefinition, or -1 if mod is disabled.
     */
    public static int resolve(TradeDefinition def, VillagerBargainsConfig config) {
        return resolve(null, def, config);
    }

    /**
     * Resolves the minimum price for any trade.
     *
     * @param offer   the concrete merchant offer; needed for enchanted books. May be null.
     * @param def     vanilla trade definition with min/max; may be null for enchanted books.
     * @param config  the loaded mod config.
     * @return the desired price, or -1 if the mod is disabled.
     */
    public static int resolve(MerchantOffer offer, TradeDefinition def, VillagerBargainsConfig config) {
        if (!config.enabled) return -1;

        if (offer != null && offer.getResult().is(Items.ENCHANTED_BOOK)) {
            return resolveEnchantedBookPrice(offer);
        }

        if (def == null) return -1;

        // All non-book trades: force vanilla minimum price.
        return def.vanillaMin();
    }

    // ── Enchanted book pricing ────────────────────────────────────────────────

    /**
     * Returns the minimum emerald cost for an enchanted book trade.
     *
     * Resolution order:
     *   1. Manual override in ENCHANT_MIN_OVERRIDES for this enchantment + level.
     *   2. Fallback formula: non-treasure = 3*level+2, treasure = 2*(3*level+2).
     *
     * Result is clamped to [1, 64].
     */
    private static int resolveEnchantedBookPrice(MerchantOffer offer) {
        ItemStack result = offer.getResult();
        ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);

        if (enchantments == null || enchantments.isEmpty()) {
            return 5; // Cheapest valid non-treasure L1 price.
        }

        var entry  = enchantments.entrySet().iterator().next();
        Holder<Enchantment> holder = entry.getKey();
        int level = Math.max(1, entry.getIntValue());

        String enchantId = getEnchantmentId(holder);

        int override = getOverrideMinPrice(enchantId, level);
        int price;
        if (override > 0) {
            // 1) Explicit per-enchant, per-level override wins.
            price = override;
        } else {
            // 2) Fallback formula based on vanilla librarian pricing.
            boolean isTreasure = holder.is(EnchantmentTags.TREASURE);
            price = 3 * level + 2;
            if (isTreasure) price *= 2;
        }

        return Math.max(1, Math.min(64, price));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the namespaced registry id string for an enchantment holder,
     * e.g. "minecraft:mending".
     *
     * Uses BuiltInRegistries.ENCHANTMENT (the official static registry for
     * built-in enchantments, available in both Mojang mappings for 26.1.x
     * and Yarn mappings for 1.19.3+).
     */
    private static String getEnchantmentId(Holder<Enchantment> holder) {
        if (holder == null || holder.value() == null) return "";
        // BuiltInRegistries.ENCHANTMENT.getKey() is the canonical way to get
        // the ResourceLocation (namespaced id) for a built-in enchantment.
        var key = BuiltInRegistries.ENCHANTMENT.getKey(holder.value());
        return key != null ? key.toString() : "";
    }

    private static int getOverrideMinPrice(String enchantId, int level) {
        if (enchantId == null || enchantId.isEmpty()) return -1;
        int[] levels = ENCHANT_MIN_OVERRIDES.get(enchantId);
        if (levels == null) return -1;
        int index = level - 1;
        if (index < 0 || index >= levels.length) return -1;
        return levels[index];
    }
}
