package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Resolves the final price for a trade.
 *
 * When the mod is enabled, trades are forced to either the MINIMUM or MAXIMUM
 * vanilla price depending on config.
 *
 *  - For normal trades: uses vanilla min or max count from TradeDefinition.
 *  - For librarian enchanted books: reproduces Minecraft's documented
 *    min/max price formula based on enchantment level and treasure status.
 *
 * Keeping this as a dedicated class means price logic stays decoupled from
 * mixins and can be changed without touching villager injection code.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * Entry point for callers that don't have a concrete MerchantOffer
     * (e.g. trade list injection before offers are created).
     */
    public static int resolve(TradeDefinition def, VillagerBargainsConfig config) {
        return resolve(null, def, config);
    }

    /**
     * @param offer   the concrete merchant offer (needed for enchanted books); may be null
     * @param def     vanilla trade definition holding min/max values
     * @param config  the loaded VillagerBargainsConfig
     * @return final price, or -1 if the mod is disabled
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
     * Reproduces Minecraft's enchanted book price formula for librarians.
     * Uses the 1.20.5+ DataComponents API.
     *
     * Formula:
     *   MINIMUM base = 2 + 3 * level
     *   MAXIMUM base = 2 + 8 * level
     *   Treasure enchantments are doubled.
     *   Result is clamped to [1, 64].
     */
    private static int resolveEnchantedBookPrice(MerchantOffer offer, TradeDefinition def, VillagerBargainsConfig config) {
        ItemStack result = offer.getResult();

        // 1.20.5+ API: enchantments stored as DataComponent.
        ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return def.vanillaMin(); // safety fallback
        }

        var entry = enchantments.entrySet().iterator().next();
        Enchantment enchantment = entry.getKey().value();
        int level = Math.max(1, entry.getIntValue());

        // isTreasureOnly() is on the Enchantment value directly in 1.20.5+
        boolean isTreasure = enchantment.isTreasureOnly();
        boolean useMax = config.priceMode == VillagerBargainsConfig.PriceMode.MAXIMUM;

        int base = useMax ? (2 + 8 * level) : (2 + 3 * level);
        if (isTreasure) base *= 2;

        int clamped = (def != null) ? def.clamp(base) : base;
        if (clamped < 1) clamped = 1;
        if (clamped > 64) clamped = 64;
        return clamped;
    }
}
