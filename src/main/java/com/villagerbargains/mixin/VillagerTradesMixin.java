package com.villagerbargains.mixin;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.util.ModLogger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Adjusts prices only for VILLAGERS, never the wandering trader.
 *
 * We hook into Villager#updateTrades, which is called whenever the villager's
 * offers are (re)generated. This gives us access to the owning villager, so we
 * can guarantee that wandering traders are untouched.
 *
 * Only newly generated offers are affected; existing offers stored in NBT are
 * loaded as-is and never passed through this method.
 */
@Mixin(targets = "net.minecraft.world.entity.npc.Villager")
public abstract class VillagerTradesMixin {

    @Shadow
    public abstract MerchantOffers getOffers();

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void villagerbargains$onUpdateTrades(CallbackInfo ci) {
        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();
        if (!config.enabled) return;

        MerchantOffers offers = this.getOffers();
        if (offers == null || offers.isEmpty()) return;

        for (MerchantOffer offer : offers) {
            applyPriceConfig(offer, config);
        }
    }

    private static void applyPriceConfig(MerchantOffer offer, VillagerBargainsConfig config) {
        ItemStack firstBuy = offer.getBaseCostA();
        if (firstBuy.isEmpty()) return;

        // For enchanted books, PriceResolver derives the price purely from the
        // enchantment on the result item — no TradeDefinition needed.
        // For all other trades, look up the definition by the buy item.
        TradeDefinition def = offer.getResult().is(Items.ENCHANTED_BOOK)
                ? null
                : findDefinitionByItem(firstBuy.getItem().toString());

        // Non-book trades require a matching definition; skip if unknown.
        if (def == null && !offer.getResult().is(Items.ENCHANTED_BOOK)) return;

        int desiredPrice = PriceResolver.resolve(offer, def, config);
        if (desiredPrice < 0) return; // mod disabled

        int currentCount = firstBuy.getCount();
        if (currentCount != desiredPrice) {
            firstBuy.setCount(desiredPrice);
            ModLogger.get().debug("[VillagerBargains] {} : {} → {}",
                    def != null ? def.tradeId() : "enchanted_book", currentCount, desiredPrice);
        }
    }

    /**
     * Finds the best-matching TradeDefinition for the given item ID string.
     * The item ID is the last path segment of the trade ID
     * (e.g. "minecraft:emerald" → matches trades whose id ends with "/emerald").
     * Returns null if nothing matches.
     */
    private static TradeDefinition findDefinitionByItem(String itemId) {
        int colon = itemId.lastIndexOf(':');
        String itemName = colon >= 0 ? itemId.substring(colon + 1) : itemId;

        for (Map.Entry<String, TradeDefinition> entry : VanillaTrades.getAll().entrySet()) {
            String tradeId = entry.getKey();
            int lastSlash = tradeId.lastIndexOf('/');
            if (lastSlash >= 0) {
                String tradeName = tradeId.substring(lastSlash + 1);
                if (tradeName.contains(itemName)
                        || tradeName.equals(itemName + "_buy")
                        || tradeName.equals(itemName + "_sell")) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}
