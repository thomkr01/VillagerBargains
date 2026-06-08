package com.villagerbargains.mixin;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.util.ModLogger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into Villager#updateTrades (called when a villager generates new offers)
 * and sets each offer's price to MINIMUM or MAXIMUM per config.
 *
 * To support new trade types: add entries to VanillaTrades.
 * To change price logic: edit PriceResolver.
 */
@Mixin(targets = "net.minecraft.world.entity.npc.Villager")
public abstract class VillagerTradesMixin {

    @Shadow
    public abstract MerchantOffers getOffers();

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void villagerbargains$onUpdateTrades(CallbackInfo ci) {
        MerchantOffers offers = this.getOffers();
        if (offers == null || offers.isEmpty()) return;

        VillagerBargainsConfig config = VillagerBargainsConfig.getInstance();

        for (MerchantOffer offer : offers) {
            applyPrice(offer, config);
        }
    }

    private static void applyPrice(MerchantOffer offer, VillagerBargainsConfig config) {
        ItemStack costA = offer.getBaseCostA();
        if (costA.isEmpty()) return;

        // Look up the trade by the buy item name
        String itemId = costA.getItem().toString();
        TradeDefinition def = findDefinition(itemId);
        if (def == null) return;

        int desired = PriceResolver.resolve(def.tradeId());
        if (desired < 0) return;

        int current = costA.getCount();
        if (current != desired) {
            costA.setCount(desired);
            ModLogger.get().debug("VillagerBargains: {} {} -> {}", def.tradeId(), current, desired);
        }
    }

    /** Matches a buy item ID to the best TradeDefinition. */
    private static TradeDefinition findDefinition(String itemId) {
        int colon = itemId.lastIndexOf(':');
        String itemName = colon >= 0 ? itemId.substring(colon + 1) : itemId;
        for (java.util.Map.Entry<String, TradeDefinition> e : VanillaTrades.getAll().entrySet()) {
            String tid = e.getKey();
            int slash = tid.lastIndexOf('/');
            String tradeName = slash >= 0 ? tid.substring(slash + 1) : tid;
            if (tradeName.equals(itemName) || tradeName.startsWith(itemName)) {
                return e.getValue();
            }
        }
        return null;
    }
}
