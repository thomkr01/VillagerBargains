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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
     * HOW TO ADD AN OVERRIDE:
     *   ENCHANT_MIN_OVERRIDES.put("minecraft:enchant_name", new int[] { lvl1, lvl2, lvl3 });
     * Leave the array shorter than max level and the formula handles remaining levels.
     */
    private static final Map<String, int[]> ENCHANT_MIN_OVERRIDES = new HashMap<>();

    static {
        // Non-treasure, up to level 3 - explicit for clarity.
        ENCHANT_MIN_OVERRIDES.put("minecraft:depth_strider", new int[] { 5, 8, 11 });
        // Treasure, level 1 only
        ENCHANT_MIN_OVERRIDES.put("minecraft:mending",       new int[] { 10 });
        // Add more entries here as needed, one line per enchantment id.
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static int resolve(TradeDefinition def, VillagerBargainsConfig config) {
        return resolve(null, def, config);
    }

    public static int resolve(MerchantOffer offer, TradeDefinition def, VillagerBargainsConfig config) {
        if (!config.enabled) return -1;

        if (offer != null && offer.getResult().is(Items.ENCHANTED_BOOK)) {
            return resolveEnchantedBookPrice(offer);
        }

        if (def == null) return -1;

        return def.vanillaMin();
    }

    // ── Enchanted book pricing ────────────────────────────────────────────────

    private static int resolveEnchantedBookPrice(MerchantOffer offer) {
        ItemStack result = offer.getResult();
        ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);

        if (enchantments == null || enchantments.isEmpty()) {
            return 5;
        }

        var entry  = enchantments.entrySet().iterator().next();
        Holder<Enchantment> holder = entry.getKey();
        int level = Math.max(1, entry.getIntValue());

        String enchantId = getEnchantmentId(holder);

        int override = getOverrideMinPrice(enchantId, level);
        int price;
        if (override > 0) {
            price = override;
        } else {
            // Fallback formula: non-treasure = 3*level+2, treasure = 2*(3*level+2)
            boolean isTreasure = holder.is(EnchantmentTags.TREASURE);
            price = 3 * level + 2;
            if (isTreasure) price *= 2;
        }

        return Math.max(1, Math.min(64, price));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Gets the namespaced id string for an enchantment (e.g. "minecraft:mending").
     *
     * In 26.1.x enchantments are a dynamic/datapack registry, so they have no
     * entry in BuiltInRegistries. Instead we read the key directly from the
     * Holder, which always carries it for any registered enchantment.
     *
     * holder.getKey() returns Optional<ResourceKey<Enchantment>>.
     * ResourceKey.location() returns the ResourceLocation (namespaced id).
     */
    private static String getEnchantmentId(Holder<Enchantment> holder) {
        if (holder == null) return "";
        Optional<String> id = holder.getKey()
                .map(key -> key.location().toString());
        return id.orElse("");
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
