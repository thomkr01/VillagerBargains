package com.villagerbargains.trade;

import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
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
 * When the mod is enabled, trades are forced to either MINIMUM or MAXIMUM
 * price based on the configured globalPriceMode.
 *
 *  - For normal trades:       uses vanillaMin() or vanillaMax() from TradeDefinition.
 *  - For enchanted books:     uses per-enchantment/per-level override table,
 *                             falling back to Mojang's formula.
 */
public final class PriceResolver {
    private PriceResolver() {}

    /**
     * Manual override table for enchanted book prices.
     *
     * Key:   enchantment registry id string (e.g. "minecraft:depth_strider").
     * Value: int[level][2] where index 0 = min price, index 1 = max price.
     *
     * HOW TO ADD AN OVERRIDE:
     *   ENCHANT_OVERRIDES.put("minecraft:enchant_name", new int[][] { {lvl1min, lvl1max}, {lvl2min, lvl2max} });
     * Leave the array shorter than max level and the formula handles remaining levels.
     */
    private static final Map<String, int[][]> ENCHANT_OVERRIDES = new HashMap<>();

    static {
        // Format: { {level1_min, level1_max}, {level2_min, level2_max}, ... }
        ENCHANT_OVERRIDES.put("minecraft:depth_strider", new int[][] { {5, 15}, {8, 20}, {11, 25} });
        ENCHANT_OVERRIDES.put("minecraft:mending",       new int[][] { {10, 38} });
        // Add more entries here as needed, one line per enchantment id.
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static int resolve(TradeDefinition def, VillagerBargainsConfig config) {
        return resolve(null, def, config);
    }

    public static int resolve(MerchantOffer offer, TradeDefinition def, VillagerBargainsConfig config) {
        if (!config.enabled) return -1;

        if (offer != null && offer.getResult().is(Items.ENCHANTED_BOOK)) {
            return resolveEnchantedBookPrice(offer, config);
        }

        if (def == null) return -1;

        return config.globalPriceMode == VillagerBargainsConfig.PriceMode.MAXIMUM
            ? def.vanillaMax()
            : def.vanillaMin();
    }

    // ── Enchanted book pricing ────────────────────────────────────────────────

    private static int resolveEnchantedBookPrice(MerchantOffer offer, VillagerBargainsConfig config) {
        ItemStack result = offer.getResult();
        ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);

        if (enchantments == null || enchantments.isEmpty()) {
            return 5;
        }

        var entry = enchantments.entrySet().iterator().next();
        Holder<Enchantment> holder = entry.getKey();
        int level = Math.max(1, entry.getIntValue());

        String enchantId = getEnchantmentId(holder);
        boolean isMax = config.globalPriceMode == VillagerBargainsConfig.PriceMode.MAXIMUM;

        int price = getOverridePrice(enchantId, level, isMax);
        if (price <= 0) {
            // Fallback formula
            boolean isTreasure = holder.is(EnchantmentTags.TREASURE);
            int base = 3 * level + 2;
            if (isTreasure) base *= 2;
            price = isMax ? (int)(base * 1.5) : base;
        }

        return Math.max(1, Math.min(64, price));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Gets the namespaced id string for an enchantment, e.g. "minecraft:mending".
     *
     * In MC 26.1, ResourceKey exposes identifier() (not location()).
     * identifier() returns the Identifier (namespace:path) of the registry entry.
     */
    private static String getEnchantmentId(Holder<Enchantment> holder) {
        if (holder == null) return "";
        Optional<ResourceKey<Enchantment>> keyOpt = holder.unwrapKey();
        if (!keyOpt.isPresent()) return "";
        ResourceKey<Enchantment> key = keyOpt.get();
        return key.identifier().toString();
    }

    private static int getOverridePrice(String enchantId, int level, boolean isMax) {
        if (enchantId == null || enchantId.isEmpty()) return -1;
        int[][] levels = ENCHANT_OVERRIDES.get(enchantId);
        if (levels == null) return -1;
        int index = level - 1;
        if (index < 0 || index >= levels.length) return -1;
        return isMax ? levels[index][1] : levels[index][0];
    }
}
