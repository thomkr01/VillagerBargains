package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Map;

/**
 * Resolves the final price for a trade.
 *
 * When the mod is enabled, we always force trades to the *cheapest* vanilla
 * price the game would ever offer to the player.
 *
 *  - For normal trades, that is simply the vanilla minimum count from
 *    VanillaTrades (e.g. 1 emerald for a book, or 20 wheat for 1 emerald).
 *  - For librarian enchanted books, Minecraft uses an internal formula based
 *    on enchantment level and whether the enchantment is a treasure
 *    enchantment. We reproduce the documented minimum price here so that
 *    players always see the best possible roll.[cite:555][cite:557]
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
            return resolveEnchantedBookPrice(offer, def);
        }

        // Non-book trades: always use the official vanilla minimum count.
        return def.vanillaMin();
    }

    // ── Enchanted book pricing ────────────────────────────────────────────────

    /**
     * Recreates the documented minimum emerald cost formula for librarian
     * enchanted books.[cite:553][cite:555][cite:557]
     *
     * Base minimum cost per book is:
     *   base = 2 + 3 * level
     * Treasure enchantments (e.g. Mending, Frost Walker) are charged at
     * double that base cost. The result is then clamped to the vanilla
     * librarian emerald bounds (5–64 in current versions).
     */
    private static int resolveEnchantedBookPrice(MerchantOffer offer, TradeDefinition def) {
        ItemStack result = offer.getResult();
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(result);
        if (enchantments.isEmpty()) {
            // Safety fallback: if something goes wrong, fall back to static min.
            return def.vanillaMin();
        }

        Map.Entry<Enchantment, Integer> first = enchantments.entrySet().iterator().next();
        Enchantment enchantment = first.getKey();
        int level = Math.max(1, first.getValue());

        int baseMin = 2 + 3 * level; // documented minimum price by level.[cite:547]
        if (enchantment.isTreasureOnly()) {
            // Treasure enchantments are always charged at double price.[cite:557]
            baseMin *= 2;
        }

        int clamped = baseMin;
        if (def != null) {
            clamped = def.clamp(clamped);
        }

        // Absolute safety bounds for librarian emerald costs.
        if (clamped < 1) clamped = 1;
        if (clamped > 64) clamped = 64;
        return clamped;
    }
}
