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
 *  - For librarian enchanted books: uses a per‑enchantment, per‑level
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
     * Key:   enchantment id string (e.g. "minecraft:depth_strider").
     * Value: int[] of per‑level minimum emerald costs, index = level‑1.
     *
     * If an entry exists and contains a positive value for the requested
     * level, that value is used instead of the formula. This keeps the
     * implementation strictly data‑driven and easy to adjust for new
     * Minecraft versions or community‑discovered edge cases.
     */
    private static final Map<String, int[]> ENCHANT_MIN_OVERRIDES = new HashMap<>();

    static {
        // Examples — these currently match the vanilla formula but are here
        // to illustrate how to override specific enchants/levels if Mojang
        // introduces exceptions.
        //
        // Non‑treasure, up to level 3
        ENCHANT_MIN_OVERRIDES.put("minecraft:depth_strider", new int[] { 5, 8, 11 });
        // Treasure, level 1 only
        ENCHANT_MIN_OVERRIDES.put("minecraft:mending",        new int[] { 10 });
        // Add more entries here as needed, one line per enchantment id.
    }

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
     * Official librarian minimum price for enchanted books, with the ability
     * to override specific enchantments and levels via ENCHANT_MIN_OVERRIDES.
     *
     * Fallback formula when no override exists:
     *  - Non-treasure:  min = 3 * level + 2
     *  - Treasure:      min = 2 * (3 * level + 2)
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

        String enchantId = getEnchantmentId(holder);

        // 1) Try explicit per‑enchant, per‑level override.
        int override = getOverrideMinPrice(enchantId, level);
        int price;
        if (override > 0) {
            price = override;
        } else {
            // 2) Fallback to formula based on level + treasure status.
            boolean isTreasure = holder.is(EnchantmentTags.TREASURE);
            price = 3 * level + 2; // vanilla minimum for non‑treasure
            if (isTreasure) {
                price *= 2;
            }
        }

        // Clamp to the valid emerald trade range.
        return Math.max(1, Math.min(64, price));
    }

    private static String getEnchantmentId(Holder<Enchantment> holder) {
        if (holder == null) return "";
        Enchantment enchantment = holder.value();
        if (enchantment == null) return "";

        Object key = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
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
