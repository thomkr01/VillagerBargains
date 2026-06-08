package com.villagerbargains.mixin;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.util.ModLogger;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Intercepts every new TradeOffer added to a villager's TradeOfferList.
 *
 * HOW IT WORKS
 * ─────────────────────────────────────────────────────────────────────────────
 * When a villager gains a new trade (profession assignment or level-up),
 * Minecraft calls TradeOfferList#add(TradeOffer) for each generated offer.
 * We inject AFTER that add, inspect the last entry, look up the trade ID
 * in VanillaTrades, resolve the configured price via PriceResolver, and
 * mutate the first ingredient's count if it differs from the vanilla-rolled value.
 *
 * Existing offers stored in NBT are never loaded through this path, so they
 * remain completely untouched.
 *
 * SERVER-SIDE ONLY
 * ─────────────────────────────────────────────────────────────────────────────
 * Trade generation happens on the server (or the integrated single-player server).
 * Clients only receive the final TradeOfferList over the network — they never
 * call add() during generation, so this Mixin has zero effect on pure clients.
 *
 * MATCHING TRADES TO IDS
 * ─────────────────────────────────────────────────────────────────────────────
 * We identify a trade by matching its first ingredient item ID against every
 * entry in VanillaTrades. This is fuzzy but robust: if Mojang renames an item
 * or adds new trades, unrecognised offers simply pass through unmodified.
 *
 * TO UPDATE FOR A NEW MC VERSION: edit VanillaTrades.java only.
 */
@Mixin(TradeOfferList.class)
public class TradeOfferListMixin {

    /**
     * Inject after each add() call so we see the offer that was just appended.
     */
    @Inject(method = "add(Lnet/minecraft/village/TradeOffer;)V", at = @At("RETURN"))
    private void onAdd(TradeOffer offer, CallbackInfo ci) {
        TradeOfferList self = (TradeOfferList) (Object) this;
        if (self.isEmpty()) return;

        // The offer we just added is the last element.
        TradeOffer added = self.get(self.size() - 1);
        applyPriceConfig(added);
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private static void applyPriceConfig(TradeOffer offer) {
        ItemStack firstBuy = offer.getOriginalFirstBuyItem();
        if (firstBuy.isEmpty()) return;

        String itemId = firstBuy.getItem().toString();
        TradeDefinition def = findDefinitionByItem(itemId);
        if (def == null) return;

        VillagerBargainsConfig config  = VillagerBargainsConfig.getInstance();
        int desiredPrice = PriceResolver.resolve(def.tradeId(), config);
        int currentCount = firstBuy.getCount();

        if (currentCount != desiredPrice) {
            firstBuy.setCount(desiredPrice);
            ModLogger.get().debug(
                    "[VillagerBargains] {} : {} → {}",
                    def.tradeId(), currentCount, desiredPrice);
        }
    }

    /**
     * Finds the best-matching TradeDefinition for the given item ID string.
     * The item ID is the last path segment of the trade ID
     * (e.g. "minecraft:emerald" → matches trades whose id ends with "/emerald").
     * Falls back to null if nothing matches.
     */
    private static TradeDefinition findDefinitionByItem(String itemId) {
        // Strip namespace: "minecraft:emerald" → "emerald"
        int colon = itemId.lastIndexOf(':');
        String itemName = colon >= 0 ? itemId.substring(colon + 1) : itemId;

        for (Map.Entry<String, TradeDefinition> entry : VanillaTrades.getAll().entrySet()) {
            String tradeId = entry.getKey();
            // Trade IDs end with "/<name>" — check if the trade name contains the item name
            int lastSlash = tradeId.lastIndexOf('/');
            if (lastSlash >= 0) {
                String tradeName = tradeId.substring(lastSlash + 1);
                if (tradeName.contains(itemName) || tradeName.equals(itemName + "_buy") || tradeName.equals(itemName + "_sell")) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}
