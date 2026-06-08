package com.villagerbargains.mixin;

import com.villagerbargains.config.VillagerBargainsConfig;
import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.trade.TradeDefinition;
import com.villagerbargains.trade.VanillaTrades;
import com.villagerbargains.util.ModLogger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into Villager#updateTrades and sets each offer's emerald cost
 * to MINIMUM or MAXIMUM per config.
 *
 * Matching strategy:
 *  1. Sell item is enchanted_book -> look up by enchantment ID (BOOK_REGISTRY)
 *  2. All other trades            -> look up by buy item name  (REGISTRY)
 *
 * To support new trades: edit VanillaTrades.
 * To change price logic: edit PriceResolver.
 */
@Mixin(targets = "net.minecraft.world.entity.npc.Villager")
public abstract class VillagerTradesMixin {

    @Shadow public abstract MerchantOffers getOffers();

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

        TradeDefinition def = resolveDefinition(offer);
        if (def == null) return;

        int desired = PriceResolver.resolve(def.tradeId());
        if (desired < 0) return;

        int current = costA.getCount();
        if (current != desired) {
            costA.setCount(desired);
            ModLogger.get().debug("VillagerBargains: {} {} -> {}", def.tradeId(), current, desired);
        }
    }

    private static TradeDefinition resolveDefinition(MerchantOffer offer) {
        // Step 1: enchanted book — match by sell enchantment ID
        ItemStack result = offer.getResult();
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var enchEntry = enchantments.entrySet().iterator().next();
                var keyOpt = enchEntry.getKey().unwrapKey();
                if (keyOpt.isPresent()) {
                    ResourceLocation loc = keyOpt.get().location();
                    String enchId  = loc.getNamespace() + ":" + loc.getPath();
                    String sellKey = "enchanted_book:" + enchId;
                    TradeDefinition def = VanillaTrades.getByBook(sellKey);
                    if (def != null) return def;
                }
            }
        }

        // Step 2: all other trades — match by buy item name
        String itemId    = offer.getBaseCostA().getItem().toString();
        int colon        = itemId.lastIndexOf(':');
        String itemName  = colon >= 0 ? itemId.substring(colon + 1) : itemId;
        for (java.util.Map.Entry<String, TradeDefinition> e : VanillaTrades.getAll().entrySet()) {
            String tid       = e.getKey();
            int slash        = tid.lastIndexOf('/');
            String tradeName = slash >= 0 ? tid.substring(slash + 1) : tid;
            if (tradeName.equals(itemName) || tradeName.startsWith(itemName)) {
                return e.getValue();
            }
        }
        return null;
    }
}
