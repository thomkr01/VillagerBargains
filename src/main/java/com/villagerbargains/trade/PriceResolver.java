package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
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
 *    The def parameter may be null for enchanted books — price is derived
 *    entirely from the enchantment, not from VanillaTrades.
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
     * @param def     vanilla trade definition holding min/max values; may be null for enchanted books
     * @param config  the loaded VillagerBargainsConfig
     * @return final price, or -1 if the mod is disabled
     */
    public static int resolve(MerchantOffer offer, TradeDefinition def, VillagerBargainsConfig config) {
        if (!config.enabled) return -1;

        if (offer != null && offer.getResult().is(Items.ENCHANTED_BOOK)) {
            return resolveEnchantedBookPrice(offer, config);
        }

        if (def == null) return -1;

        // Non-book trades: use the configured price mode.
        return config.priceMode == VillagerBargainsConfig.PriceMode.MAXIMUM
                ? def.vanillaMax()
                : def.vanillaMin();
    }

    // ── Enchanted book pricing ────────────────────────────────────────────────

    /**
     * Reproduces Minecraft's enchanted book price formula for librarians.
     *
     * In Minecraft 1.21+, enchantments are fully data-driven.
     * "Treasure" is determined by the EnchantmentTags.TREASURE tag, not
     * by a method on the Enchantment class.
     *
     * Formula (no TradeDefinition needed — price is enchantment-specific):
     *   MINIMUM base = 2 + 3 * level
     *   MAXIMUM base = 2 + 8 * level
     *   Treasure enchantments are doubled.
     *   Result is clamped to [1, 64].
     */
    private static int resolveEnchantedBookPrice(MerchantOffer offer, VillagerBargainsConfig config) {
        ItemStack result = offer.getResult();

        ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            // No enchantment data — fall back to vanilla minimum base price.
            return 5;
        }

        var entry = enchantments.entrySet().iterator().next();
        Holder<Enchantment> holder = entry.getKey();
        int level = Math.max(1, entry.getIntValue());

        // 1.21+: treasure check via tag instead of isTreasureOnly()
        boolean isTreasure = holder.is(EnchantmentTags.TREASURE);
        boolean useMax = config.priceMode == VillagerBargainsConfig.PriceMode.MAXIMUM;

        int base = useMax ? (2 + 8 * level) : (2 + 3 * level);
        if (isTreasure) base *= 2;

        // Clamp to valid emerald trade range.
        return Math.max(1, Math.min(64, base));
    }
}
