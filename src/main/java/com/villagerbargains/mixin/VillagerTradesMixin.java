package com.villagerbargains.mixin;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.util.ModLogger;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
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
@Mixin(Villager.class)
public abstract class VillagerTradesMixin extends AbstractVillager {

    private VillagerTradesMixin() {
        super(null, null);
    }

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void villagerbargains$onUpdateTrades(CallbackInfo ci) {
        MerchantOffers offers = this.getOffers();
        if (offers == null || offers.isEmpty()) return;

        for (MerchantOffer offer : offers) {
            applyPriceConfig(offer);
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private static void applyPriceConfig(MerchantOffer offer) {
        ItemStack firstBuy = offer.getBaseCostA();
        if (firstBuy.isEmpty()) return;

        String itemId = firstBuy.getItem().toString();
        TradeDefinition def = findDefinitionByItem(itemId);
        if (def == null) return;

        VillagerBargainsConfig config  = VillagerBargainsConfig.getInstance();
        int desiredPrice = PriceResolver.resolve(def.tradeId(), config);
        if (desiredPrice < 0) return; // unknown trade or config disabled

        int currentCount = firstBuy.getCount();
        if (currentCount != desiredPrice) {
            firstBuy.setCount(desiredPrice);
            ModLogger.get().debug("[VillagerBargains] {} : {} → {}", def.tradeId(), currentCount, desiredPrice);
        }
    }

    /**
     * Finds the best-matching TradeDefinition for the given item ID string.
     * The item ID is the last path segment of the trade ID
     * (e.g. "minecraft:emerald" → matches trades whose id ends with "/emerald").
     * Falls back to null if nothing matches.
     */
    private static TradeDefinition findDefinitionByItem(String itemId) {
        int colon = itemId.lastIndexOf(':');
        String itemName = colon >= 0 ? itemId.substring(colon + 1) : itemId;

        for (Map.Entry<String, TradeDefinition> entry : VanillaTrades.getAll().entrySet()) {
            String tradeId = entry.getKey();
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
