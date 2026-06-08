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
 * When the mod is enabled, trades are forced to the official vanilla
 * MINIMUM value for every trade type:
 *
 *  - For normal trades: uses vanillaMin from TradeDefinition.
 *  - For librarian enchanted books: uses Mojang's documented minimum
 *    emerald cost formula based on level and treasure status.
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
            return resolveEnchantedBookPrice(offer);
        }

        if (def == null) return -1;

        // Non-book trades: always use the vanilla MINIMUM amount.
        return def.vanillaMin();
    }

    // ── Enchanted book pricing ────────────────────────────────────────────────

    /**
     * Official librarian minimum price for enchanted books:
     *
     *  - Non-treasure:  min = 3 * level + 2
     *  - Treasure:      min = 2 * (3 * level + 2)
     *
     * This matches the minimum bounds from Mojang's documented emerald
     * price ranges for librarian offers (e.g. L1: 5, L2: 8, L3: 11...).
     */
    private static int resolveEnchantedBookPrice(MerchantOffer offer) {
        ItemStack result = offer.getResult();

        ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            // No enchantment data — fall back to cheapest possible non‑treasure L1.
            return 5;
        }

        var entry = enchantments.entrySet().iterator().next();
        Holder<Enchantment> holder = entry.getKey();
        int level = Math.max(1, entry.getIntValue());

        boolean isTreasure = holder.is(EnchantmentTags.TREASURE);

        int price = 3 * level + 2; // vanilla minimum for non‑treasure
        if (isTreasure) {
            price *= 2;
            // Known Mojang edge cases (like Frost Walker II often being
            // documented at 16) can be special‑cased here if desired.
        }

        // Clamp to the valid emerald trade range.
        return Math.max(1, Math.min(64, price));
    }
}
