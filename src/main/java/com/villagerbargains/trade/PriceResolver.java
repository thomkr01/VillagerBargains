package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Map;

/**
 * Resolves the final price for a trade.
 *
 * When the mod is enabled, we always force trades to the configured price mode
 * (MINIMUM or MAXIMUM) using vanilla price formulas.
 *
 *  - For normal trades, that is simply the vanilla min/max count.
 *  - For librarian enchanted books, Minecraft uses an internal formula based
 *    on enchantment level and whether the enchantment is a treasure
 *    enchantment. We reproduce the documented min/max price here.
 *
 * Keeping this as a dedicated class means we can reintroduce additional
 * modes or per-trade overrides later without touching mixins.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * Backwards-compatible entry point that only knows about the static
     * vanilla min/max range. Used by callers that don't need enchanted-book
     * awareness.
     */
    public static int resolve(TradeDefinition def, VillagerBargainsConfig config) {
        return resolve(null, def, config);
    }

    /**
     * @param offer   the concrete merchant offer (needed for enchanted books)
     * @param def     vanilla trade definition (min/max values)
     * @param config  the loaded VillagerBargainsConfig
     * @return final price, or -1 if the mod is disabled for this world
     */
    public static int resolve(MerchantOffer offer, TradeDefinition def, VillagerBargainsConfig config) {
        if (!config.enabled) return -1;

        if (offer != null && offer.getResult().is(Items.ENCHANTED_BOOK)) {
            return resolveEnchantedBookPrice(offer, def, config);
        }

        // Non-book trades: use the configured price mode.
        return config.priceMode == VillagerBargainsConfig.PriceMode.MAXIMUM
                ? def.vanillaMax()
                : def.vanillaMin();
    }

    // ── Enchanted book pricing ────────────────────────────────────────────────

    /**
     * Recreates the minimum/maximum emerald cost formula for librarian
     * enchanted books using the 1.20.5+ DataComponents API.
     *
     * Base cost per book is:
     *   MINIMUM: 2 + 3 * level
     *   MAXIMUM: 2 + 8 * level
     * Treasure enchantments (e.g. Mending, Frost Walker) are charged at
     * double the base cost. Result is clamped to vanilla librarian bounds.
     */
    private static int resolveEnchantedBookPrice(MerchantOffer offer, TradeDefinition def, VillagerBargainsConfig config) {
        ItemStack result = offer.getResult();

        // 1.20.5+ API: enchantments are stored as a DataComponent.
        ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            // Safety fallback: if something goes wrong, fall back to static min.
            return def.vanillaMin();
        }

        // Pick the first enchantment entry.
        var entry = enchantments.entrySet().iterator().next();
        var enchantmentHolder = entry.getKey();
        int level = Math.max(1, entry.getIntValue());

        boolean isTreasure = enchantmentHolder.value().isTreasureOnly();
        boolean useMax = config.priceMode == VillagerBargainsConfig.PriceMode.MAXIMUM;

        int base = useMax ? (2 + 8 * level) : (2 + 3 * level);
        if (isTreasure) base *= 2;

        int clamped = (def != null) ? def.clamp(base) : base;

        // Absolute safety bounds for librarian emerald costs.
        if (clamped < 1) clamped = 1;
        if (clamped > 64) clamped = 64;
        return clamped;
    }
}
